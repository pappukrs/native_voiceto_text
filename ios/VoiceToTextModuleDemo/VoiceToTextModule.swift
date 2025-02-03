import Foundation
import Speech
import React

@objc(VoiceToTextModule)
class VoiceToTextModule: RCTEventEmitter, SFSpeechRecognizerDelegate {
  private var speechRecognizer: SFSpeechRecognizer?
  private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
  private var recognitionTask: SFSpeechRecognitionTask?
  private let audioEngine = AVAudioEngine()

  // Supported events (e.g., "onPartialResults")
  override func supportedEvents() -> [String]! { ["onPartialResults"] }

  // Start listening method
  @objc func startListening(_ resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    SFSpeechRecognizer.requestAuthorization { status in
      DispatchQueue.main.async {
        switch status {
        case .authorized:
          self.setupRecognition(resolve: resolve, reject: reject)
        default:
          reject("SPEECH_ERROR", "Permission denied", nil)
        }
      }
    }
  }

  // Setup audio session and recognition
  private func setupRecognition(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    do {
      let audioSession = AVAudioSession.sharedInstance()
      try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
      try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

      let recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
      self.recognitionRequest = recognitionRequest

      speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
      recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
        if let result = result {
          resolve(result.bestTranscription.formattedString)
          if result.isFinal {
            self.cleanup()
          }
        } else if let error = error {
          reject("SPEECH_ERROR", error.localizedDescription, nil)
          self.cleanup()
        }
      }

      let inputNode = audioEngine.inputNode
      let recordingFormat = inputNode.outputFormat(forBus: 0)
      inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
        recognitionRequest.append(buffer)
      }

      audioEngine.prepare()
      try audioEngine.start()
    } catch {
      reject("SPEECH_ERROR", "Setup failed", error)
      cleanup()
    }
  }

  // Stop listening
  @objc func stopListening() {
    cleanup()
  }

  // Cleanup resources
  private func cleanup() {
    audioEngine.stop()
    audioEngine.inputNode.removeTap(onBus: 0)
    recognitionRequest?.endAudio()
    recognitionTask?.cancel()
    recognitionRequest = nil
    recognitionTask = nil
  }
}