package cordova.plugin.robart.iot;

import android.content.Context;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Publisher;
import io.reactivex.functions.Function;
import static cc.robart.bluetooth.sdk.core.BluetoothDeviceState.STATE_CONNECTED;
import cc.robart.bluetooth.sdk.exceptions.RobBleCoreException;

import cc.robart.aicu.android.sdk.factory.AICUConnector;
import cc.robart.aicu.android.sdk.factory.RobartSDKFactory;
import cc.robart.bluetooth.sdk.core.response.BluetoothWIFIScanResults;
import cc.robart.bluetooth.sdk.core.response.BluetoothGetRobotIdResponse;
import cc.robart.bluetooth.sdk.core.response.BluetoothWIFIScanResultResponse;

import cc.robart.aicu.android.sdk.configuration.RobotIotConfiguration;
import cc.robart.aicu.android.sdk.configuration.BaseRobotConfiguration;
import cc.robart.aicu.android.sdk.configuration.Strategy;
import cc.robart.aicu.android.sdk.application.RobartSDK;
import cc.robart.aicu.android.sdk.commands.*;
import cc.robart.aicu.android.sdk.commands.maps.*;
import cc.robart.aicu.android.sdk.datatypes.BatteryStatus.ChargingState;
import cc.robart.aicu.android.sdk.datatypes.*;
import cc.robart.aicu.android.sdk.datatypes.RobotStatus.Mode;
import cc.robart.aicu.android.sdk.exceptions.AuthenticationException;
import cc.robart.aicu.android.sdk.exceptions.ConnectionException;
import cc.robart.aicu.android.sdk.exceptions.RequestException;
import cc.robart.aicu.android.sdk.internal.data.FeatureMap;
import cc.robart.aicu.android.sdk.internal.data.Polygon;
import cc.robart.aicu.android.sdk.internal.data.RobotImpl;
import cc.robart.aicu.android.sdk.listeners.ConnectionListener;
import cc.robart.aicu.android.sdk.models.KeyValuePair;
import cc.robart.aicu.android.sdk.retrofit.client.FirmwareManager;
import cc.robart.aicu.android.sdk.retrofit.client.FirmwareManagerImpl;
import cc.robart.aicu.android.sdk.retrofit.client.IotManager;
import cc.robart.aicu.android.sdk.retrofit.client.IotManagerImpl;

import cc.robart.aicu.android.sdk.retrofit.request.PairingInfoRequest;
import cc.robart.aicu.android.sdk.retrofit.response.PairingStatus;
import cc.robart.bluetooth.sdk.core.BluetoothDeviceState;
import cc.robart.bluetooth.sdk.core.client.BluetoothClient;
import cc.robart.bluetooth.sdk.core.client.BluetoothClientImpl;
import cc.robart.bluetooth.sdk.core.exceptions.RxExceptions;
import cc.robart.bluetooth.sdk.core.response.BluetoothWIFIScanResultResponse;
import cc.robart.bluetooth.sdk.core.scan.ScanResult;
import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import cc.robart.aicu.android.sdk.listeners.RobotDiscoveryListener;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Iterator;

//import io.reactivex.common.functions.Consumer;

class ConfigurationData {
    private String iotUserLogin;
    private String robotUid;
    private String robotPassword;
    private String serverEndpoint;
    private String robotEndpoint;
    private String stsk;
    private IotManager iotManager = null;
    private RobartIoTCordovaPlugin instance = null;
    private RobotIotConfiguration robotIotConfiguration = null;

    public ConfigurationData() {
        super();
    }

    public ConfigurationData(String iotUserLogin, String robotUid, String robotPassword, String serverEndpoint,
                             String robotEndpoint, String stsk) {
        super();
        this.iotUserLogin = iotUserLogin;

        if (robotUid != null && !robotUid.isEmpty() && !robotUid.contains("Unknown") && !robotUid.contains(" ")) {
            System.out.println("ConfigurationData. Setting a new robotID: " + robotUid);
            this.robotUid = robotUid;
        }

        System.out.println("IotUserLogin: " + iotUserLogin);
        System.out.println("RobotUid: " + robotUid);
        System.out.println("Stsk: " + stsk);

        this.robotPassword = robotPassword;
        this.serverEndpoint = serverEndpoint;
        this.robotEndpoint = robotEndpoint;
        this.stsk = stsk;
    }

    public RobartIoTCordovaPlugin getInstance() {
        return instance;
    }

    public void setInstance(RobartIoTCordovaPlugin instance) {
        this.instance = instance;
    }

    public RobotIotConfiguration getRobotIotConfiguration() {
        return robotIotConfiguration;
    }

    public void setRobotIotConfiguration(RobotIotConfiguration robotIotConfiguration) {
        this.robotIotConfiguration = robotIotConfiguration;
    }

    public String getIotUserLogin() {
        return iotUserLogin;
    }

    public void setIotUserLogin(String iotUserLogin) {
        this.iotUserLogin = iotUserLogin;
    }

    public String getRobotUid() {
        return robotUid;
    }

    public void setRobotUid(String robotUid) {
        this.robotUid = robotUid;
    }

    public String getRobotPassword() {
        return robotPassword;
    }

    public void setRobotPassword(String robotPassword) {
        this.robotPassword = robotPassword;
    }

    public String getServerEndpoint() {
        return serverEndpoint;
    }

    public void setServerEndpoint(String serverEndpoint) {
        this.serverEndpoint = serverEndpoint;
    }

    public String getRobotEndpoint() {
        return robotEndpoint;
    }

    public void setRobotEndpoint(String robotEndpoint) {
        this.robotEndpoint = robotEndpoint;
    }

    public String getStsk() {
        return stsk;
    }

    public void setStsk(String stsk) {
        this.stsk = stsk;
    }

    public void setIotManager(IotManager iotManager) {
        this.iotManager = iotManager;
    }

    public IotManager getIotManager() {
        return this.iotManager;
    }

    public RobartIoTCordovaPlugin getRobartIoTCordovaPluginInstance() {
        return instance;
    }

    public void setRobartIoTCordovaPluginInstance(RobartIoTCordovaPlugin instance) {
        this.instance = instance;
    }

    public void initServerOld(RobartIoTCordovaPlugin instance) {
        if (this.robotIotConfiguration != null) {
            return;
        }

        try {
            System.out.println("InitServer, robotIotConfiguration");

            RobartSDK.initialize(instance.cordova.getActivity().getApplicationContext());


            RobartSDK.initialize(instance.cordova.getActivity().getApplicationContext());
            robotIotConfiguration = RobotIotConfiguration.builder()
                    .setHost(serverEndpoint)
                    .setParts("iot")
                    .setPort(443)
                    .setStrategy(Strategy.HTTPS)
                    .build();

            this.iotManager = new IotManagerImpl(robotIotConfiguration);
/*
            this.robotIotConfiguration = ServerConfiguration.builder().hostUrl(serverEndpoint).parts("iot")
                    .port(443).strategy(Strategy.https).build();
            this.iotManager = new IotManagerImpl(this.robotIotConfiguration);
            this.instance = instance;
            AICUConnector.init(this.robotIotConfiguration);
 */
        } catch (Exception e) {
            System.out.println("Exception thrown in initializing the app" + e.getMessage());
        }
    }

    public void initServer(RobartIoTCordovaPlugin instance) {
        if (this.robotIotConfiguration != null) {
            return;
        }

        try {
            System.out.println("InitServer, robotIotConfiguration");

            RobartSDK.initialize(instance.cordova.getActivity().getApplicationContext());
            robotIotConfiguration = RobotIotConfiguration.builder()
                    .setHost(serverEndpoint)
                    .setParts("iot")
                    .setPort(443)
                    .setStrategy(Strategy.HTTPS)
                    .build();

            this.iotManager = new IotManagerImpl(robotIotConfiguration);
            /*
             RobartSDK.initialize(instance.cordova.getActivity().getApplicationContext());

            this.robotIotConfiguration = ServerConfiguration.builder().hostUrl(serverEndpoint).parts("iot")
                    .port(443).strategy(Strategy.https).build();
            this.iotManager = new IotManagerImpl(this.robotIotConfiguration);
            this.instance = instance;
            AICUConnector.init(this.robotIotConfiguration);
             */
        } catch (Exception e) {
            System.out.println("Exception thrown in initializing the app" + e.getMessage());
        }
    }
}

public class RobartIoTCordovaPlugin extends CordovaPlugin {

    private static Integer currentPermanent = 0;
    private static String uid;

    private static Context instance = null;
    private BluetoothClient rxBleClient;
    private CompositeDisposable scanSubscription;
    private int pairingCheckCounts = 0;
    private static Integer wifiStatusCounts = 0;
    private CallbackContext btCallbackContext;
    private ConfigurationData configurationData = null;
    private Boolean configuredConnector = false;
    private Boolean justPaired = false;
    private int getRobotIdCount = 0;
    private RobotImpl robot = null;
    private List<BluetoothWIFIScanResultResponse> wifiList = null;
    private Boolean foundRobot = false;
    private Boolean firstTimeConfiguration = false;


    private void startRobotDiscovery(String message, CallbackContext callbackContext) {
        System.out.println("StartRobotDiscovery. Starting");
        RobartSDK.initialize(this.cordova.getActivity().getApplicationContext());
        this.foundRobot = false;
        this.configuredConnector = false;
        this.firstTimeConfiguration = true;

        btCallbackContext = callbackContext;
        try {
            scanSubscription.add(getClient().startRobotDiscovery()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::setItem, this::onScanFailure));
        } catch (Exception e) {
            // Ignore this error
        }
    }

    public void setItem(ScanResult scanResult) {
        String deviceName = scanResult.getScanRecord().getDeviceName();
        String macAddress = scanResult.getBleDevice().getMacAddress();

        if (deviceName != null && !deviceName.isEmpty() && deviceName.startsWith("robot-") && !this.foundRobot) {
            this.foundRobot = true;
            System.out.println("Success StartRobotDiscovery, name: " + deviceName + " macAddress: " + macAddress);

            System.out.println("StopRobotDiscovery, starting");
            getClient().stopRobotDiscovery().subscribe(response -> {
                System.out.println("StopRobotDiscovery, success");
                connectDevice(macAddress, btCallbackContext);
            }, throwable -> {
                System.out.println("StopRobotDiscovery, Exception:" + throwable);
                connectDevice(macAddress, btCallbackContext);
            });
        }
    }

    public void onScanFailure(Throwable throwable) {
        System.out.println("Error StartRobotDiscovery, onScanFailure: " + throwable.toString());
        btCallbackContext.error("{'response': 'error'}".replaceAll("'", "\""));
    }

    private void btRobotDisconnect(String message, CallbackContext callbackContext) {
        System.out.println("BtRobotDisconnect. Calling : " + message);

        getClient().disconnectFromRobot().subscribe(aBoolean -> System.out.println("disconnectFromRobot() called: " + aBoolean),
                RxExceptions::propagate);

        getClient().addDisconnectCallback(() -> {
            callbackContext.success("{}");
            System.out.println("disconnected");
        });
    }

    public BluetoothClient getClient() {
        if (rxBleClient == null) {
            if (instance == null) {
                instance = this.cordova.getActivity().getApplicationContext();
            }

            rxBleClient = BluetoothClientImpl.create(instance);
            rxBleClient.setLogging(true);

        }
        return rxBleClient;
    }
    private Disposable connection;

    public void connectDevice(String macAddress, CallbackContext callbackContext) {
        RobartIoTCordovaPlugin plugin = this;
        System.out.println("ConnectDevice, macAddress: " + macAddress);

        if (connection != null && !connection.isDisposed()) {
            sendChainCommands()
                    .subscribe(bluetoothWIFIScanResults -> {
                        System.out.println("Wifi response data :" + bluetoothWIFIScanResults.getScan());
                        StringBuilder sbList = new StringBuilder();
                        String stringList = "";
                        String response = "";

                        for (BluetoothWIFIScanResultResponse currentWifi : bluetoothWIFIScanResults.getScan()) {
                            sbList.append(String.format("{'ssid': '%s', 'rssi': '%s', 'pairwisecipher': '%s'}, ",
                                    currentWifi.getSsid().replaceAll("'", ""), currentWifi.getRssi(),
                                    currentWifi.getPairwiseCipher()));
                        }

                        if (sbList.toString().length() > 2) {
                            stringList = sbList.toString().substring(0, sbList.toString().length() - 2);
                        }

                        response = String.format("{'response': [%s]}", stringList).replaceAll("'", "\"");
                        System.out.println("GetWifiScanResult, Response : " + response);
                        callbackContext.success(response);
                    }, throwable -> {
                        System.out.println("GetWifiScanResult, exception: " + throwable.getMessage());
                        callbackContext.error(
                                "{'response': 'getWifiScanResult, error on connect device. please review the native console'}"
                                        .replaceAll("'", "\""));
                    });
            return;
        }
        addDisconnectCallback();

        connection = getClient().connectToRobot(macAddress)
                .map(new Function<Integer, Boolean>() {
                    @Override
                    public Boolean apply(Integer integer) throws Exception {
                        System.out.println("connection status" + integer);
                        boolean value = integer == STATE_CONNECTED;
                        System.out.println("Connection status: " + integer);
                        if (integer == STATE_CONNECTED) {
                            return true;
                        } else {
                            throw new RobBleCoreException("not connected");
                        }
                    }
                })
                .flatMap(new Function<Boolean, Publisher<? extends BluetoothWIFIScanResults>>() {
                    @Override
                    public Publisher<BluetoothWIFIScanResults> apply(Boolean aBoolean) throws Exception {
                        return sendChainCommands();
                    }
                })
                .subscribe(bluetoothWIFIScanResults -> {
                    System.out.println("Wifi response data :" + bluetoothWIFIScanResults.getScan());
                    StringBuilder sbList = new StringBuilder();
                    String stringList = "";
                    String response = "";

                    for (BluetoothWIFIScanResultResponse currentWifi : bluetoothWIFIScanResults.getScan()) {
                        sbList.append(String.format("{'ssid': '%s', 'rssi': '%s', 'pairwisecipher': '%s'}, ",
                                currentWifi.getSsid().replaceAll("'", ""), currentWifi.getRssi(),
                                currentWifi.getPairwiseCipher()));
                    }

                    if (sbList.toString().length() > 2) {
                        stringList = sbList.toString().substring(0, sbList.toString().length() - 2);
                    }

                    response = String.format("{'response': [%s]}", stringList).replaceAll("'", "\"");
                    System.out.println("GetWifiScanResult, Response : " + response);
                    callbackContext.success(response);
                }, throwable -> {
                    System.out.println("GetWifiScanResult, exception: " + throwable.getMessage());
                    callbackContext.error(
                            "{'response': 'getWifiScanResult, error on connect device. please review the native console'}"
                                    .replaceAll("'", "\""));
                });
    }

    private Flowable<BluetoothWIFIScanResults> sendChainCommands() {
        return getClient()
                .getRobotId()
                .flatMap(new Function<BluetoothGetRobotIdResponse, SingleSource<? extends BluetoothWIFIScanResults>>() {
                    @Override
                    public SingleSource<BluetoothWIFIScanResults> apply(BluetoothGetRobotIdResponse integer) throws Exception {
                        uid = integer.getUniqueId();
                        return getClient().startWifiScan()
                                .flatMap(new Function<Boolean, SingleSource<? extends BluetoothWIFIScanResults>>() {
                                    @Override
                                    public SingleSource<BluetoothWIFIScanResults> apply(Boolean aBoolean) throws Exception {
                                        return getWifiSingle();
                                    }
                                });
                    }
                }).toFlowable();
    }


    private void addDisconnectCallback() {
        getClient().addDisconnectCallback(() -> {
            if (connection != null) {
                connection.dispose();
                connection = null;
            }
            System.out.println("disconnected");
        });
    }

    private Single<BluetoothWIFIScanResults> getWifiSingle() {
        return Single.defer(() -> getClient().getWifiScanResult().doOnSuccess(bluetoothWIFIScanResults -> {
            System.out.println("GetWifiScanResult, getWifiScanResult() called" + bluetoothWIFIScanResults);
        }).doOnSuccess(bluetoothWIFIScanResults -> {
            if (bluetoothWIFIScanResults.isScanning()) {
                System.out.println("GetWifiScanResult, scanning..." + bluetoothWIFIScanResults.isScanning());
                throw new IllegalStateException("GetWifiScanResult, not final list");
            }
            System.out.println("GetWifiScanResult, scanning..." + bluetoothWIFIScanResults.isScanning());
        })).retry((integer, throwable) -> throwable instanceof IllegalStateException);
    }

    private void errorHandler(String method, Throwable ex, CallbackContext callbackContext) {
        System.out.println("METHOD " + method);
        System.out.println(ex);
        callbackContext.error("{'error': 'Unexpected error. Please, check the native console'}".replaceAll("'", "\""));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        String message1 = args.getString(0);
        String message = "";

        if (message1 == null) {
            message1 = "";
        }

        if (action.equals("mapGetRobotPosition") || action.equals("mapGetMovableObjectsList") || action.equals("mapGetRoomLayout") || action.equals("mapGetCleaningGrid") || action.equals("mapGetMaps") || action.equals("mapGetTiles") || action.equals("mapGetAreas") || action.equals("actionSetArea") || action.equals("actionModifyArea")) {
            char[] array = message1.toCharArray();
            StringBuilder t = new StringBuilder();

            Boolean negative = false;
            for (char ch: array) {
                if (ch == '-') {
                    t.append("'");
                    negative = true;
                }

                if ((ch == ',' || ch == '}') && negative) {
                    t.append("'");
                    negative = false;
                }

                t.append(ch);
            }

            message = t.toString().replaceAll("'", "\"").replaceAll(":\"\"", ":null");
        } else {
            message = message1.replaceAll("'", "\"").replaceAll(":\"\"", ":null");
        }

        //message = message1.replaceAll("'", "\"").replaceAll(":\"\"", ":null");

        if (action.equals("mapGetRobotPosition")) {
            this.mapGetRobotPosition(message, callbackContext);
            return true;
        }

        if (action.equals("connectRobot")) {
            this.connectRobot(message, callbackContext);
            return true;
        }

        if (action.equals("reconnectRobot")) {
            this.reconnectRobot(message, callbackContext);
            return true;
        }

        if (action.equals("getRobotID")) {
            this.getRobotID(message, callbackContext);
            return true;
        }

        if (action.equals("getRobotStatus")) {
            this.getRobotStatus(message, callbackContext);
            return true;
        }

        if (action.equals("startRobot")) {
            this.startRobot(message, callbackContext);
            return true;
        }

        if (action.equals("btRobotDisconnect")) {
            this.btRobotDisconnect(message, callbackContext);
            return true;
        }

        if (action.equals("startRobotDiscovery")) {
            this.startRobotDiscovery(message, callbackContext);
            return true;
        }

        if (action.equals("connectToWifi")) {
            this.connectToWifi(message, callbackContext);
            return true;
        }

        if (action.equals("robotGetRobotId")) {
            this.robotGetRobotId(message, callbackContext);
            return true;
        }

        if (action.equals("searchWifi")) {
            this.searchWifi(message, callbackContext);
            return true;
        }

        if (action.equals("startWifiScan")) {
            this.startWifiScan(message, callbackContext);
            return true;
        }

        if (action.equals("getFirmware")) {
            this.getFirmware(message, callbackContext);
            return true;
        }

        if (action.equals("stopRobot")) {
            this.stopRobot(message, callbackContext);
            return true;
        }

        if (action.equals("sendHomeRobot")) {
            this.sendHomeRobot(message, callbackContext);
            return true;
        }

        if (action.equals("scheduleList")) {
            this.scheduleList(message, callbackContext);
            return true;
        }

        if (action.equals("scheduleEdit")) {
            this.scheduleEdit(message, callbackContext);
            return true;
        }

        if (action.equals("scheduleAdd")) {
            this.scheduleAdd(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetMovableObjectsList")) {
            this.mapGetMovableObjectsList(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetRoomLayout")) {
            this.mapGetRoomLayout(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetCleaningGrid")) {
            this.mapGetCleaningGrid(message, callbackContext);
            return true;
        }

        if (action.equals("robotSetName")) {
            this.robotSetName(message, callbackContext);
            return true;
        }

        if (action.equals("robotGetEventList")) {
            this.robotGetEventList(message, callbackContext);
            return true;
        }

        if (action.equals("mapDelete")) {
            this.mapDelete(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetMaps")) {
            this.mapGetMaps(message, callbackContext);
            return true;
        }

        if (action.equals("actionSetSuctionControl")) {
            this.actionSetSuctionControl(message, callbackContext);
            return true;
        }

        if (action.equals("getCommandResult")) {
            this.getCommandResult(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetTiles")) {
            this.mapGetTiles(message, callbackContext);
            return true;
        }

        if (action.equals("saveMap")) {
            this.saveMap(message, callbackContext);
            return true;
        }

        if (action.equals("mapGetAreas")) {
            this.mapGetAreas(message, callbackContext);
            return true;
        }

        if (action.equals("actionSetArea")) {
            this.actionSetArea(message, callbackContext);
            return true;
        }

        if (action.equals("actionModifyArea")) {
            this.actionModifyArea(message, callbackContext);
            return true;
        }

        if (action.equals("mapDeleteArea")) {
            this.mapDeleteArea(message, callbackContext);
            return true;
        }

        if (action.equals("scheduleWifiStatus")) {
            this.scheduleWifiStatus(message, callbackContext);
            return true;
        }

        if (action.equals("getCurrentMapId")) {
            this.getCurrentMapId(message, callbackContext);
            return true;
        }

        if (action.equals("actionSetExplore")) {
            this.actionSetExplore(message, callbackContext);
            return true;
        }

        if (action.equals("scheduleDelete")) {
            this.scheduleDelete(message, callbackContext);
            return true;
        }

        if (action.equals("downloadFirmware")) {
            this.downloadFirmware(message, callbackContext);
            return true;
        }

        if (action.equals("installFirmware")) {
            this.installFirmware(message, callbackContext);
            return true;
        }

        return false;
    }

    //ORIGINAL INSTALL MAKE SURE INSTALL FIRMWARE HAS EVERYTHING
    /*private void installFirmware(String message, CallbackContext callbackContext) {`
        System.out.println("Firmwareupdate. Started");
        // Se puede verificar si se decargo usando: SDKUtils.isFirmwareStored
        String robotId = RobartSDKFactory.getDefaultAICUConnector().getCurrentRobotId();
        System.out.println("Firmwareupdate, finding robot in IM mode: " + robotId);
        AICUConnector.findRobots(new RobotDiscoveryListener() {
            @Override
            public void onDiscoveredRobot(final Robot r) {
                System.out.println("Firmwareupdate, onDiscoveredRobot, found a robot: " + r);

                if (r.getUniqueId().equals(robotId)) {
                  System.out.println("Firmwareupdate, onDiscoveredRobot, calling connectToRobot");

                  RobartSDKFactory.connectToRobot(r, new ConnectionListener() {
                      @Override
                      public void onConnect() {
                          System.out.println("Firmwareupdate, onDiscoveredRobot, connected to the robot");

                          // String uniqueId = RobartSDKFactory.getDefaultAICUConnector().getCurrentRobotId();
                          System.out.println("Firmwareupdate, InstallFirmware: " + robotId);

                          RobartSDKFactory.getDefaultAICUConnector().sendCommand(new InstallFirmware(robotId, new RequestCallback() {
                              @Override
                              public void onSuccess() {
                                System.out.println("Firmwareupdate, connectToRobot, InstallFirmware, success");
                                String response = ("{'success': true}").replaceAll("'", "\"");
                                System.out.println("Firmwareupdate. Response : " + response);
                                callbackContext.success(response);
                                    }
                              @Override
                              public void onAuthenticationError(AuthenticationException ex) {
                                  System.out.println("Firmwareupdate, onAuthenticationError, " + ex.getMessage());
                              }
                              @Override
                              public void onConnectionError(ConnectionException ex) {
                                  System.out.println("Firmwareupdate, onConnectionError, " + ex.getMessage());
                              }
                              @Override
                              public void onRequestError(RequestException e) {
                                  System.out.println("Firmwareupdate, onRequestError, " + e.getMessage());
                              }
                          }));
                      }
                      @Override
                      public void onDisconnect() {
                        System.out.println("Firmwareupdate, onDisconnect, the SDK was disconnected of the Robot");
                      }
                      @Override
                      public void onConnectionError(final Exception exception) {
                        System.out.println("Firmwareupdate, onConnectionError, " + exception.getMessage());
                      }
                      @Override
                      public void onAuthenticationError(final AuthenticationException exception) {
                        System.out.println("Firmwareupdate, onAuthenticationError, " + exception.getMessage());
                      }
                    });
                } else {
                  System.out.println("Firmwareupdate, found other robot: " +  r.toString());
                }
            }
            @Override
            public void onDiscoveredRobotUpdate(final Robot r) {
                    System.out.println("Firmwareupdate, onDiscoveredRobotUpdate: " + r.toString());
            }
        });
    }*/

    private void installFirmware(String message, CallbackContext callbackContext) {
        
        System.out.println("Firmwareupdate. Started");
        String robotId = RobartSDKFactory.getDefaultAICUConnector().getCurrentRobotId();
        System.out.println("Firmwareupdate, finding robot in IM mode: " + robotId);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new InstallFirmware(robotId, new RequestCallback() {
            @Override
            public void onSuccess() {
                System.out.println("Firmwareupdate, connectToRobot, InstallFirmware, success");
                String response = ("{'success': true}").replaceAll("'", "\"");
                System.out.println("Firmwareupdate. Response : " + response);
                callbackContext.success(response);
            }
            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                System.out.println("Firmwareupdate, onAuthenticationError, " + ex.getMessage());
            }
            @Override
            public void onConnectionError(ConnectionException ex) {
                System.out.println("Firmwareupdate, onConnectionError, " + ex.getMessage());
            }
            @Override
            public void onRequestError(RequestException e) {
                System.out.println("Firmwareupdate, onRequestError, " + e.getMessage());
            }
        }));
    }

    private void downloadFirmware(String message, CallbackContext callbackContext) {
        System.out.println("DownloadFirmware. Started");
        String uniqueId = RobartSDKFactory.getDefaultAICUConnector().getCurrentRobotId();
        System.out.println("DownloadFirmware. UniqueId: " + uniqueId);

        FirmwareManager firmwareManager = new FirmwareManagerImpl();
        firmwareManager.downloadFirmware(uniqueId).subscribe(getDisposableObserver(message, callbackContext));
    }

    private void scheduleDelete(String message, CallbackContext callbackContext) {
        System.out.println("ScheduleDelete. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final Integer task_id = Integer.parseInt(jsonMap.get("task_id").toString());
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new DeleteScheduledTaskRobotCommand(task_id, new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = ("{'cmd_id': " + result.getCommandId() + "}").replaceAll("'", "\"");
                System.out.println("ScheduleDelete. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ScheduleDelete", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ScheduleDelete", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ScheduleDelete", ex, callbackContext);
            }
        }));
    }

    private void actionSetExplore(String message, CallbackContext callbackContext) {
        System.out.println("ActionSetExplore. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new ExploreRobotCommand(new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = String.format("{'cmd_id': %d}", result.getCommandId()).toString().replaceAll("'",
                        "\"");
                System.out.println("ActionSetExplore. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ActionSetExplore", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ActionSetExplore", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ActionSetExplore", ex, callbackContext);
            }

        }));
    }

    private void getCurrentMapId(String message, CallbackContext callbackContext) {
        System.out.println("GetCurrentMapId. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetCurrentMapIdRobotCommand(new RequestCallbackWithResult<Integer>() {

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("GetCurrentMapId", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("GetCurrentMapId", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("GetCurrentMapId", ex, callbackContext);
            }

            @Override
            public void onSuccess(final Integer result) {
                String response = ("{'map_id': " + result.toString() + "}").replaceAll("'", "\"");
                System.out.println("GetCurrentMapId. Calling : " + response);
                callbackContext.success(response.replaceAll("'", "\""));
            }
        }));
    }

    private void scheduleWifiStatus(String ssid, CallbackContext callbackContext) {
        System.out.println("ScheduleWifiStatus. Calling : " + ssid);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetWifiStatusRobotCommand(new RequestCallbackWithResult<WIFIStatus>() {
            @Override
            public void onSuccess(final WIFIStatus result) {
                if (result.getStatus() == WIFIConnectionStatus.CONNECTED && result.getSSID().equals(ssid)) {
                    robotGetRobotId(ssid, callbackContext);
                } else {
                    if (wifiStatusCounts <= 10) {
                        wifiStatusCounts = wifiStatusCounts + 1;
                        scheduleWifiStatus(ssid, callbackContext);
                    } else {
                        System.out.println("BT: Could not connect to the WiFi");
                        callbackContext.error("{'error': 'Error occured on connectoToWifi'}".replaceAll("'", "\""));
                    }
                }
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ScheduleWifiStatus", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ScheduleWifiStatus", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ScheduleWifiStatus", ex, callbackContext);
            }
        }));
    }

    private void mapDeleteArea(String message, CallbackContext callbackContext) {
        System.out.println("MapDeleteArea. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("MapDeleteArea. MapID : " + jsonMap.get("map_id").toString());
        System.out.println("MapDeleteArea. AreaID : " + jsonMap.get("area_id").toString());

        int mapId = Integer.parseInt(jsonMap.get("map_id").toString());
        int areaId = Integer.parseInt(jsonMap.get("area_id").toString());

        if (currentPermanent != 0) {
            mapId = currentPermanent.intValue();
        }

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new DeleteAreaRobotCommand(mapId, areaId, new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = ("{'cmd_id': " + result.getCommandId() + "}").replaceAll("'", "\"");
                System.out.println("MapDeleteArea. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapDeleteArea", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapDeleteArea", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapDeleteArea", ex, callbackContext);
            }
        }));
    }

    private void actionModifyArea(String message, CallbackContext callbackContext) {
        System.out.println("ActionModifyArea. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final Integer area_id = Integer.parseInt(jsonMap.get("area_id").toString());
        final String area_meta_data = jsonMap.get("area_meta_data").toString();
        final String area_type = jsonMap.get("area_type").toString();
        final List<Point2D> points = new ArrayList<>();
        int axisIdx = 1;
        String axisKeyX = "";
        String axisKeyY = "";
        boolean next = true;
        while (next) {
            axisKeyX = "x" + Integer.toString(axisIdx);
            axisKeyY = "y" + Integer.toString(axisIdx);
            if (jsonMap.get(axisKeyX) != null) {
                points.add(Point2D.builder().x(Float.valueOf(jsonMap.get(axisKeyX).toString()))
                        .y(Float.valueOf(jsonMap.get(axisKeyY).toString())).build());
                axisIdx++;
            } else {
                next = false;
                break;
            }
        }
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
            @Override
            public void onSuccess(List<MapInfo> result) {
                System.out.println("ActionModifyArea. Success to call the GetMapsRobotCommand method");

                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        RobartIoTCordovaPlugin.currentPermanent = mi.getMapId();
                    }
                }

                AreaType setType;
                AreaState setState;

                if (area_type.equals("to_be_cleaned")) {
                    setType = AreaType.TO_BE_CLEANED;
                    setState = AreaState.CLEAN;
                } else {
                    setType = AreaType.ROOM;
                    setState = AreaState.BLOCKING;
                }

                Area area = Area.builder().areaId(area_id).areaType(setType).roomType(RoomType.DINING)
                        .floorType(FloorType.CARPET).cleaningParameterSet(CleaningParameterSet.SILENT)
                        .areaState(setState).points(points).metaData(area_meta_data).build();

                RobartSDKFactory.getDefaultAICUConnector().sendCommand(new ModifyAreaRobotCommand(RobartIoTCordovaPlugin.currentPermanent, area,
                        new RequestCallbackWithCommandID() {
                            @Override
                            public void onSuccess(CommandId result) {
                                String response = ("{'cmd_id': " + result.getCommandId() + "}").replaceAll("'", "\"");
                                System.out.println("ActionModifyArea. Response : " + result);
                                callbackContext.success(response);
                            }

                            @Override
                            public void onAuthenticationError(AuthenticationException ex) {
                                errorHandler("ActionModifyArea", ex, callbackContext);
                            }

                            @Override
                            public void onConnectionError(ConnectionException ex) {
                                errorHandler("ActionModifyArea", ex, callbackContext);
                            }

                            @Override
                            public void onRequestError(RequestException ex) {
                                errorHandler("ActionModifyArea", ex, callbackContext);
                            }
                        }));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ActionModifyArea", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ActionModifyArea", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ActionModifyArea", ex, callbackContext);
            }
        }));
    }

    private void actionSetArea(String message, CallbackContext callbackContext) {
        System.out.println("ActionSetArea. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final Integer map_id = Integer.parseInt(jsonMap.get("map_id").toString());
        final String area_meta_data = jsonMap.get("area_meta_data").toString();
        final String area_type = jsonMap.get("area_type").toString();

        final List<Point2D> points = new ArrayList<>();
        int axisIdx = 1;
        String axisKeyX = "";
        String axisKeyY = "";
        boolean next = true;
        while (next) {
            axisKeyX = "x" + Integer.toString(axisIdx);
            axisKeyY = "y" + Integer.toString(axisIdx);

            if (jsonMap.get(axisKeyX) != null) {
                points.add(Point2D.builder().x(Float.valueOf(jsonMap.get(axisKeyX).toString()))
                        .y(Float.valueOf(jsonMap.get(axisKeyY).toString())).build());
                axisIdx++;
            } else {
                next = false;
                break;
            }
        }
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
            @Override
            public void onSuccess(List<MapInfo> result) {

                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        RobartIoTCordovaPlugin.currentPermanent = mi.getMapId();
                    }
                }

                if (RobartIoTCordovaPlugin.currentPermanent == map_id) {

                    AreaType setType;
                    AreaState setState;

                    if (area_type.equals("to_be_cleaned")) {
                        setType = AreaType.TO_BE_CLEANED;
                        setState = AreaState.CLEAN;
                    } else {
                        setType = AreaType.ROOM;
                        setState = AreaState.BLOCKING;
                    }

                    Area area = Area.builder().areaType(setType).roomType(RoomType.DINING).floorType(FloorType.CARPET)
                            .cleaningParameterSet(CleaningParameterSet.SILENT).areaState(setState).points(points)
                            .metaData(area_meta_data).build();
                    System.out.println("ActionSetArea. Area ready: " + map_id + " " + area);
                    RobartSDKFactory.getDefaultAICUConnector().sendCommand(new AddAreaRobotCommand(map_id, area, new RequestCallbackWithCommandID() {
                        @Override
                        public void onSuccess(CommandId result) {
                            String response = ("{'cmd_id': " + result.getCommandId() + "}").replaceAll("'", "\"");
                            System.out.println("ActionSetArea. Response : " + response);
                            callbackContext.success(response);
                        }

                        @Override
                        public void onAuthenticationError(AuthenticationException ex) {
                            errorHandler("actionSetArea", ex, callbackContext);
                        }

                        @Override
                        public void onConnectionError(ConnectionException ex) {
                            errorHandler("actionSetArea", ex, callbackContext);
                        }

                        @Override
                        public void onRequestError(RequestException ex) {
                            errorHandler("actionSetArea", ex, callbackContext);
                        }
                    }));
                }
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("actionSetArea", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("actionSetArea", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("actionSetArea", ex, callbackContext);
            }
        }));
    }

    private void mapGetAreas(String message, CallbackContext callbackContext) {
        System.out.println("MapGetAreas. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        final Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        int mapId = Integer.parseInt(jsonMap.get("map_id").toString());

        if (currentPermanent != 0) {
            mapId = currentPermanent;
        }

        System.out.println("MapGetAreas. MapID : " + mapId);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetAreasRobotCommand(mapId, new RequestCallbackWithResult<Areas>() {
            @Override
            public void onSuccess(Areas result) {
                System.out.println("MapGetAreas. Result : " + result);
                StringBuilder points = new StringBuilder();
                StringBuilder areas = new StringBuilder();
                String type = "";
                String temporalArea = "";
                String pointsString = "";

                for (Area currentArea : result.getAreas()) {
                    System.out.println("MapGetAreas. Area : " + currentArea);
                    type = "room";
                    points = new StringBuilder();

                    if (currentArea.getAreaType() == AreaType.TO_BE_CLEANED) {
                        type = "to_be_cleaned";
                    }

                    for (Point2D currentPoint : currentArea.getPoints()) {
                        points.append("{'x': " + currentPoint.getX() + ", 'y': " + currentPoint.getY() + "}, ");
                    }

                    pointsString = points.toString().replaceAll("'", "\"");
                    if (pointsString.length() > 2) {
                        pointsString = pointsString.substring(0, pointsString.length() - 2);
                    }

                    temporalArea = String.format(
                            "{'id': %d, 'cleaning_parameter_set': 1, 'area_meta_data': '%s', 'area_type': '%s', 'points': [%s]}, ",
                            currentArea.getAreaId(), currentArea.getMetaData(), type, pointsString);
                    System.out.println("MapGetAreas. Area JSON : " + temporalArea);
                    areas.append(temporalArea);
                }
                String areasString = areas.toString().replaceAll("'", "\"");

                if (areasString.length() > 2) {
                    areasString = areasString.substring(0, areasString.length() - 2);
                }

                int mapId = Integer.parseInt(jsonMap.get("map_id").toString());

                if (currentPermanent != 0) {
                    mapId = currentPermanent;
                }

                String response = String.format("{'map_id': %d, 'areas': [%s]}", mapId, areasString).replaceAll("'",
                        "\"");
                System.out.println("MapGetAreas. Response : " + response.replaceAll("'", "\""));
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetAreas", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetAreas", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetAreas", ex, callbackContext);
            }
        }));
    }

    private void saveMap(String message, CallbackContext callbackContext) {
        System.out.println("SaveMap. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("SaveMap. MapID : " + jsonMap.get("map_id").toString());
        int mapId = Integer.parseInt(jsonMap.get("map_id").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new SaveMapRobotCommand(mapId, new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = ("{'cmd_id': " + result.getCommandId() + "}").replaceAll("'", "\"");
                System.out.println("SaveMap. Response : " + result);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("SaveMap", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("SaveMap", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("SaveMap", ex, callbackContext);
            }
        }));
    }

    private void mapGetTiles(String message, CallbackContext callbackContext) {
        System.out.println("MapGetTiles. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("MapGetTiles. MapID : " + jsonMap.get("map_id").toString());
        int mapId = Integer.parseInt(jsonMap.get("map_id").toString());
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetTileMapRobotCommand(mapId, new RequestCallbackWithResult<TileMap>() {

            @Override
            public void onSuccess(TileMap result) {
                StringBuilder sb = new StringBuilder();
                StringBuilder sbLines = new StringBuilder();

                for (Line l : result.getLines()) {
                    sbLines.append(String.format("{'x1': %s, 'y1': %s, 'x2': %s, 'y2': %s}, ", l.getStartPoint().getX(),
                            l.getStartPoint().getY(), l.getEndPoint().getX(), l.getEndPoint().getY()));
                }

                String stringLines = "";

                if (sbLines.toString().length() > 2) {
                    stringLines = sbLines.toString().substring(0, sbLines.toString().length() - 2).replaceAll("'",
                            "\"");
                }

                sb.append(String.format(
                        "{'map': {'map_id': %d, 'lines': [%s], 'docking_pose': {'x': %s, 'y': %s, 'heading': %s, 'valid': %b}}}",
                        result.getMapId(), stringLines, result.getDockingPose().getX(), result.getDockingPose().getY(),
                        result.getDockingPose().getHeading(), result.getDockingPose().isValid()));

                String response;
                response = sb.toString().replaceAll("'", "\"");

                System.out.println("MapGetTiles. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetTiles", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetTiles", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetTiles", ex, callbackContext);
            }
        }));
    }

    private void getCommandResult(String message, CallbackContext callbackContext) {
        System.out.println("GetCommandResult. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector()
                .sendCommand(new GetCommandResultRobotCommand(new RequestCallbackWithResult<List<CommandResult>>() {
                    @Override
                    public void onSuccess(final List<CommandResult> result) {
                        StringBuffer response1 = new StringBuffer("");
                        StringBuffer lines = new StringBuffer("");

                        for (CommandResult currElem : result) {
                            String status = "unknown";

                            if (currElem.getCommandStatus() == CommandStatus.QUEUED) {
                                status = "queued";
                            } else if (currElem.getCommandStatus() == CommandStatus.SKIPPED) {
                                status = "skipped";
                            } else if (currElem.getCommandStatus() == CommandStatus.EXECUTING) {
                                status = "executing";
                            } else if (currElem.getCommandStatus() == CommandStatus.DONE) {
                                status = "done";
                            } else if (currElem.getCommandStatus() == CommandStatus.ERROR) {
                                status = "error";
                            } else if (currElem.getCommandStatus() == CommandStatus.INTERRUPTED) {
                                status = "interrupted";
                            } else if (currElem.getCommandStatus() == CommandStatus.ABORTED) {
                                status = "aborted";
                            }

                            response1.append(
                                    String.format("{'cmd_id': %s, 'status': '%s'}, ", currElem.getCommandId(), status));
                        }

                        if (response1.length() > 0) {
                            lines.append(String.format("[%s]",
                                    response1.toString().substring(0, response1.toString().length() - 2)));
                        } else {
                            lines.append("[]");
                        }

                        System.out.println("GetCommandResult. Calling : " + lines);
                        callbackContext.success(lines.toString().replaceAll("'", "\""));
                    }

                    @Override
                    public void onAuthenticationError(AuthenticationException ex) {
                        errorHandler("GetCommandResult", ex, callbackContext);
                    }

                    @Override
                    public void onConnectionError(ConnectionException ex) {
                        errorHandler("GetCommandResult", ex, callbackContext);
                    }

                    @Override
                    public void onRequestError(RequestException ex) {
                        errorHandler("GetCommandResult", ex, callbackContext);
                    }
                }));
    }

    private void actionSetSuctionControl(String message, CallbackContext callbackContext) {
        System.out.println("ActionSetSuctionControl. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        int paramSet = Integer.parseInt(jsonMap.get("cleaning_parameter_set").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new SetCleaningParameterSetRobotCommand(paramSet, new RequestCallback() {
            @Override
            public void onSuccess() {
                String response = "{}";
                System.out.println("ActionSetSuctionControl. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ActionSetSuctionControl", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ActionSetSuctionControl", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ActionSetSuctionControl", ex, callbackContext);
            }
        }));
    }

    private void mapGetMaps(String message, CallbackContext callbackContext) {
        System.out.println("MapGetMaps. Before Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {

            @Override
            public void onSuccess(List<MapInfo> result) {
                StringBuilder sb = new StringBuilder();
                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        currentPermanent = mi.getMapId();
                        sb.append(String.format("{'map_id': %s, 'permanent': 1}, ", mi.getMapId()));
                    } else {
                        sb.append(String.format("{'map_id': %s, 'permanent': 0}, ", mi.getMapId()));
                    }
                }

                String response;
                if (sb.toString().length() > 2) {
                    response = "{\"maps\": [" + sb.toString().substring(0, sb.toString().length() - 2)
                            + "]}".replaceAll("'", "\"");
                } else {
                    response = "{\"maps\": []}".replaceAll("'", "\"");
                }

                System.out.println("MapGetMaps. Response : " + response.replaceAll("'", "\""));
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetMaps", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetMaps", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetMaps", ex, callbackContext);
            }
        }));
    }

    private void mapDelete(String message, CallbackContext callbackContext) {
        System.out.println("MapDelete. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("MapDelete. MapID : " + jsonMap.get("map_id").toString());

        RequestCallbackWithCommandID a = null;
        Integer mapId = Integer.parseInt(jsonMap.get("map_id").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new DeleteMapRobotCommand(mapId.intValue(), new RequestCallbackWithCommandID() {
            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapDelete", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapDelete", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapDelete", ex, callbackContext);
            }

            @Override
            public void onSuccess(CommandId arg0) {
                currentPermanent = 0;
                String response = ("{'cmd_id': " + arg0.getCommandId() + "}").replaceAll("'", "\"");

                System.out.println("MapDelete. Response : " + response);
                callbackContext.success(response);
            }
        }));
    }


    private void robotGetEventList(String message, CallbackContext callbackContext) {
        System.out.println("RobotGetEventList. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetEventLogRobotCommand(0, new RequestCallbackWithResult<List<Event>>() {
            @Override
            public void onSuccess(final List<Event> result) {

                StringBuilder sb = new StringBuilder();
                Calendar cal = Calendar.getInstance();

                for (Event currEvent : result) {
                    cal.setTimeInMillis(currEvent.getTimestamp().getTimeInMillis());
                    String timestamp = String.format("{'year': %d,'month':%d,'day':%d,'hour':%d,'min':%d,'sec':%d}",
                        cal.get(Calendar.YEAR),
                        (cal.get(Calendar.MONTH) + 1),
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND));

                    sb.append(String.format(
                            "{'id': %d, 'type': '%s', 'type_id': %d, 'timestamp': %s, 'map_id': %d, 'area_id': %d, 'source_type': '%s','source_id': %d, 'hierarchy': '%s'}, ",
                            currEvent.getId(), currEvent.getEventType(), currEvent.getEventType().getTypeId(),
                            timestamp, currEvent.getMapID(), currEvent.getAreaID(), "", 0,
                            ""));
                }

                String response = "{'robot_events': [] }";
                if (sb.toString().length() > 2) {
                    response = sb.toString().substring(0, sb.toString().length() - 2).replaceAll("'", "\"");
                    response = String.format("{'robot_events': [%s]}", response);
                }

                System.out.println("RobotGetEventList. Response : " + response.replaceAll("'", "\""));
                callbackContext.success(response.replaceAll("'", "\""));
            }

            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("RobotGetEventList", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("RobotGetEventList", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("RobotGetEventList", ex, callbackContext);
            }
        }));
    }

    private void robotSetName(String message, CallbackContext callbackContext) {
        System.out.println("RobotSetName. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("RobotSetName. Name : " + jsonMap.get("name").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new SetRobotNameRobotCommand(jsonMap.get("name").toString(), new RequestCallback() {
            @Override
            public void onSuccess() {
                System.out.println("RobotSetName. Response : {}");
                callbackContext.success("{}");
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("RobotSetName", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("RobotSetName", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("RobotSetName", ex, callbackContext);
            }
        }));
    }

    private void mapGetCleaningGrid(String message, CallbackContext callbackContext) {
        System.out.println("MapGetCleaningGrid. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetCleaningGridMapRobotCommand(new RequestCallbackWithResult<CleaningGridMap>() {
            @Override
            public void onSuccess(CleaningGridMap result) {
                StringBuilder sbArea = new StringBuilder();

                int sizeX = result.getSizeX();
                int sizeY = result.getSizeY();
                for (int u = 0; u < sizeY; u++) {
                    for (int v = 0; v < sizeX; v++) {
                        if (result.getCleaned().get(u * sizeX + v) == 1) {
                            sbArea.append("1");
                        } else {
                            sbArea.append("0");
                        }
                    }
                }

                String stringArea = sbArea.toString();
                String encodedCleanedArea = encodeCleaningGrid(stringArea);
                String response = String.format(
                        "{'map_id': %d, 'lower_left_x': %s, 'lower_left_y': %s, 'size_x': %s, 'size_y': %s, 'resolution': %s, 'cleaned': %s}",
                        result.getMapId(), result.getLowerLeftX(), result.getLowerLeftY(), result.getSizeX(),
                        result.getSizeY(), result.getResolution(), encodedCleanedArea).replaceAll("'", "\"");

                System.out.println("MapGetCleaningGrid. Response : " + response.replaceAll("'", "\""));
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetCleaningGrid", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetCleaningGrid", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetCleaningGrid", ex, callbackContext);
            }
        }));
    }

    private void mapGetRoomLayout(String message, CallbackContext callbackContext) {
        System.out.println("MapGetRoomLayout. Calling : " + message);
        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        System.out.println("MapGetRoomLayout. MapID : " + jsonMap.get("map_id").toString());
        int mapId = Integer.parseInt(jsonMap.get("map_id").toString());
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetFeatureMapRobotCommand(mapId, new RequestCallbackWithResult<FeatureMap>() {
            @Override
            public void onSuccess(FeatureMap featureMap) {
                StringBuilder sbLines = new StringBuilder();
                for (Line currSeg : featureMap.getLines()) {
                    sbLines.append(String.format("{'x1': %s, 'y1': %s, 'x2': %s, 'y2': %s}, ", currSeg.getX1(),
                            currSeg.getY1(), currSeg.getX2(), currSeg.getY2()));
                }
                String stringLines = "";
                if (sbLines.toString().length() > 2) {
                    stringLines = sbLines.toString().substring(0, sbLines.toString().length() - 2).replaceAll("'",
                            "\"");
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format(
                        "{'map': {'map_id': %d, 'lines': [%s], 'docking_pose': {'x': %s, 'y': %s, 'heading': %s, 'valid': %b}}}",
                        featureMap.getMapId(), stringLines, featureMap.getDockingPose().getX(),
                        featureMap.getDockingPose().getY(), featureMap.getDockingPose().getHeading(),
                        featureMap.getDockingPose().isValid()));

                String response;
                response = sb.toString().replaceAll("'", "\"");
                System.out.println("MapGetRoomLayout. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetRoomLayout", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetRoomLayout", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetRoomLayout", ex, callbackContext);
            }
        }));

    }

    private void mapGetMovableObjectsList(String message, CallbackContext callbackContext) {
        System.out.println("MapGetMovableObjectsList. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetNNPolygonsRobotCommand(new RequestCallbackWithResult<NNMap>() {
            @Override
            public void onSuccess(NNMap result) {
                StringBuilder sbMap = new StringBuilder();
                StringBuilder sbPolygons = new StringBuilder();
                StringBuilder sbSegments = new StringBuilder();
                String stringMap;
                String stringPolygons = "";
                String stringSegments = "";

                List<Polygon> nnPolys = result.getPolygons();
                for (Polygon currPoly : nnPolys) {
                    List<Line> segs = currPoly.getLines();
                    for (Line l : segs) {
                        sbSegments.append(String.format("{'x1': %s, 'y1': %s, 'x2': %s, 'y2': %s}, ", l.getX1(),
                                l.getY1(), l.getX2(), l.getY2()));
                    }
                    stringSegments = sbSegments.substring(0, sbSegments.toString().length() - 2).replaceAll("'", "\"");
                    sbPolygons.append(String.format("{'segments': [%s]}, ", stringSegments));
                }
                stringPolygons = sbPolygons.substring(0, sbPolygons.toString().length() - 2).replaceAll("'", "\"");
                sbMap.append(String.format("{'map': {'map_id': %d 'polygons': [%s], 'timestamp': 0}}",
                        result.getMapId(), stringPolygons));
                stringMap = sbMap.toString().replaceAll("'", "\"");
                System.out.println("MapGetRoomLayout. Response : " + stringMap);
                callbackContext.success(stringMap);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetMovableObjectsList", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetMovableObjectsList", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetMovableObjectsList", ex, callbackContext);
            }
        }));
    }

    private void scheduleAdd(String message, CallbackContext callbackContext) {
        System.out.println("ScheduleAdd. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final String days_of_week = jsonMap.get("days_of_week").toString();
        final Integer hour = Integer.parseInt(jsonMap.get("hour").toString());
        final Integer min = Integer.parseInt(jsonMap.get("min").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
            @Override
            public void onSuccess(List<MapInfo> result) {
                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        RobartIoTCordovaPlugin.currentPermanent = mi.getMapId();
                    }
                }

                Task task = Task.builder().mode(CleaningMode.CLEANING_MODE_CLEAN_MAP)
                        .cleaningParameterSet(CleaningParameterSet.DEFAULT)
                        .mapId(RobartIoTCordovaPlugin.currentPermanent).parameter(null).build();

                List<String> daysOfWeek = asList(days_of_week.split("\\s*,\\s*"));
                List<Integer> daysOfWeekInteger = new ArrayList<Integer>();
                for (String day : daysOfWeek) {
                    daysOfWeekInteger.add(Integer.parseInt(day));
                }

                ScheduledTask scheduledTask = ScheduledTask.builder().id(0).currentTask(task).hour(hour).minute(min)
                        .enabled(true).daysOfWeek(daysOfWeekInteger).build();

                RobartSDKFactory.getDefaultAICUConnector().sendCommand(
                        new AddScheduledTaskRobotCommand(scheduledTask, new RequestCallbackWithCommandID() {
                            @Override
                            public void onSuccess(CommandId result) {
                                String response = String.format("{'cmd_id': %d}", result.getCommandId()).toString()
                                        .replaceAll("'", "\"");
                                System.out.println("ScheduleAdd. Response : " + response);
                                callbackContext.success(response);
                            }

                            @Override
                            public void onAuthenticationError(AuthenticationException ex) {
                                errorHandler("ScheduleAdd", ex, callbackContext);
                            }

                            @Override
                            public void onConnectionError(ConnectionException ex) {
                                errorHandler("ScheduleAdd", ex, callbackContext);
                            }

                            @Override
                            public void onRequestError(RequestException ex) {
                                errorHandler("ScheduleAdd", ex, callbackContext);
                            }
                        }));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ScheduleAdd", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ScheduleAdd", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ScheduleAdd", ex, callbackContext);
            }
        }));
    }

    private void scheduleEdit(String message, CallbackContext callbackContext) {
        System.out.println("scheduleEdit. Calling : " + message);

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final Integer task_id = Integer.parseInt(jsonMap.get("task_id").toString());
        final Integer enabled = Integer.parseInt(jsonMap.get("enabled").toString());
        final String days_of_week = jsonMap.get("days_of_week").toString();
        final Integer hour = Integer.parseInt(jsonMap.get("hour").toString());
        final Integer min = Integer.parseInt(jsonMap.get("min").toString());

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
            @Override
            public void onSuccess(List<MapInfo> result) {
                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        RobartIoTCordovaPlugin.currentPermanent = mi.getMapId();
                    }
                }
                Task task = Task.builder().mode(CleaningMode.CLEANING_MODE_CLEAN_ALL)
                        .cleaningParameterSet(CleaningParameterSet.SILENT)
                        .mapId(RobartIoTCordovaPlugin.currentPermanent).parameter(null).build();

                boolean isEnabled = (enabled != 0);
                List<String> daysOfWeek = asList(days_of_week.split("\\s*,\\s*"));
                List<Integer> daysOfWeekInteger = new ArrayList<Integer>();
                for (String day : daysOfWeek) {
                    daysOfWeekInteger.add(Integer.parseInt(day));
                }

                ScheduledTask scheduledTask = ScheduledTask.builder().id(task_id).currentTask(task).hour(hour)
                        .minute(min).enabled(isEnabled).daysOfWeek(daysOfWeekInteger).build();

                RobartSDKFactory.getDefaultAICUConnector().sendCommand(
                        new ModifyScheduledTaskRobotCommand(scheduledTask, new RequestCallbackWithCommandID() {
                            @Override
                            public void onSuccess(CommandId result) {
                                String response = String.format("{'cmd_id': %d}", result.getCommandId()).toString()
                                        .replaceAll("'", "\"");
                                System.out.println("scheduleEdit. Response : " + response);
                                callbackContext.success(response);
                            }

                            @Override
                            public void onAuthenticationError(AuthenticationException ex) {
                                errorHandler("scheduleEdit", ex, callbackContext);
                            }

                            @Override
                            public void onConnectionError(ConnectionException ex) {
                                errorHandler("scheduleEdit", ex, callbackContext);
                            }

                            @Override
                            public void onRequestError(RequestException ex) {
                                errorHandler("scheduleEdit", ex, callbackContext);
                            }
                        }));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("scheduleEdit", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("scheduleEdit", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("scheduleEdit", ex, callbackContext);
            }
        }));
    }

    private void scheduleList(String message, CallbackContext callbackContext) {
        System.out.println("ScheduleList. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetScheduleRobotCommand(new RequestCallbackWithResult<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                System.out.println("ScheduleList. Raw Response: " + result.getSchedule());
                final StringBuilder sb = new StringBuilder();
                Iterator<ScheduledTask> it = result.getSchedule().iterator();
                while (it.hasNext()) {
                    ScheduledTask scheduledTask = it.next();
                    sb.append(String.format(
                            "{'task_id': %d, 'enabled': %b, 'time':{'days_of_week': [%s],'hour': %d,'min': %d,'sec':0}, 'task':{'map_id': %d,'cleaning_parameter_set': 1,'cleaning_mode': 1 }}, ",
                            scheduledTask.getID(), scheduledTask.isEnabled(), scheduledTask.getDayOfWeekListAsString(),
                            scheduledTask.getHour(), scheduledTask.getMinute(), scheduledTask.getTask().getMapId()));
                }
                String response = "";
                if (sb.toString().length() > 2) {
                    response = sb.toString().substring(0, sb.toString().length() - 2).replaceAll("'", "\"");
                    response = String.format("{'schedule': [%s]}", response).replaceAll("'", "\"");
                } else {
                    response = String.format("{'schedule': []}", response).replaceAll("'", "\"");
                }

                System.out.println("ScheduleList. Response: " + response.replaceAll("'", "\""));
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("ScheduleList", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("ScheduleList", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("ScheduleList", ex, callbackContext);
            }
        }));
    }

    private void sendHomeRobot(String message, CallbackContext callbackContext) {
        System.out.println("SendHomeRobot. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GoHomeRobotCommand(new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = "{'response': 'success'}";
                System.out.println("SendHomeRobot. Response: " + result.getCommandId());
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("SendHomeRobot", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("SendHomeRobot", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("SendHomeRobot", ex, callbackContext);
            }
        }));
    }

    private void stopRobot(String message, CallbackContext callbackContext) {
        System.out.println("StopRobot. Calling: " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new StopRobotCommand(new RequestCallbackWithCommandID() {
            @Override
            public void onSuccess(CommandId result) {
                String response = "{'response': 'success'}";
                System.out.println("StopRobot. Response: " + result.getCommandId());
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("StopRobot", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("StopRobot", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("StopRobot", ex, callbackContext);
            }
        }));
    }

    private void mapGetRobotPosition(String message, CallbackContext callbackContext) {
        System.out.println("MapGetRobotPosition. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotPoseRobotCommand(new RequestCallbackWithResult<RobPose>() {
            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("MapGetRobotPosition", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("MapGetRobotPosition", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("MapGetRobotPosition", ex, callbackContext);
            }

            @Override
            public void onSuccess(final RobPose result) {
                System.out.println("MapGetRobotPosition. RobPose: " + result);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format(
                        "{'map_id': %d, 'x1': %s, 'y1': %s, 'heading': %s, 'valid': true, 'timestamp': %s}",
                        result.getMapId(), result.getPose().getX().floatValue(), result.getPose().getY().floatValue(),
                        result.getPose().getHeading(), true, result.getTimestamp().longValue() * 1000));
                String response = sb.toString().replaceAll("'", "\"");

                char[] array = response.toCharArray();
                StringBuilder t = new StringBuilder();

                Boolean negative = false;
                for (char ch: array) {
                    if (ch == '-') {
                        t.append("'");
                        negative = true;
                    }

                    if ((ch == ',' || ch == '}') && negative) {
                        t.append("'");
                        negative = false;
                    }

                    t.append(ch);
                }

                String message = t.toString().replaceAll("'", "\"").replaceAll(":\"\"", ":null");

                System.out.println("MapGetRobotPosition. Response :" + message);
                callbackContext.success(message);
            }
        }));
    }

    private void robotGetRobotId(String message, CallbackContext callbackContext) {
        System.out.println("RobotGetRobotId. Calling : " + message);

        if (this.configurationData == null) {
            this.configurationData = new ConfigurationData();
        }

        this.configurationData.initServer(this);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotIDRobotCommand(new RequestCallbackWithResult<Robot>() {
            @Override
            public void onSuccess(final Robot result1) {
                RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotNameRobotCommand(new RequestCallbackWithResult<String>() {
                    @Override
                    public void onSuccess(final String result2) {
                        String response = String.format(
                                "{'name': '%s', 'unique_id': '%s', 'camlas_unique_id': '%s', 'model': '%s', 'firmware': '%s'}",
                                result2, result1.getUniqueId().toString(), result1.getUniqueId().toString(),
                                result1.getModel().toString(), result1.getFirmware().toString());

                        System.out.println("RobotGetRobotId. Response: " + response);
                        callbackContext.success(response.replaceAll("'", "\""));
                    }

                    @Override
                    public void onAuthenticationError(AuthenticationException ex) {
                        errorHandler("RobotGetRobotId", ex, callbackContext);
                    }

                    @Override
                    public void onConnectionError(ConnectionException ex) {
                        errorHandler("RobotGetRobotId", ex, callbackContext);
                    }

                    @Override
                    public void onRequestError(RequestException ex) {
                        errorHandler("RobotGetRobotId", ex, callbackContext);
                    }
                }));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("RobotGetRobotId", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("RobotGetRobotId", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("RobotGetRobotId", ex, callbackContext);
            }
        }));
    }

    private static String encodeRLE(String source) {
        StringBuffer dest = new StringBuffer();
        for (int i = 0; i < source.length(); i++) {
            int runLength = 1;
            while (i + 1 < source.length() && source.charAt(i) == source.charAt(i + 1)) {
                runLength++;
                i++;
            }
            dest.append(runLength);
            dest.append(",");
            //dest.append(source.charAt(i));
        }
        String str = dest.toString();
        return str.substring(0, str.length() - 1);
    }

    private static String encodeCleaningGrid(String text) {
        String result;
        String response;
        if (text == null || text.length() == 0 || text.isEmpty()) {
            return "[]";
        }
        result = encodeRLE(text);
        if (text.charAt(1) == '1') {
            response = String.format("[0, %s]", result);
        } else {
            response = String.format("[1, %s]", result);
        }
        return response;
    }

    ;

    private void connectToWifi(String message, CallbackContext callbackContext) {

        JsonParserFactory factory = JsonParserFactory.getInstance();
        JSONParser parser = factory.newJsonParser();
        Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

        final String ssid = jsonMap.get("ssid").toString();
        String passphrase = jsonMap.get("passphrase").toString();
        System.out.println("ConnectToWifi, Connecting to Wifi: " + ssid);

        getClient().connectToWifi(ssid, passphrase).subscribe(value -> {
            ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            es.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println("ConnectToWifi. response success");
                    String response = "{}";
                    callbackContext.success(response);
                }
            }, 1, TimeUnit.SECONDS);

        }, throwable -> {
            System.out.println("ConnectToWifi, Exception:" + throwable);
        });
    }

    private void searchWifi(String message, CallbackContext callbackContext) {
        System.out.println("SearchWifi. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetAvailableWifiListFromRobot(new RequestCallbackWithResult<WIFIScanResults>() {
            @Override
            public void onSuccess(final WIFIScanResults result) {
                StringBuilder sbList = new StringBuilder();
                String stringList;
                String response;

                for (WIFIScanResult currentWifi : result.getScan()) {
                    sbList.append(String.format("{'ssid': '%s', 'rssi': '%s', 'pairwisecipher': '%s'}, ", currentWifi,
                            currentWifi.getRSSI(), currentWifi.getPairwiseciper()));
                }
                stringList = sbList.toString().replaceAll("'", "\"");

                if (stringList.length() > 2) {
                    stringList = stringList.substring(0, stringList.toString().length() - 2);
                }

                response = String.format("{'response': [%s]}", stringList).replaceAll("'", "\"");
                System.out.println("SearchWifi. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("SearchWifi", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("SearchWifi", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("SearchWifi", ex, callbackContext);
            }
        }));
    }

    private void startWifiScan(String message, CallbackContext callbackContext) {
        System.out.println("StartWifiScan. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new StartWifiScanRobotCommand(new RequestCallback() {
            @Override
            public void onSuccess() {
                String response = "{}";
                System.out.println("StartWifiScan. Response : " + response);
                callbackContext.success(response);
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("StartWifiScan", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("StartWifiScan", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("StartWifiScan", ex, callbackContext);
            }
        }));
    }

    private void getRobotStatus(String message, CallbackContext callbackContext) {
        System.out.println("GetRobotStatus. Calling : " + message);

        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotStatusRobotCommand(new RequestCallbackWithResult<RobotStatus>() {
            @Override
            public void onSuccess(final RobotStatus result) {
                String charging;
                String mode;

                if (result.getBatteryStatus().chargingState() == ChargingState.CHARGING) {
                    charging = "charging";
                } else if (result.getBatteryStatus().chargingState() == ChargingState.UNCONNECTED) {
                    charging = "unconnected";
                } else if (result.getBatteryStatus().chargingState() == ChargingState.CONNECTED) {
                    charging = "connected";
                } else {
                    charging = "unknown";
                }

                if (result.getMode() == Mode.READY) {
                    mode = "ready";
                } else if (result.getMode() == Mode.EXPLORING) {
                    mode = "exploring";
                } else if (result.getMode() == Mode.CLEANING) {
                    mode = "cleaning";
                } else if (result.getMode() == Mode.NOT_READY) {
                    mode = "not_ready";
                } else if (result.getMode() == Mode.DOCKING) {
                    mode = "go_home";
                } else if (result.getMode() == Mode.LIFTED) {
                    mode = "lifted";
                } else if (result.getMode() == Mode.TARGET_POINT) {
                    mode = "target_point";
                } else if (result.getMode() == Mode.RECOVERY) {
                    mode = "recovery";
                } else {
                    mode = "unknown";
                }

                String response = String.format(
                        "{'voltage': '%s', 'cleaningParameter': '%s', 'batteryLevel': '%s', 'chargingState': '%s', 'mode': '%s'}",
                        result.getBatteryStatus().voltage().toString(), result.getCleaningParamSet().toString(),
                        result.getBatteryStatus().level().toString(), charging, mode);

                System.out.println("GetRobotStatus. Response: " + response);
                callbackContext.success(response.replaceAll("'", "\""));
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("GetRobotStatus", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("GetRobotStatus", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("GetRobotStatus", ex, callbackContext);
            }
        }));
    }

    private void getRobotID(String message, CallbackContext callbackContext) {
        System.out.println("GetRobotID. Started.");
        RobartIoTCordovaPlugin plugin = this;

        if (plugin.robot == null) {
            System.out.println("GetRobotID. Robot is null");
            errorHandler("GetRobotID #0", new Exception("GetRobotID. Robot is null"), callbackContext);
        } else {
            System.out.println("GetRobotID. Robot: " + plugin.robot.getUniqueId());
        }

        try {
            RobartSDKFactory.connectToRobot(plugin.robot, new ConnectionListener() {
                @Override
                public void onConnect() {
                    System.out.println("GetRobotID. Successful on the connection to the robot");
                    try {
                        System.out.println("GetRobotID. Sending GetRobotIDRobotCommand");

                        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotIDRobotCommand(new RequestCallbackWithResult<Robot>() {
                            @Override
                            public void onSuccess(final Robot result1) {
                                RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetRobotNameRobotCommand(new RequestCallbackWithResult<String>() {
                                    @Override
                                    public void onSuccess(final String result2) {
                                        System.out.println("GetRobotID. Success response of the GetRobotNameRobotCommand method");
                                        String response = String.format(
                                                "{'name': '%s', 'unique_id': '%s', 'camlas_unique_id': '%s', 'model': '%s', 'firmware': '%s'}",
                                                result2, result1.getUniqueId().toString(), result1.getUniqueId().toString(),
                                                result1.getModel().toString(), result1.getFirmware().toString());

                                        System.out.println("GetRobotID. Response: " + response);
                                        callbackContext.success(response.replaceAll("'", "\""));
                                    }

                                    @Override
                                    public void onAuthenticationError(AuthenticationException ex) {
                                        errorHandler("GetRobotID #1", ex, callbackContext);
                                    }

                                    @Override
                                    public void onConnectionError(ConnectionException ex) {
                                        errorHandler("GetRobotID #2", ex, callbackContext);
                                    }

                                    @Override
                                    public void onRequestError(RequestException ex) {
                                        errorHandler("GetRobotID #3", ex, callbackContext);
                                    }
                                }));
                            }

                            @Override
                            public void onAuthenticationError(AuthenticationException ex) {
                                errorHandler("GetRobotID #4", ex, callbackContext);
                            }

                            @Override
                            public void onConnectionError(ConnectionException ex) {
                                errorHandler("GetRobotID #5", ex, callbackContext);
                            }

                            @Override
                            public void onRequestError(RequestException ex) {
                                errorHandler("GetRobotID #6", ex, callbackContext);
                            }
                        }));
                    } catch (Exception e1) {
                        System.out.println("GetRobotID, exception #3: " + e1.getMessage());
                        errorHandler("GetRobotID #7", e1, callbackContext);
                    }
                }

                @Override
                public void onConnectionError(final Throwable exception) {
                    System.out.println("GetRobotID, exception #1: " + exception.getMessage());
                    errorHandler("GetRobotID #8", exception, callbackContext);
                }

                @Override
                public void onAuthenticationError(final AuthenticationException exception) {
                    System.out.println("GetRobotID #9" + exception.getMessage());
                }

                @Override
                public void onDisconnect() {
                    System.out.println("GetRobotID #10, disconnected");
                }
            });

        } catch (Exception e2) {
            System.out.println("GetRobotID. AICUConnector.init Exception " + e2.getMessage());
            errorHandler("GetRobotID #11", e2, callbackContext);
        }
    }

    private void startRobot(String message, CallbackContext callbackContext) {
        System.out.println("StartRobot. Calling : " + message);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
            @Override
            public void onSuccess(List<MapInfo> result) {
                Boolean foundPM = false;

                for (MapInfo mi : result) {
                    if (mi.isPermanent()) {
                        RobartIoTCordovaPlugin.currentPermanent = mi.getMapId();
                        foundPM = true;
                    }
                }

                System.out.println("StartRobot. Found Permanent Map : " + foundPM);

                if (foundPM) {
                    Task task = Task.builder().mode(CleaningMode.CLEANING_MODE_CLEAN_MAP).mapId(currentPermanent)
                            .cleaningParameterSet(CleaningParameterSet.DEFAULT).build();
                    RobartSDKFactory.getDefaultAICUConnector().sendCommand(new CleanRobotCommand(task, new RequestCallbackWithCommandID() {
                        @Override
                        public void onSuccess(CommandId result) {
                            String response = "{'response': 'success'}";
                            System.out.println("StartRobot PM. Response: " + result.getCommandId());
                            callbackContext.success(response.replaceAll("'", "\""));
                        }

                        @Override
                        public void onAuthenticationError(AuthenticationException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }

                        @Override
                        public void onConnectionError(ConnectionException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }

                        @Override
                        public void onRequestError(RequestException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }
                    }));
                } else {
                    Task task = Task.builder().mode(CleaningMode.CLEANING_MODE_START_OR_CONTINUE).parameter(null)
                            .cleaningParameterSet(CleaningParameterSet.DEFAULT).build();
                    RobartSDKFactory.getDefaultAICUConnector().sendCommand(new CleanRobotCommand(task, new RequestCallbackWithCommandID() {
                        @Override
                        public void onSuccess(CommandId result) {
                            String response = "{'response': 'success'}";
                            System.out.println("StartRobot. Response: " + result.getCommandId());
                            callbackContext.success(response.replaceAll("'", "\""));
                        }

                        @Override
                        public void onAuthenticationError(AuthenticationException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }

                        @Override
                        public void onConnectionError(ConnectionException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }

                        @Override
                        public void onRequestError(RequestException ex) {
                            errorHandler("StartRobot", ex, callbackContext);
                        }
                    }));
                }
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("StartRobot", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("StartRobot", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("StartRobot", ex, callbackContext);
            }
        }));
    }

    private void reconnectRobot(String message, CallbackContext callbackContext) {
        System.out.println("ReconnectRobot, calling");
        connectRobot(message, callbackContext);
    }

    private void configureTime(String message, CallbackContext callbackContext) {
        int year, month, day, hour, min;

        Calendar calender = Calendar.getInstance();
        day = calender.get(Calendar.DAY_OF_MONTH);
        month = calender.get(Calendar.MONTH);
        year = calender.get(Calendar.YEAR);
        hour = calender.get(Calendar.HOUR_OF_DAY);
        min = calender.get(Calendar.MINUTE);
        System.out.println("SetRobotTimeRobotCommand: " + day + "-" + month + "-" + year + " " + hour + ":" + min);
        RobartSDKFactory.getDefaultAICUConnector().sendCommand(new SetRobotTimeRobotCommand(year, month, day, hour, min, new RequestCallback() {
            @Override
            public void onSuccess() {
                System.out.println("SetRobotTimeRobotCommand. Success");
            }

            @Override
            public void onAuthenticationError(AuthenticationException ex) {
                errorHandler("SetRobotTimeRobotCommand", ex, callbackContext);
            }

            @Override
            public void onConnectionError(ConnectionException ex) {
                errorHandler("SetRobotTimeRobotCommand", ex, callbackContext);
            }

            @Override
            public void onRequestError(RequestException ex) {
                errorHandler("SetRobotTimeRobotCommand", ex, callbackContext);
            }
        }));
    }

    public void finalizePairing() {
        System.out.println("BT: FinalizePairing started");
        RobartSDK.initialize(this.cordova.getActivity().getApplicationContext());

        getClient().finalizePairing().subscribe(aBoolean -> {
            System.out.println("Success on finalizing Pairing");
            this.justPaired = true;
        }, throwable -> {
            System.out.println("Error on finalizing Paring: " + throwable.getMessage());
        });
    }

    private void connectRobot(String message, CallbackContext callbackContext) {
        RobartIoTCordovaPlugin plugin = this;
        System.out.println("ConnectRobot. Calling. " + plugin.robot);
        RobartSDK.initialize(this.cordova.getActivity().getApplicationContext());

        if (plugin.robot != null) {
            System.out.println("ConnectRobot. The robot is already configured. Trying to connect");
            RobartSDKFactory.connectToRobot(plugin.robot, new ConnectionListener() {
                @Override
                public void onConnect() {
                    System.out.println("ConnectRobot. The robot is connected");
                    String response = String.format(
                            "{'name': '%s', 'unique_id': '%s', 'camlas_unique_id': '%s', 'model': '%s', 'firmware': '%s'}",
                            plugin.robot.getNickname(), plugin.robot.getUniqueId(), plugin.robot.getUniqueId(),
                            plugin.robot.getModel(), plugin.robot.getFirmware());
                    plugin.configureTime(message, callbackContext);
                    System.out.println("ConnectRobot, GetRobotID. Response: " + response);
                    callbackContext.success(response.replaceAll("'", "\""));
                }

                @Override
                public void onConnectionError(final Throwable exception) {
                    System.out.println("ConnectRobot, GetRobotID, exception #8: " + exception.getMessage());
                    plugin.robot = null;
                    plugin.connectRobot(message, callbackContext);
                }

                @Override
                public void onAuthenticationError(final AuthenticationException exception) {
                    System.out.println("ConnectRobot, GetRobotID #9" + exception.getMessage());
                    plugin.robot = null;
                    plugin.connectRobot(message, callbackContext);
                }

                @Override
                public void onDisconnect() {
                    System.out.println("ConnectRobot, GetRobotID #10, disconnected");
                    plugin.robot = null;
                    plugin.connectRobot(message, callbackContext);
                }
            });
        } else {
            JsonParserFactory factory = JsonParserFactory.getInstance();
            JSONParser parser = factory.newJsonParser();
            Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

            String iotUserLogin = jsonMap.get("iotUserLogin").toString();
            String robotUid = jsonMap.get("robotUid").toString();
            String robotPassword = jsonMap.get("robotPassword").toString();
            String serverEndpoint = jsonMap.get("serverEndpoint").toString();
            String robotEndpoint = jsonMap.get("robotEndpoint").toString();
            String stsk = jsonMap.get("stsk").toString();

            serverEndpoint = serverEndpoint.replaceAll("https://", "").replaceAll("/iot/", "");

            if (robotUid == null || robotUid.isEmpty() || robotUid.equals("null")) {
                robotUid = this.uid;
            }

            this.configurationData = new ConfigurationData(iotUserLogin, robotUid, robotPassword, serverEndpoint,
                    robotEndpoint, stsk);

            this.configurationData.initServer(this);
            this.configurationData.getIotManager().registerDevice(this.configurationData.getStsk(), this.configurationData.getIotUserLogin()).subscribe(
                    onRegisterDeviceSuccess(message, callbackContext, this.configurationData),
                    onRegisterDeviceError(message, callbackContext));
        }
    }

    private Consumer<? super KeyValuePair> onRegisterDeviceSuccess(String message, CallbackContext callbackContext,
                                                           ConfigurationData cData) {
        return new Consumer<KeyValuePair>() {
            @Override
            public void accept(@NonNull KeyValuePair user) throws Exception {
                System.out.println("The device was registered. Starting the pairing, it will require some seconds");
                cData.getRobartIoTCordovaPluginInstance().schedulePairingInitialize(message, callbackContext, cData);
            }
        };
    }

    private Consumer<? super Throwable> onRegisterDeviceError(String message, CallbackContext callbackContext) {
        return new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                System.out.println("Invalid STSK");
                callbackContext.error("{'error': 'Invalid STSK'}".replaceAll("'", "\""));
            }
        };
    }

    private void schedulePairingInitialize(String message, CallbackContext callbackContext, ConfigurationData cData) {
        cData.initServer(cData.getRobartIoTCordovaPluginInstance());

        PairingInfoRequest pairingInfoRequest = PairingInfoRequest.builder().username(cData.getIotUserLogin())
                .robotPassword(cData.getRobotPassword()).build();
        cData.getIotManager().initPairing(pairingInfoRequest, cData.getRobotUid()).subscribe(
                getInitPairingOnNext(message, callbackContext, cData),
                getInitPairingOnError(message, callbackContext, cData));
    }

    private Consumer<? super Throwable> getInitPairingOnError(String message, CallbackContext callbackContext,
                                                              ConfigurationData cData) {
        return (Consumer<Throwable>) throwable -> {
            if (throwable instanceof RequestException) {
                System.out.println(
                        "error in initializing the pairing. code:" + ((RequestException) throwable).getErrorCode());
            } else {
                System.out.println("error in initializing the pairing.");
            }

            this.schedulePairingInitialize(message, callbackContext, cData);
            // cData.getRobartIoTCordovaPluginInstance().errorHandler("GetInitPairingOnError", new Exception(throwable),
            // 		callbackContext);
        };
    }

    private Consumer<? super PairingStatus> getInitPairingOnNext(String message, CallbackContext callbackContext,
                                                                 ConfigurationData cData) {
        return (Consumer<PairingStatus>) responsePS -> {
            System.out.println("Pairing, response from server: " + responsePS.getResponse());

            if (responsePS.getResponse().equals("already paired") || responsePS == PairingStatus.ALREADY_PAIRED) {
                cData.getIotManager().checkPairedRobots(cData.getIotUserLogin()).subscribe(
                        getCheckPairingOnNext(message, callbackContext, cData),
                        getCheckPairingOnError(message, callbackContext, cData));
            } else {
                schedulePairingCheck(message, callbackContext, cData);
            }
        };
    }

    private void schedulePairingCheck(String message, CallbackContext callbackContext, ConfigurationData cData) {
        cData.getIotManager().checkPairedStatus(cData.getIotUserLogin(), cData.getRobotUid())
                .subscribe(getPairedStatusOnNext(message, callbackContext, cData), getPairedStatusOnError(message, callbackContext, cData));
    }

    public Consumer<? super PairingStatus> getPairedStatusOnNext(String message, CallbackContext callbackContext, ConfigurationData cData) {
        return (Consumer<PairingStatus>) val -> {
            System.out.println("Pairing, status:" + val.getResponse());

            cData.getIotManager().checkPairedRobots(cData.getIotUserLogin()).subscribe(
                    getCheckPairingOnNext(message, callbackContext, cData),
                    getCheckPairingOnError(message, callbackContext, cData));
        };
    }

    public Consumer<? super Throwable> getPairedStatusOnError(String message, CallbackContext callbackContext, ConfigurationData cData) {
        return (Consumer<Throwable>) throwable -> {
            if (throwable instanceof RequestException) {
                System.out.println("Pairing, error in getting the paired status. code:" + ((RequestException) throwable).getErrorCode());
            } else {
                System.out.println("Pairing, error in getting the paired status:" + throwable.getMessage());
            }
            // callbackContext.error("{'error': 'Check the console'}".replaceAll("'", "\""));
            this.schedulePairingCheck(message, callbackContext, cData);
        };
    }

    public Consumer<? super Throwable> getCheckPairingOnError(String message, CallbackContext callbackContext,
                                                              ConfigurationData cData) {
        return (Consumer<Throwable>) throwable1 -> {
            System.out.println("Pairing, getPairingStatus error occured: " + throwable1.getMessage());
            System.out.println("Pairing, this error is because the robot is already paired, so, FinalizePairing is started");

            cData.getRobartIoTCordovaPluginInstance().finalizePairing();
            callbackContext.error("{'error': 'Check the console'}".replaceAll("'", "\""));
        };
    }

    public Consumer<? super List<RobotImpl>> getCheckPairingOnNext(String message, CallbackContext callbackContext,
                                                                   ConfigurationData cData) {
        return (Consumer<List<RobotImpl>>) robotInfos -> {
            System.out.println("Pairing, getPairingStatus completed. Paired robots: " + robotInfos.size());
            Boolean paired = false;
            RobotImpl currElem = null;
            String uid1 = cData.getRobotUid();

            if (uid1 == null) {
                uid1 = this.uid;
            }

            for (RobotImpl robot : robotInfos) {
                if (robot.getUniqueId().equals(uid1)) {
                    paired = true;
                    currElem = robot;
                    System.out.println("Pairing, Found the robot!!");
                }
            }

            if (paired) {
                System.out.println("Pairing, success, the robot is paired");
                // cData.getRobartIoTCordovaPluginInstance().finalizePairing();
                RobartIoTCordovaPlugin plugin = cData.getRobartIoTCordovaPluginInstance();
                cData.initServer(plugin);

                plugin.robot = currElem;
                RobartSDKFactory.connectToRobot(currElem, new ConnectionListener() {
                    @Override
                    public void onConnect() {
                        System.out.println("Pairing, Connected to the robot #2, FinalizePairing started");
                        RobartSDK.initialize(cData.getRobartIoTCordovaPluginInstance().cordova.getActivity().getApplicationContext());

                        if (plugin.firstTimeConfiguration) {
                            getClient().finalizePairing().subscribe(aBoolean -> {
                                System.out.println("Pairing, success on finalizing Pairing: " + aBoolean);
                                cData.getRobartIoTCordovaPluginInstance().justPaired = true;
                                cData.initServer(cData.getRobartIoTCordovaPluginInstance());
                                //AICUConnector.stopFindRobot();
                                plugin.configureTime(message, callbackContext);
                                callbackContext.success("{'response': 'Connected to the robot'}".replaceAll("'", "\""));
                            }, throwable -> {
                                System.out.println("Pairing, error on finalizing Paring: " + throwable.getMessage());
                            });
                        } else {
                            cData.initServer(cData.getRobartIoTCordovaPluginInstance());
                            plugin.configureTime(message, callbackContext);
                            callbackContext.success("{'response': 'Connected to the robot'}".replaceAll("'", "\""));
                        }
                    }

                    @Override
                    public void onConnectionError(final Throwable exception) {
                        System.out.println("Pairing, exception: " + exception.getMessage());
                        callbackContext
                                .error("{'error': 'exception in connecting to robot'}".replaceAll("'", "\""));
                    }

                    @Override
                    public void onAuthenticationError(final AuthenticationException exception) {
                        System.out.println("Exception: " + exception.getMessage());
                        callbackContext
                                .error("{'error': 'exception in connecting to robot'}".replaceAll("'", "\""));
                    }

                    @Override
                    public void onDisconnect() {
                        System.out.println("Disconnected");
                    }
                });
            } else {
                cData.getRobartIoTCordovaPluginInstance().pairingCheckCounts = cData
                        .getRobartIoTCordovaPluginInstance().pairingCheckCounts + 1;
                cData.getIotManager().checkPairedRobots(cData.getIotUserLogin()).subscribe(
                        getCheckPairingOnNext(message, callbackContext, cData),
                        getCheckPairingOnError(message, callbackContext, cData));
            }
        };
    }

    private void getFirmware(String message, CallbackContext callbackContext) {
        System.out.println("GetFirmware. Started");
        String uniqueId =  RobartSDKFactory.getDefaultAICUConnector().getCurrentRobotId();
        System.out.println("GetFirmware. UniqueId: " + uniqueId);

        FirmwareManager firmwareManager = new FirmwareManagerImpl();
        firmwareManager.getLatestAvailableFirmware(uniqueId).subscribe(
                getLatestAvailableFirmwareOnSuccess(message, callbackContext),
                getLatestAvailableFirmwareOnError(message, callbackContext));
    }

    public Consumer<? super Throwable> getLatestAvailableFirmwareOnError(String message, CallbackContext callbackContext) {
        return (Consumer<Throwable>) e -> {
            System.out.println("GetFirmware, exception: " + e.getMessage());
            callbackContext.error("{'error': 'exception on GetFirmware'}".replaceAll("'", "\""));
        };
    }

    public Consumer<? super String> getLatestAvailableFirmwareOnSuccess(String message, CallbackContext callbackContext) {
        return new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) throws Exception {
                String response = ("{'firmware': '" + s + "'}").replaceAll("'", "\"");
                System.out.println("GetFirmware. Response : " + response);
                callbackContext.success(response);
            }
        };
    }

    private DisposableObserver<String> getDisposableObserver(String message, CallbackContext callbackContext) {
        return new DisposableObserver<String>() {
            @Override
            public void onNext(String o) {
                System.out.println("DownloadFirmware. Downloading, but not complete. Results: " + o);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("DownloadFirmware, exception: " + t.getMessage());
                callbackContext.error("{'error': 'exception on DownloadFirmware'}".replaceAll("'", "\""));
            }

            @Override
            public void onComplete() {
                String response = ("{'success': true}").replaceAll("'", "\"");
                System.out.println("DownloadFirmware. Response : " + response);
                callbackContext.success(response);
            }
        };
    }

}
