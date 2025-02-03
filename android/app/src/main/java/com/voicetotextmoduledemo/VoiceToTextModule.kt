package com.voicetotextmoduledemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.media.AudioRecord
import android.media.MediaRecorder
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlin.math.log10
import kotlin.math.sqrt

class VoiceToTextModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RecognitionListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var promise: Promise? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Add amplitude tracking properties
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var amplitudeThread: Thread? = null

    override fun getName(): String = "VoiceToTextModule"

    override fun getConstants(): Map<String, Any> {
        return hashMapOf(
            "RECORDING_STARTED" to "recordingStarted",
            "RECORDING_FINISHED" to "recordingFinished"
        )
    }

    @ReactMethod
    fun startAmplitudeTracking() {
        mainHandler.post {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                audioRecord?.startRecording()
                isRecording = true

                amplitudeThread = Thread {
                    val buffer = ShortArray(bufferSize)
                    while (isRecording) {
                        audioRecord?.read(buffer, 0, bufferSize)
                        val amplitude = calculateAmplitude(buffer)
                        sendEvent("onAmplitude", amplitude.toDouble())
                        Thread.sleep(50) // Update every 50ms
                    }
                }.apply { start() }
            } catch (e: Exception) {
                // Handle initialization errors
                sendEvent("onAmplitudeError", e.message)
            }
        }
    }

    @ReactMethod
    fun stopAmplitudeTracking() {
        mainHandler.post {
            isRecording = false
            amplitudeThread?.join()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            amplitudeThread = null
        }
    }

    private fun calculateAmplitude(buffer: ShortArray): Double {
        var sum = 0.0
        for (value in buffer) {
            sum += (value * value).toDouble()
        }
        val rms = sqrt(sum / buffer.size)
        return 20 * log10(rms) // Convert to dB
    }

    @ReactMethod
    fun startListening(promise: Promise) {
        this.promise = promise
        mainHandler.post {
            try {
                // Start speech recognizer
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext).apply {
                    setRecognitionListener(this@VoiceToTextModule)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                // Start amplitude tracking along with speech recognition
                startAmplitudeTracking()
                
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                promise.reject("INIT_ERROR", e)
            }
        }
    }

    @ReactMethod
    fun stopListening() {
        mainHandler.post {
            speechRecognizer?.stopListening()
            stopAmplitudeTracking()
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Keep: Required for React Native event emitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Keep: Required for React Native event emitter
    }

    // RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        promise?.reject("SPEECH_ERROR", "Error code: $error")
        cleanup()
    }

    override fun onResults(results: Bundle) {
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            sendEvent("onResults", matches[0]) // Send final result as event
            promise?.resolve(matches[0])
        } else {
            promise?.reject("NO_MATCH", "No speech results found")
        }
        cleanup()
    }

    override fun onPartialResults(partialResults: Bundle) {
        val partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (partialMatches != null && partialMatches.isNotEmpty()) {
            val array = Arguments.createArray()
            array.pushString(partialMatches[0])
            sendEvent("onPartialResults", array)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun cleanup() {
        mainHandler.post {
            stopAmplitudeTracking()
            speechRecognizer?.destroy()
            speechRecognizer = null
            promise = null
        }
    }

    private fun sendEvent(eventName: String, data: Any?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, data)
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        stopAmplitudeTracking()
        cleanup()
    }
}