package cordova.plugin.robart.iot;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

import cc.robart.aicu.android.sdk.retrofit.client.FirmwareManager;
import cc.robart.aicu.android.sdk.retrofit.client.FirmwareManagerImpl;
import cc.robart.aicu.android.sdk.commands.InstallFirmware;
import android.content.Context;
import cc.robart.aicu.android.sdk.AICUConnector;
import cc.robart.aicu.android.sdk.application.RobartSDK;
import cc.robart.aicu.android.sdk.commands.GoHomeRobotCommand;
import cc.robart.aicu.android.sdk.commands.SetRobotNameRobotCommand;
import cc.robart.aicu.android.sdk.commands.SetRobotTimeRobotCommand;
import cc.robart.aicu.android.sdk.commands.StartWifiScanRobotCommand;
import cc.robart.aicu.android.sdk.commands.StopRobotCommand;
import cc.robart.aicu.android.sdk.datatypes.CommandId;
import cc.robart.aicu.android.sdk.datatypes.CommandResult;
import cc.robart.aicu.android.sdk.datatypes.CommandStatus;
import cc.robart.aicu.android.sdk.datatypes.Event;
import cc.robart.aicu.android.sdk.datatypes.FloorType;
import cc.robart.aicu.android.sdk.datatypes.Line;
import cc.robart.aicu.android.sdk.datatypes.RequestCallbackWithCommandID;
import cc.robart.aicu.android.sdk.exceptions.AuthenticationException;
import cc.robart.aicu.android.sdk.exceptions.ConnectionException;
import cc.robart.aicu.android.sdk.exceptions.RequestException;
import cc.robart.aicu.android.sdk.internal.data.FeatureMap;
import cc.robart.aicu.android.sdk.internal.data.Polygon;
import cc.robart.aicu.android.sdk.internal.data.RobotImpl;
import cc.robart.aicu.android.sdk.listeners.ConnectionListener;
import cc.robart.aicu.android.sdk.models.User;
import cc.robart.aicu.android.sdk.retrofit.client.IotManager;
import cc.robart.aicu.android.sdk.retrofit.client.IotManagerImpl;
import cc.robart.aicu.android.sdk.retrofit.request.IotLongTermSessionKeyRequest;
import cc.robart.aicu.android.sdk.retrofit.request.PairingInfoRequest;
import cc.robart.aicu.android.sdk.retrofit.response.PairingStatus;
import cc.robart.aicu.android.sdk.commands.maps.DeleteMapRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetAreasRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetCurrentMapIdRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetFeatureMapRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetMapsRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetNNPolygonsRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.GetTileMapRobotCommand;
import cc.robart.aicu.android.sdk.commands.maps.SaveMapRobotCommand;
import cc.robart.aicu.android.sdk.commands.LatestAvailableFirmwareServerCommand;
import cc.robart.aicu.android.sdk.configuration.ServerConfiguration;
import cc.robart.aicu.android.sdk.configuration.Strategy;
import cc.robart.aicu.android.sdk.datatypes.MapInfo;
import cc.robart.aicu.android.sdk.datatypes.NNMap;
import cc.robart.aicu.android.sdk.datatypes.Point2D;
import cc.robart.aicu.android.sdk.datatypes.RequestCallback;
import cc.robart.aicu.android.sdk.datatypes.RequestCallbackWithResult;
import cc.robart.aicu.android.sdk.datatypes.RobPose;
import cc.robart.aicu.android.sdk.datatypes.Robot;
import cc.robart.aicu.android.sdk.datatypes.RobotStatus;
import cc.robart.aicu.android.sdk.datatypes.Task;
import cc.robart.aicu.android.sdk.datatypes.TileMap;
import cc.robart.aicu.android.sdk.datatypes.WIFIConnectionStatus;
import cc.robart.aicu.android.sdk.datatypes.WIFIScanResult;
import cc.robart.aicu.android.sdk.datatypes.WIFIScanResults;
import cc.robart.aicu.android.sdk.datatypes.WIFIStatus;
import cc.robart.aicu.android.sdk.datatypes.Area;
import cc.robart.aicu.android.sdk.datatypes.AreaState;
import cc.robart.aicu.android.sdk.datatypes.AreaType;
import cc.robart.aicu.android.sdk.datatypes.Areas;
import cc.robart.aicu.android.sdk.datatypes.BatteryStatus.ChargingState;
import cc.robart.aicu.android.sdk.datatypes.CleaningGridMap;
import cc.robart.aicu.android.sdk.datatypes.RobotStatus.Mode;
import cc.robart.aicu.android.sdk.datatypes.RoomType;
import cc.robart.aicu.android.sdk.datatypes.Schedule;
import cc.robart.aicu.android.sdk.datatypes.ScheduledTask;
import cc.robart.aicu.android.sdk.datatypes.CleaningMode;
import cc.robart.aicu.android.sdk.datatypes.CleaningParameterSet;
import cc.robart.aicu.android.sdk.commands.AddScheduledTaskRobotCommand;
import cc.robart.aicu.android.sdk.commands.ModifyScheduledTaskRobotCommand;
import cc.robart.aicu.android.sdk.commands.CleanRobotCommand;
import cc.robart.aicu.android.sdk.commands.DeleteAreaRobotCommand;
import cc.robart.aicu.android.sdk.commands.DeleteScheduledTaskRobotCommand;
import cc.robart.aicu.android.sdk.commands.ExploreRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetAvailableWifiListFromRobot;
import cc.robart.aicu.android.sdk.commands.GetCleaningGridMapRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetCommandResultRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetEventLogRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetRobotIDRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetRobotNameRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetRobotPoseRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetRobotStatusRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetScheduleRobotCommand;
import cc.robart.aicu.android.sdk.commands.GetWifiStatusRobotCommand;
import cc.robart.aicu.android.sdk.commands.SetCleaningParameterSetRobotCommand;
import cc.robart.aicu.android.sdk.commands.ModifyAreaRobotCommand;
import cc.robart.aicu.android.sdk.commands.AddAreaRobotCommand;
import cc.robart.bluetooth.sdk.core.client.BluetoothClient;
import cc.robart.bluetooth.sdk.core.client.BluetoothClientImpl;
import cc.robart.bluetooth.sdk.core.scan.ScanResult;
import cc.robart.bluetooth.sdk.core.BluetoothDeviceState;
import cc.robart.bluetooth.sdk.core.response.BluetoothWIFIScanResultResponse;
import cc.robart.bluetooth.sdk.core.exceptions.RxExceptions;
import io.reactivex.annotations.NonNull;
//import io.reactivex.common.functions.Consumer;
import io.reactivex.functions.Consumer;
import android.os.Parcel;
import android.os.Parcelable;
import org.reactivestreams.Subscription;
import static java.util.Arrays.asList;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.Single;
import java.util.ArrayList;
import io.reactivex.observers.DisposableObserver;
import cc.robart.aicu.android.sdk.listeners.RobotDiscoveryListener;
import cc.robart.aicu.android.sdk.internal.discovery.RobotDiscoveryHandler;

class ConfigurationData {
	private String iotUserLogin;
	private String robotUid;
	private String robotPassword;
	private String serverEndpoint;
	private String robotEndpoint;
	private String stsk;
	private IotManager iotManager = null;
	private RobartIoTCordovaPlugin instance = null;
	private ServerConfiguration serverConfiguration = null;

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

	public ServerConfiguration getServerConfiguration() {
		return serverConfiguration;
	}

	public void setServerConfiguration(ServerConfiguration serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
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

	public void initServer(RobartIoTCordovaPlugin instance) {
		if (this.serverConfiguration != null) {
			return;
		}

		try {
			System.out.println("InitServer, serverConfiguration");
			RobartSDK.initialize(instance.cordova.getActivity().getApplicationContext());

			this.serverConfiguration = ServerConfiguration.builder().hostUrl(serverEndpoint).parts("iot")
					.port(443).strategy(Strategy.https).build();
			this.iotManager = new IotManagerImpl(this.serverConfiguration);
			this.instance = instance;
			AICUConnector.init(this.serverConfiguration);
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
			scanSubscription.add(getClient().startRobotDiscovery().observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::setItem, this::onScanFailure));
		} catch (Exception e) {
			// Ignore this error
		}
	}

	public void setItem(ScanResult scanResult) {
		String deviceName = scanResult.getScanRecord().getDeviceName();
		String macAddress = scanResult.getBleDevice().getMacAddress();

		if (deviceName != null && !deviceName.isEmpty() && deviceName.indexOf("robot-") > -1 && !this.foundRobot) {
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
		}
		return rxBleClient;
	}

	public void connectDevice(String macAddress, CallbackContext callbackContext) {
		RobartIoTCordovaPlugin plugin = this;
		System.out.println("ConnectDevice, macAddress: " + macAddress);

		getClient().addDisconnectCallback(() -> {
			System.out.println("ConnectDevice, BT disconnected");
		});

		getClient().connectToRobot(macAddress).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
			if (aBoolean == BluetoothDeviceState.STATE_HANDSHAKE_DONE) {
				getClient().getRobotId().subscribe(bluetoothGetRobotIdResponse -> {
					this.uid = bluetoothGetRobotIdResponse.getUniqueId();
					getClient().startWifiScan().subscribe(new Consumer<Boolean>() {
	                    @Override
	                    public void accept(Boolean aBoolean) throws Exception {
	                    		plugin.getWifiScanResult("", callbackContext);
	                    }
	                	}, new Consumer<Throwable>() {
	                    @Override
	                    public void accept(Throwable throwable) throws Exception {
	                    		System.out.println("ConnectDevice, Exception on StartWifiScan:" + throwable);
	                    		callbackContext.error(
	    							"{'response': 'error on connect device. please review the native console'}".replaceAll("'", "\""));
	                    }
	                	});
				}, throwable -> {
					System.out.println("ConnectDevice, Exception:" + throwable);
					callbackContext.error(
							"{'response': 'error on connect device. please review the native console'}".replaceAll("'", "\""));
				});

			} else {
				System.out.println(
						"ConnectDevice. There was an error, aBoolean is not BluetoothDeviceState.STATE_HANDSHAKE_DONE. The current value is: "
								+ aBoolean);
				callbackContext.error(
						"{'response': 'error on connect device. please review the native console'}".replaceAll("'", "\""));
			}
		}, throwable -> {
			if (throwable.toString().indexOf("BleAlreadyConnectedException") > -1) {
				System.out.println("ConnectDevice. The robot is already connected");
			} else {
				System.out.println("ConnectDevice. This is because the robot is restarting. Exception: " + throwable.toString());
			}
		});
	}

	public void getWifiScanResult(String message, CallbackContext callbackContext) {
		System.out.println("GetWifiScanResult, executing");
		RobartIoTCordovaPlugin plugin = this;

		Single.defer(() -> getClient().getWifiScanResult().doOnSuccess(bluetoothWIFIScanResults -> {
			System.out.println("GetWifiScanResult, getWifiScanResult() called" + bluetoothWIFIScanResults);
		}).doOnSuccess(bluetoothWIFIScanResults -> {
			if (bluetoothWIFIScanResults.isScanning()) {
				System.out.println("GetWifiScanResult, scanning..." + bluetoothWIFIScanResults.isScanning());
				throw new IllegalStateException("GetWifiScanResult, not final list");
			}
			System.out.println("GetWifiScanResult, scanning..." + bluetoothWIFIScanResults.isScanning());
		})).retry((integer, throwable) -> throwable instanceof IllegalStateException)
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

	private void errorHandler(String method, Exception ex, CallbackContext callbackContext) {
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

		message = message1.replaceAll("'", "\"").replaceAll(":\"\"", ":null");

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

		return false;
	}

	private void robotGetRobotId(String message, CallbackContext callbackContext) {
		System.out.println("RobotGetRobotId. Calling : " + message);

		if (this.configurationData == null) {
			this.configurationData = new ConfigurationData();
		}

		this.configurationData.initServer(this);

		AICUConnector.sendCommand(new GetRobotIDRobotCommand(new RequestCallbackWithResult<Robot>() {
			@Override
			public void onSuccess(final Robot result1) {
				AICUConnector.sendCommand(new GetRobotNameRobotCommand(new RequestCallbackWithResult<String>() {
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
	};

	private void connectToWifi(String message, CallbackContext callbackContext) {

		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map<String, String> jsonMap = parser.parseJson(message.replaceAll("'", "\"").replaceAll(":\"\"", ":null"));

		final String ssid = jsonMap.get("ssid").toString();
		String passphrase = jsonMap.get("passphrase").toString();
		System.out.println("ConnectToWifi, Connecting to Wifi: " + ssid);

		getClient().connectToWifi(ssid, passphrase).subscribe(value -> {
			ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
			es.schedule(new Runnable(){
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
		AICUConnector.sendCommand(new GetAvailableWifiListFromRobot(new RequestCallbackWithResult<WIFIScanResults>() {
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
		AICUConnector.sendCommand(new StartWifiScanRobotCommand(new RequestCallback() {
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

		AICUConnector.sendCommand(new GetRobotStatusRobotCommand(new RequestCallbackWithResult<RobotStatus>() {
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
			AICUConnector.connectToRobot(plugin.robot, new ConnectionListener() {
				@Override
				public void onConnect() {
					System.out.println("GetRobotID. Successful on the connection to the robot");
					try {
						System.out.println("GetRobotID. Sending GetRobotIDRobotCommand");

						AICUConnector.sendCommand(new GetRobotIDRobotCommand(new RequestCallbackWithResult<Robot>() {
							@Override
							public void onSuccess(final Robot result1) {
								AICUConnector.sendCommand(new GetRobotNameRobotCommand(new RequestCallbackWithResult<String>() {
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
				public void onConnectionError(final Exception exception) {
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
		AICUConnector.sendCommand(new GetMapsRobotCommand(new RequestCallbackWithResult<List<MapInfo>>() {
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
					AICUConnector.sendCommand(new CleanRobotCommand(task, new RequestCallbackWithCommandID() {
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
					AICUConnector.sendCommand(new CleanRobotCommand(task, new RequestCallbackWithCommandID() {
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
		AICUConnector.sendCommand(new SetRobotTimeRobotCommand(year, month, day, hour, min, new RequestCallback() {
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
			AICUConnector.connectToRobot(plugin.robot, new ConnectionListener() {
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
				public void onConnectionError(final Exception exception) {
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

	private Consumer<? super User> onRegisterDeviceSuccess(String message, CallbackContext callbackContext,
			ConfigurationData cData) {
		return new Consumer<User>() {
			@Override
			public void accept(@NonNull User user) throws Exception {
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
				AICUConnector.connectToRobot(currElem, new ConnectionListener() {
					@Override
					public void onConnect() {
						System.out.println("Pairing, Connected to the robot #2, FinalizePairing started");
						RobartSDK.initialize(cData.getRobartIoTCordovaPluginInstance().cordova.getActivity().getApplicationContext());

						if (plugin.firstTimeConfiguration) {
							getClient().finalizePairing().subscribe(aBoolean -> {
								System.out.println("Pairing, success on finalizing Pairing: " + aBoolean);
								cData.getRobartIoTCordovaPluginInstance().justPaired = true;
								cData.initServer(cData.getRobartIoTCordovaPluginInstance());
								AICUConnector.stopFindRobot();
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
					public void onConnectionError(final Exception exception) {
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
		String uniqueId = AICUConnector.getCurrentRobot().getUniqueId();
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
