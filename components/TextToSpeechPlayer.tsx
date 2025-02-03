// chatbot/src/components/TextToSpeechPlayer.tsx
import React from 'react';
import { WebView } from 'react-native-webview';
import { View, StyleSheet } from 'react-native';

const HTMLContent = (apiKey: string, text: string) => `
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="https://cdn.jsdelivr.net/npm/elevenlabs@latest/dist/elevenlabs.min.js"></script>
</head>
<body>
    <button onclick="playAudio()" style="
        padding: 15px 30px;
        font-size: 16px;
        background: #007AFF;
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
    ">
        Play
    </button>

    <script>
        const client = new ElevenLabsClient({
            apiKey: '${apiKey}'
        });

        async function playAudio() {
            try {
                const audioStream = await client.textToSpeech.convertAsStream(
                    'JBFqnCBsd6RMkjVDRZzb', {
                        text: \`${text}\`,
                        model_id: 'eleven_multilingual_v2'
                    }
                );

                const audioChunks = [];
                for await (const chunk of audioStream) {
                    audioChunks.push(chunk);
                }

                const audioBlob = new Blob(audioChunks, { type: 'audio/mpeg' });
                const audioUrl = URL.createObjectURL(audioBlob);
                const audio = new Audio(audioUrl);
                audio.play();
                
                window.ReactNativeWebView.postMessage('PLAYING');

            } catch (error) {
                window.ReactNativeWebView.postMessage('ERROR:' + error.message);
            }
        }
    </script>
</body>
</html>
`;

const TextToSpeechPlayer: React.FC = () => {
  const handleMessage = (event: any) => {
    const message = event.nativeEvent.data;
    if (message.startsWith('ERROR')) {
      console.error('WebView Error:', message);
    }
  };

  return (
    <View style={styles.container}>
      <WebView
        originWhitelist={['*']}
        javaScriptEnabled={true}
        domStorageEnabled={true}
        mixedContentMode="always"
        onMessage={handleMessage}
        allowsInlineMediaPlayback={true}
        mediaPlaybackRequiresUserAction={false}
        source={{ html: HTMLContent(
          'sk_db2fb5d6d6fdde9761fcab5fde804ffb4626c89bf5388af6',
          'Munafa Technologies leads in digital innovation and transformative solutions.'
        ) }}
        style={styles.webview}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: 200,
    height: 100,
  },
  webview: {
    flex: 1,
  }
});

export default TextToSpeechPlayer;