import React, { useEffect } from 'react';
import { NativeModules, NativeEventEmitter, Button, View, Text, PermissionsAndroid, Platform } from 'react-native';

const { VoiceToTextModule } = NativeModules;
const voiceModuleEmitter = new NativeEventEmitter(VoiceToTextModule);

const App = () => {
  const [text, setText] = React.useState('');

  useEffect(() => {
    const subscription = voiceModuleEmitter.addListener('onPartialResults', (data) => {
      setText(data[0]); // Update UI with partial results
    });
    return () => subscription.remove();
  }, []);

  const requestPermission = async () => {
    if (Platform.OS !== 'android') return true;
    
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        {
          title: "Microphone Permission",
          message: "App needs access to your microphone for speech recognition",
          buttonNeutral: "Ask Me Later",
          buttonNegative: "Cancel",
          buttonPositive: "OK"
        }
      );
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    } catch (err) {
      console.warn(err);
      return false;
    }
  };

  const startListening = async () => {
    try {
      const hasPermission = await requestPermission();
      if (!hasPermission) {
        console.error('Permission denied');
        return;
      }
      const result = await VoiceToTextModule.startListening();
      setText(result); // Final result
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>{text}</Text>
      <Button title="Start Listening" onPress={startListening} />
      <Button title="Stop" onPress={() => VoiceToTextModule.stopListening()} />
    </View>
  );
};

export default App;