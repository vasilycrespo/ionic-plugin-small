var exec = require('cordova/exec');

exports.connectRobot = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "connectRobot", [arg0]);
};

exports.reconnectRobot = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "reconnectRobot", [arg0]);
};

exports.getRobotID = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getRobotID", [arg0]);
};

exports.startRobotDiscovery = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "startRobotDiscovery", [arg0]);
};

exports.btRobotDisconnect = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "btRobotDisconnect", [arg0]);
};

exports.connectToWifi = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "connectToWifi", [arg0]);
};
