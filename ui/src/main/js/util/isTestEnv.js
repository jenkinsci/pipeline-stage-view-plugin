var windowHandle = require('window-handle');

module.exports = function isTestEnv() {
  if (windowHandle.getWindow() === undefined) {
      return true;
  } else if (windowHandle.getWindow().navigator === undefined) {
      return true;
  } else if (windowHandle.getWindow().navigator.userAgent === undefined) {
      return true;
  } else if (windowHandle.getWindow().navigator.userAgent.toLowerCase().indexOf("jsdom") !== -1) {
      return true;
  }
}
