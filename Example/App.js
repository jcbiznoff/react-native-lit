/* @flow */
import React from 'react';
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from 'react-native';
import RNLit from 'react-native-lit';

type State = {
  isOn: boolean,
};

export default class App extends React.Component<void, void, State> {
  state: State = {
    isOn: false,
  };

  updateFlashlight = () => {
    RNLit.isFlashAvail()
      .then(result => {
        if (result.deviceSupportsFlash) {
          return RNLit.turnOn(!this.state.isOn);
        }
      })
      .then(result => {
        this.setState({isOn: !this.state.isOn});
      })
      .catch(error => {console.log(error)});
  }

  render() {
    return (
      <View style={styles.container}>
        <TouchableOpacity onPress={this.updateFlashlight}>
          <Text> {this.state.isOn ? 'Turn Off' : 'Turn On'}</Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
