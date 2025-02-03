package com.voicetotextmoduledemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class VoiceToTextModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RecognitionListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var promise: Promise? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun getName(): String = "VoiceToTextModule"

    @ReactMethod
    fun startListening(promise: Promise) {
        this.promise = promise
        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext).apply {
                    setRecognitionListener(this@VoiceToTextModule)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

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
        }
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
}