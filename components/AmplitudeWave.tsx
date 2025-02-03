import React, { useEffect, useRef } from 'react';
import { View, Animated, StyleSheet, NativeEventEmitter, NativeModules } from 'react-native';

const { VoiceToTextModule } = NativeModules;

const AmplitudeWave = () => {
  const animatedValue = useRef(new Animated.Value(0)).current;
  
  useEffect(() => {
    const eventEmitter = new NativeEventEmitter(VoiceToTextModule);
    
    const subscription = eventEmitter.addListener('onAmplitude', (dB: number) => {
      if (typeof dB === 'number') {
        const normalized = Math.max(0, Math.min(1, (dB + 60) / 60));
        Animated.spring(animatedValue, {
          toValue: normalized,
          useNativeDriver: true,
          friction: 8,
          tension: 30
        }).start();
      }
    });

    // Start amplitude tracking when component mounts
    VoiceToTextModule.startAmplitudeTracking();

    return () => {
      subscription.remove();
      VoiceToTextModule.stopAmplitudeTracking();
    };
  }, [animatedValue]);

  const barCount = 20;
  const bars = Array(barCount).fill(0).map((_, i) => (
    <Animated.View
      key={i}
      style={[
        styles.bar,
        {
          transform: [
            {
              scaleY: animatedValue.interpolate({
                inputRange: [0, 1],
                outputRange: [0.1, 1 + Math.sin(i / 3) * 0.5],
              })
            }
          ],
          backgroundColor: animatedValue.interpolate({
            inputRange: [0, 0.4, 0.8],
            outputRange: ['#4a90e2', '#ffd700', '#ff4444']
          })
        }
      ]}
    />
  ));

  return <View style={styles.container}>{bars}</View>;
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    height: 150,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },
  bar: {
    width: 3,
    height: '80%',
    backgroundColor: '#4a90e2',
    borderRadius: 2,
  },
});

export default AmplitudeWave; 