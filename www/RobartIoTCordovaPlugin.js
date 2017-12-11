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

exports.getRobotStatus = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getRobotStatus", [arg0]);
};

exports.startRobot = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "startRobot", [arg0]);
};

exports.stopRobot = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "stopRobot", [arg0]);
};

exports.sendHomeRobot = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "sendHomeRobot", [arg0]);
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

// NEW METHODS
exports.scheduleList = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "scheduleList", [arg0]);
};
exports.scheduleEdit = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "scheduleEdit", [arg0]);
};
exports.scheduleDelete = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "scheduleDelete", [arg0]);
};
exports.scheduleAdd = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "scheduleAdd", [arg0]);
};
exports.mapGetRobotPosition = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetRobotPosition", [arg0]);
};
exports.mapGetMovableObjectsList = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetMovableObjectsList", [arg0]);
};
exports.mapGetRoomLayout = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetRoomLayout", [arg0]);
};
exports.mapGetCleaningGrid = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetCleaningGrid", [arg0]);
};
exports.robotGetRobotId = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "robotGetRobotId", [arg0]);
};
exports.robotSetName = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "robotSetName", [arg0]);
};
exports.robotGetEventList = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "robotGetEventList", [arg0]);
};
exports.mapDelete = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapDelete", [arg0]);
};
exports.mapGetMaps = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetMaps", [arg0]);
};
exports.getRobotStatus = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getRobotStatus", [arg0]);
};
exports.actionSetSuctionControl = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "actionSetSuctionControl", [arg0]);
};
exports.mapGetAreas = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetAreas", [arg0]);
};
exports.actionSetExplore = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "actionSetExplore", [arg0]);
};
exports.actionSetArea = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "actionSetArea", [arg0]);
};
exports.actionModifyArea = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "actionModifyArea", [arg0]);
};
exports.mapDeleteArea = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapDeleteArea", [arg0]);
};
exports.getCommandResult = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getCommandResult", [arg0]);
};
exports.mapGetTiles = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "mapGetTiles", [arg0]);
};
exports.saveMap = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "saveMap", [arg0]);
};
exports.getCurrentMapId = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getCurrentMapId", [arg0]);
};
exports.getFirmware = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "getFirmware", [arg0]);
};
exports.downloadFirmware = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "downloadFirmware", [arg0]);
};
exports.installFirmware = function(arg0, success, error) {
    exec(success, error, "RobartIoTCordovaPlugin", "installFirmware", [arg0]);
};
