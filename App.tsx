import React, { useEffect, useState } from 'react';
import { NativeModules, NativeEventEmitter, Button, View, Text, PermissionsAndroid, Platform, StyleSheet } from 'react-native';
import AmplitudeWave from './components/AmplitudeWave';

const { VoiceToTextModule } = NativeModules;
const voiceModuleEmitter = new NativeEventEmitter(VoiceToTextModule);

const App = () => {
  const [text, setText] = useState('');
  const [isListening, setIsListening] = useState(false);

  useEffect(() => {
    const subscription = voiceModuleEmitter.addListener('onPartialResults', (data) => {
      if (data && data[0]) {
        setText(data[0]);
      }
    });

    return () => {
      subscription.remove();
    };
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
      setIsListening(true);
      const result = await VoiceToTextModule.startListening();
      setText(result);
    } catch (error) {
      console.error(error);
      setIsListening(false);
    }
  };

  const stopListening = () => {
    VoiceToTextModule.stopListening();
    setIsListening(false);
  };

  return (
    <View style={styles.container}>
      {isListening && <AmplitudeWave />}
      <Text style={styles.text}>{text || 'Start speaking...'}</Text>
      <Button 
        title={isListening ? "Stop Listening" : "Start Listening"} 
        onPress={isListening ? stopListening : startListening} 
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  text: {
    fontSize: 18,
    marginVertical: 20,
    textAlign: 'center',
  },
});

export default App;