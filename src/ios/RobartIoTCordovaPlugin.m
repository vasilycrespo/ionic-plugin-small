/********* RobartIoTCordovaPlugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
@import RobartSDK;
@import RobartBluetooth;

// INTERFACE OF THE BLUETOOTH FUNCTIONALITY
@interface RobartBluetoothCordova : NSObject

@end

@interface RobartBluetoothCordova () <RABBluetoothServiceDelegate>

@property (strong) RABBluetoothService *service;
@property (strong) NSMutableArray<RABBluetoothRobot *> *robots;
@property (strong) CDVInvokedUrlCommand *command;
@property (nonatomic, assign) NSInteger wifiStatusCounts;
@property (nonatomic, assign) NSInteger pairingInitializeCounts;
@property (nonatomic, assign) NSInteger pairingCheckCounts;

- (void)connectToWifi:(CDVInvokedUrlCommand*)command ssid:(NSString*)ssid passphrase:(NSString*)passphrase;

@end

// INTERFACE OF THE CORDOVA PLUGIN
@interface RobartIoTCordovaPlugin : CDVPlugin <RASDKRobotDiscoveryServiceDelegate, RASDKFirmwareServiceDelegate> {}

- (void)connectRobot:(CDVInvokedUrlCommand*)command;
- (void)reconnectRobot:(CDVInvokedUrlCommand*)command;
- (void)getRobotID:(CDVInvokedUrlCommand*)command;
- (void)getRobotStatus:(CDVInvokedUrlCommand*)command;
- (void)startRobot:(CDVInvokedUrlCommand*)command;
- (void)stopRobot:(CDVInvokedUrlCommand*)command;
- (void)sendHomeRobot:(CDVInvokedUrlCommand*)command;
- (void)btRobotDisconnect:(CDVInvokedUrlCommand*)command;
- (void)startRobotDiscovery:(CDVInvokedUrlCommand*)command;
- (void)connectToWifi:(CDVInvokedUrlCommand*)command;

- (void)scheduleList:(CDVInvokedUrlCommand*)command;
- (void)scheduleEdit:(CDVInvokedUrlCommand*)command;
- (void)scheduleDelete:(CDVInvokedUrlCommand*)command;
- (void)scheduleAdd:(CDVInvokedUrlCommand*)command;
- (void)mapGetRobotPosition:(CDVInvokedUrlCommand*)command;
- (void)mapGetMovableObjectsList:(CDVInvokedUrlCommand*)command;
- (void)mapGetRoomLayout:(CDVInvokedUrlCommand*)command;
- (void)mapGetCleaningGrid:(CDVInvokedUrlCommand*)command;
- (void)robotGetRobotId:(CDVInvokedUrlCommand*)command;
- (void)robotSetName:(CDVInvokedUrlCommand*)command;
- (void)robotGetEventList:(CDVInvokedUrlCommand*)command;
- (void)mapDelete:(CDVInvokedUrlCommand*)command;
- (void)mapGetMaps:(CDVInvokedUrlCommand*)command;
- (void)actionSetSuctionControl:(CDVInvokedUrlCommand*)command;
- (void)getCommandResult:(CDVInvokedUrlCommand*)command;
- (void)mapGetTiles:(CDVInvokedUrlCommand*)command;
- (void)saveMap:(CDVInvokedUrlCommand*)command;
- (void)mapGetAreas:(CDVInvokedUrlCommand*)command;
- (void)actionSetArea:(CDVInvokedUrlCommand*)command;
- (void)actionModifyArea:(CDVInvokedUrlCommand*)command;
- (void)mapDeleteArea:(CDVInvokedUrlCommand*)command;
- (void)getFirmware:(CDVInvokedUrlCommand*)command;
- (void)downloadFirmware:(CDVInvokedUrlCommand*)command;
- (void)installFirmware:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) RASDKIoTManagerService* iotManagerService;
@property(strong) RobartBluetoothCordova *rbc;
@property(strong) NSString *uid;
@property(strong) CDVInvokedUrlCommand *temporalCmd;
@property (nonatomic, assign) NSInteger pairingInitializeCounts;
@property (nonatomic, assign) NSInteger pairingCheckCounts;
@property (nonatomic, assign) NSInteger sdkInitCount;
@property (nonatomic, assign) NSInteger currentPermanent;
@property (nonatomic, strong) RASDKFirmwareService* firmwareService;
@property (nonatomic, strong) RASDKRobotDiscoveryService* robotDiscoveryService;

@end


// IMPLEMENTATION OF THE BLUETOOTH FUNCTIONALITY
@implementation RobartBluetoothCordova
static const NSInteger kMaxNumberOfSDKInits             = 10;



RobartIoTCordovaPlugin *plugin;

- (instancetype)initWithPlugin:(RobartIoTCordovaPlugin*) plugin1 command:(CDVInvokedUrlCommand*) command {
    self = [super init];
    
    if (self) {
        
        if (self.service) {
            NSLog(@"Disconnecting if previously attempted");
            [self.service disconnect];
        }
        
        NSLog(@"Initializing");
        
        self.service = [[RABBluetoothService alloc] init];
        self.service.delegate = self;
        
        plugin = plugin1;
        self.command = command;
        self.robots = [[NSMutableArray alloc] init];
        self.wifiStatusCounts = 0;
        self.pairingInitializeCounts = 0;
        plugin.pairingInitializeCounts = 0;
        NSLog(@"Starting robot discovery");
        [self.service startRobotDiscovery];
    }
    
    return self;
}
- (instancetype)disconnect:(RobartIoTCordovaPlugin*) plugin1 command:(CDVInvokedUrlCommand*) command {
    //self = [super init];
    if (self) {
        NSLog(@"Disconnecting");
        if (self.service) {
            [self.service disconnect];
            return self;
        }
        
        self.service = [[RABBluetoothService alloc] init];
        self.service.delegate = self;
        [self.service disconnect];
    }
    
    return self;
}

- (void)bluetoothService:(RABBluetoothService *)service bluetoothStateDidChange:(RABBluetoothStatus)status {
    NSLog(@"Bluetooth status did change to: %ld", (long)status);
}

- (void)bluetoothService:(RABBluetoothService *)service discoveryDidFoundRobot:(RABBluetoothRobot *)robot {
    NSLog(@"Found device with name %@", robot.name);
    
    if(robot.name != nil && [robot.name containsString:@"robot-"]) {
        NSLog(@"Found robot with name %@", robot.name);
        NSLog(@"Stopping robot discovery");
        [service stopRobotDiscovery];
        NSLog(@"Attempting to connect to robot. Please wait...");
        [service connectToRobot:robot];
    }
}

- (void)bluetoothService:(RABBluetoothService *)service discoveryDidFailWithError:(RABError *)error {
    NSLog(@"Discovery did fail with error %@", error);
    [self.service disconnect];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error on the discovery'}"];
    [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

- (void)bluetoothService:(RABBluetoothService *)service connectionToRobotDidFinish:(RABBluetoothRobot *)robot {
    NSLog(@"Did connect to robot with uuid: %@\n", robot.UUID);
    [service getRobotId:^(RABRobotInfo *robotInfo) {
        NSLog(@"Robot info:\n %@)", robotInfo);
        NSLog(@"This is the uid %@", robotInfo.uid);
        plugin.uid = robotInfo.uid;
        [self startWifiScan: service];
    } error:^(RABError *error) {
        NSLog(@"Error occured: %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on getRobotId'}"];
        
        [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    }];
}

- (void)bluetoothService:(RABBluetoothService *)service connectionToRobotDidFailWithError:(RABError *)error {
    NSLog(@"Connection to robot failed with error: %@", error);
    [self.service disconnect];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Connection to robot failed with error'}"];
    [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

- (void)bluetoothService:(RABBluetoothService *)service didDisconnectFromRobot:(RABBluetoothRobot *)robot {
    NSLog(@"BT: FinalizePairing completed (robot is restarting)");
}

- (void)startWifiScan:(RABBluetoothService *) service {
    [service startWifiScan:^() {
        NSLog(@"startWifiScan");
        
        [self searchWifi:service];
    } error:^(RABError * error) {
        NSLog(@"startWifiScan error: %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on startWifiScan'}"];
        
        [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    }];
}

- (void)searchWifi:(RABBluetoothService *) service {
    [service getAvailableWifiList:^(RABWifiScanResults * scanResults) {
        NSLog(@"getAvailableWifiList: %@", scanResults);
        if (scanResults.scanning) {
            [NSThread sleepForTimeInterval:2.0f];
            [self searchWifi:service];
        } else {
            NSLog(@"scanResults FINISHED: %@", scanResults);
            
            NSMutableString *response1 = [[NSMutableString alloc]init];
            NSMutableString *response = [[NSMutableString alloc]init];
            
            RABWifiScanResult *wifi;
            for (wifi in scanResults.results) {
                [response1 appendString:[NSString stringWithFormat:@"{''ssid'': ''%@'', ''rssi'': ''%ld'', ''pairwisecipher'': ''%@''}, ",wifi.ssid, (long)wifi.rssi, wifi.pairwiseCipher]];
            }
            
            if ([response1 length] > 0) {
                [response appendString:[NSString stringWithFormat:@"{''response'': [%@]}", [response1 substringToIndex:[response1 length] - 2]]];
            } else {
                [response appendString:[NSString stringWithFormat:@"{''response'': []}"]];
            }
            
            NSString *final = [response stringByReplacingOccurrencesOfString:@"''" withString:@"\""];
            
            NSLog(@"CONCATENATED WIFI LIST %@",final);
            
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
            
            [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
        }
    } error:^(RABError * error) {
        NSLog(@"getAvailableWifiList error: %@", error);
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on getAvailableWifiList'}"];
        
        [plugin.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    }];
}

- (void)connectToWifi:(CDVInvokedUrlCommand*)command ssid:(NSString*)ssid passphrase:(NSString*)passphrase {
    RABWifiConfigurationParameter *wifiParameter = [RABWifiConfigurationParameter new];
    wifiParameter.ssid = ssid;
    wifiParameter.passphrase = passphrase;
    
    NSLog(@"Before connect to Wifi %@", ssid);
    
    [self.service connectToWifi:wifiParameter completion:^() {
        [self scheduleWifiStatus:command wifi:ssid];
    } error:^(RABError * error) {
        NSLog(@"connectoToWifi error: %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on connectoToWifi'}"];
        
        [plugin.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)scheduleWifiStatus:(CDVInvokedUrlCommand*)command wifi:(NSString*)wifi {
    NSLog(@"%@", [NSString stringWithFormat:@"BT: GetWifiStatus....%zd", self.wifiStatusCounts]);
    
    [self.service getWifiStatus:^(RABWifiStatus * status) {
        NSLog(@"BT: GetWifiStatus completed. Connected to: %@", status.ssid);
        
        if(status.status == RABWifiConnectionStatusConnected && [status.ssid isEqualToString:wifi]){
            NSLog(@"BT: Connection to the WiFi is confirmed");
            NSLog(@"BT: GetRobotId started");
            
            [self.service getRobotId:^(RABRobotInfo * robotInfo) {
                plugin.uid = robotInfo.uid;
                NSLog(@"BT: GetRobotId completed");
                // [self startIoTPairing];
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{'response': 'Connected to the wifi'}"];
                
                [plugin.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                
            } error:^(RABError * error) {
                NSLog(@"%@",[NSString stringWithFormat:@"BT: GetRobotId error occured:\n%@", [error description]]);
            }];
        }
        else {
            if(self.wifiStatusCounts <= 10) {
                self.wifiStatusCounts++;
                [self scheduleWifiStatus:command wifi:wifi];
            } else {
                NSLog(@"%@", [NSString stringWithFormat:@"BT: Could not connect to the WiFi"]);
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on connectoToWifi'}"];
                
                [plugin.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
        }
    } error:^(RABError * error) {
        NSLog(@"%@", [NSString stringWithFormat:@"BT: GetWifiStatus error occured:\n%@", [error description]]);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error occured on connectoToWifi'}"];
        
        [plugin.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
    
}

@end



@implementation RobartIoTCordovaPlugin

- (void)connectRobot:(CDVInvokedUrlCommand*)command
{
    NSLog(@"METHOD connectRobot");
    
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"param %@", param);
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"jsonObject=%@", jsonObject);
    
    
    NSString *iotUserLogin = [jsonObject valueForKey:@"iotUserLogin"];
    NSString *robotUid = [jsonObject valueForKey:@"robotUid"];
    NSString *robotPassword = [jsonObject valueForKey:@"robotPassword"];
    NSString *serverEndpoint = [jsonObject valueForKey:@"serverEndpoint"];
    NSString *robotEndpoint = [jsonObject valueForKey:@"robotEndpoint"];
    
    NSString *stsk = [jsonObject valueForKey:@"stsk"];
    
    if([robotUid length]  <= 0)
    {
        NSLog(@"Getting the robotUid, %@", self.uid);
        
        robotUid = self.uid;
        
        NSLog(@"Getting the robotUid #2, %@", robotUid);
    }
    
    NSLog(@"iotUserLogin=%@", iotUserLogin);
    NSLog(@"robotUid=%@", robotUid);
    NSLog(@"robotPassword=%@", robotPassword);
    NSLog(@"serverEndpoint=%@", serverEndpoint);
    NSLog(@"robotEndpoint=%@", robotEndpoint);
    
    NSLog(@"stsk=%@", stsk);
    
    NSLog(@"All data %@ %@ %@ %@ %@ %@", iotUserLogin, robotUid, robotPassword, serverEndpoint, stsk, robotEndpoint);
    
    RASDKIoTCredentials* credentials = [RASDKIoTCredentials new];
    credentials.robotUid = robotUid;
    credentials.userLogin = iotUserLogin;
    credentials.iotEndpoint = [NSURL URLWithString:[serverEndpoint stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    
    RASDKConfiguration* configuration = [RASDKIoTConfiguration configurationWithCredentials:credentials];
    [[RASDKAicuConnecter sharedConnector] initializeWithConfiguration: configuration completion:^(RASDKRobotInfo * _Nonnull robotInfo) {
        NSLog(@"Connected to the robot. Response: %@", robotInfo);
        
        NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitYear fromDate:[NSDate date]];
        NSLog(@"Time being set");
        
        RASDKTimeParameter* currentTime = [RASDKTimeParameter new];
        currentTime.day = [components day];
        currentTime.hour = [components hour];
        currentTime.min = [components minute];
        currentTime.month = [components month];
        currentTime.year = [components year];
        
        NSLog(@"%@", currentTime);
        
        NSLog(@"All time data %ld %ld %ld %ld %ld", (long)currentTime.day, (long)currentTime.hour, (long)currentTime.min, (long)currentTime.month, (long)currentTime.year);
        
        [[RASDKAicuConnecter sharedConnector] setRobotTime:currentTime completion:^() {
            NSLog(@"success:  Time Set");
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"error setting time");
        }];
        
        NSLog(@"BT: FinalizePairing started");
        [self.rbc.service finalizePairing];
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{'response': 'Connected to the robot'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        RASDKUserCredentials* credentials1 = [RASDKUserCredentials new];
        credentials1.stsk = stsk;
        RASDKIoTManagerServiceCredentials *credentials2 = [RASDKIoTManagerServiceCredentials new];
        
        credentials2.userLogin = iotUserLogin;
        credentials2.iotEndpoint = [NSURL URLWithString:[serverEndpoint stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
        
        self.iotManagerService = [RASDKServices createIoTManagerServiceWithCredentials: credentials2];
        
        [self.iotManagerService registerDevice: credentials1 completion:^ {
            NSLog(@"The device was registered. Starting the pairing, it will require some seconds");
            [self schedulePairingInitialize:command];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"Invalid STSK");
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Invalid STSK'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

- (void)reconnectRobot:(CDVInvokedUrlCommand*)command
{
    NSLog(@"METHOD reconnectRobot");
    
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitYear fromDate:[NSDate date]];
    NSLog(@"Time being set");
    
    RASDKTimeParameter* currentTime = [RASDKTimeParameter new];
    currentTime.day = [components day];
    currentTime.hour = [components hour];
    currentTime.min = [components minute];
    currentTime.month = [components month];
    currentTime.year = [components year];
    
    NSLog(@"%@", currentTime);
    
    NSLog(@"All time data %ld %ld %ld %ld %ld", (long)currentTime.day, (long)currentTime.hour, (long)currentTime.min, (long)currentTime.month, (long)currentTime.year);
    
    [[RASDKAicuConnecter sharedConnector] setRobotTime:currentTime completion:^() {
        NSLog(@"success:  Time Set");
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error setting time");
    }];
    
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"param %@", param);
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"jsonObject=%@", jsonObject);
    
    
    NSString *iotUserLogin = [jsonObject valueForKey:@"iotUserLogin"];
    NSString *robotUid = [jsonObject valueForKey:@"robotUid"];
    NSString *robotPassword = [jsonObject valueForKey:@"robotPassword"];
    NSString *serverEndpoint = [jsonObject valueForKey:@"serverEndpoint"];
    NSString *robotEndpoint = [jsonObject valueForKey:@"robotEndpoint"];
    
    NSString *stsk = [jsonObject valueForKey:@"stsk"];
    
    if([robotUid length]  <= 0)
    {
        NSLog(@"Getting the robotUid, %@", self.uid);
        
        robotUid = self.uid;
        
        NSLog(@"Getting the robotUid #2, %@", robotUid);
    }
    
    NSLog(@"iotUserLogin=%@", iotUserLogin);
    NSLog(@"robotUid=%@", robotUid);
    NSLog(@"robotPassword=%@", robotPassword);
    NSLog(@"serverEndpoint=%@", serverEndpoint);
    NSLog(@"robotEndpoint=%@", robotEndpoint);
    
    NSLog(@"stsk=%@", stsk);
    
    NSLog(@"All data %@ %@ %@ %@ %@ %@", iotUserLogin, robotUid, robotPassword, serverEndpoint, stsk, robotEndpoint);
    
    RASDKIoTCredentials* credentials = [RASDKIoTCredentials new];
    credentials.robotUid = robotUid;
    credentials.userLogin = iotUserLogin;
    credentials.iotEndpoint = [NSURL URLWithString:[serverEndpoint stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    
    RASDKIoTManagerServiceCredentials* smCredentials = [RASDKIoTManagerServiceCredentials new];
    
    smCredentials.userLogin = iotUserLogin;
    smCredentials.iotEndpoint = [NSURL URLWithString:[serverEndpoint stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    
    RASDKIoTConfiguration* config = [RASDKIoTConfiguration configurationWithCredentials: credentials];
    
    NSLog(@"About to call user pairing for this device");
    self.iotManagerService = [RASDKServices createIoTManagerServiceWithCredentials: smCredentials];
    if (stsk) {
        RASDKUserCredentials* userCredentials  = [RASDKUserCredentials new];
        
        userCredentials.stsk = stsk;
        
        [self.iotManagerService registerDevice: userCredentials completion:^ {
            NSLog(@"Registration Complete");
            [self scheduleSDKInitWithConfig:config command:command];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"Registration Incomplete");
            [self scheduleSDKInitWithConfig:config command:command];
        }];
    } else {
        [self scheduleSDKInitWithConfig:config command:command];
    }
}

- (void)schedulePairingCheck:(CDVInvokedUrlCommand*)command {
    NSLog(@"%@", [NSString stringWithFormat:@"IoT: SchedulePairingCheck.... %zd", self.pairingCheckCounts]);
    RASDKPairedRobot* pairedRobot   = [RASDKPairedRobot new];
    pairedRobot.robotUid            = [self.uid copy];
    
    NSLog(@"schedulePairingCheck. Robot ID: %@", pairedRobot.robotUid);
    [self.iotManagerService getPairingStatus:pairedRobot completion:^(RASDKPairingStatus * _Nonnull pairingStatus) {
        NSLog(@"IoT: GetPairingStatus completed");
        
        switch (pairingStatus.currentStatus) {
            case RASDKPairingStatusValuePaired: {
                NSLog(@"IoT: Pairing status: RASDKPairingStatusValuePaired");
                
                break;
            }
            case RASDKPairingStatusValueExpired:{
                NSLog(@"IoT: Pairing status: RASDKPairingStatusValueExpired");
                break;
            }
            case RASDKPairingStatusValueWaitingForButtonConfirmation:{
                NSLog(@"IoT: Pairing status: RASDKPairingStatusValueWaitingForButtonConfirmation");
                break;
            }
            case RASDKPairingStatusValueUnknown:{
                NSLog(@"IoT: Pairing status: RASDKPairingStatusValueUnknown");
                break;
            }
                
            default:
                break;
        }
        
        NSLog(@"BT: FinalizePairing started");
        [self.rbc.service finalizePairing];
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{'response': 'Connected to the robot'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"%@", [NSString stringWithFormat:@"IoT: GetPairingStatus error occured:\n%@", [error description]]);
        NSLog(@"This error is because the robot is already paired, so, FinalizePairing is started");
        
        [self.rbc.service finalizePairing];
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{'response': 'Connected to the robot'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)schedulePairingInitialize:(CDVInvokedUrlCommand*)command {
    NSLog(@"%@", [NSString stringWithFormat:@"IoT: PairingInitialize.... %zd", self.pairingInitializeCounts]);
    RASDKPairingInfo* pairingInfo   = [RASDKPairingInfo new];
    pairingInfo.robotUid            = [self.uid copy];
    pairingInfo.robotPassword       = @"";
    
    NSLog(@"Initializing Pairing. Robot: %@", pairingInfo.robotUid);
    
    [self.iotManagerService pairingInitialize: pairingInfo completion:^(RASDKPairingInitializeStatus status) {
        NSLog(@"IoT: PairingInitialize completed");
        NSLog(@"IoT: GetPairingStatus started. Robot ID: %@", self.uid);
        self.pairingCheckCounts = 0;
        [self schedulePairingCheck:command];
    } error:^(RASDKError * _Nonnull error) {
        if(error.code == RASDKErrorCodeTimeout && self.pairingInitializeCounts < 1000) {
            self.pairingInitializeCounts++;
            [self schedulePairingInitialize:command];
        }
        else {
            NSLog(@"%@", [NSString stringWithFormat:@"IoT: PairingInitialize error occured:\n%@", [error description]]);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Error on pairing'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
    
}

- (void)getRobotID:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling getRobotID");
    
    if (self.uid) {
        NSString *response = [NSString stringWithFormat:@"{'name': '%@', 'uid': '%@', 'uidCamlas': '%@', 'model': '%@', 'firmware': '%@'}", self.uid, self.uid, @"", @"", @""];
        NSLog(@"success getRobotID %@, response: %@", self.uid, response);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:response];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
        [[RASDKAicuConnecter sharedConnector] getRobotID:^(RASDKRobotInfo * _Nonnull robotInfo) {
            [[RASDKAicuConnecter sharedConnector] getRobotName:^(RASDKRobotName * _Nonnull robotName) {
                NSString *response = [NSString stringWithFormat:@"{'name': '%@', 'uid': '%@', 'uidCamlas': '%@', 'model': '%@', 'firmware': '%@'}", robotName.name, robotInfo.uid, robotInfo.uidCamlas, robotInfo.model, robotInfo.firmware];
                NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
                
                NSLog(@"success getRobotID, response: %@", final);
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            } error:^(RASDKError * _Nonnull error) {
                NSLog(@"error getRobotID %@", error);
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"error getRobotID %@", error);
            
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }
}

- (void)getRobotStatus:(CDVInvokedUrlCommand*)command
{
    [[RASDKAicuConnecter sharedConnector] getRobotStatus:^(RASDKRobotStatus * _Nonnull robotInfo) {
        NSString *charging;
        NSString *mode;
        
        if (robotInfo.chargingState == RASDKChargeStateCharging) {
            charging = @"charging";
        } else if (robotInfo.chargingState == RASDKChargeStateUnconnect) {
            charging = @"unconnected";
        } else if (robotInfo.chargingState == RASDKChargeStateConnected) {
            charging = @"connected";
        } else {
            charging = @"unknown";
        }
        
        if (robotInfo.mode == RASDKRobotModeReady) {
            mode = @"ready";
        } else if (robotInfo.mode == RASDKRobotModeExploring) {
            mode = @"exploring";
        } else if (robotInfo.mode == RASDKRobotModeCleaning) {
            mode = @"cleaning";
        } else if (robotInfo.mode == RASDKRobotModeNotReady) {
            mode = @"not_ready";
        } else if (robotInfo.mode == RASDKRobotModeGoHome) {
            mode = @"go_home";
        } else if (robotInfo.mode == RASDKRobotModeLifted) {
            mode = @"lifted";
        } else if (robotInfo.mode == RASDKRobotModeTargetPoint) {
            mode = @"target_point";
        } else if (robotInfo.mode == RASDKRobotModeDirectControl) {
            mode = @"direct_control";
        } else if (robotInfo.mode == RASDKRobotModeRecovery) {
            mode = @"recovery";
        } else {
            mode = @"unknown";
        }
        
        NSString *response = [NSString stringWithFormat:@"{'voltage': '%f', 'cleaningParameter': '%d', 'batteryLevel': '%ld', 'chargingState': '%@', 'mode': '%@'}", robotInfo.voltage, robotInfo.cleaningParameter, (long)robotInfo.batteryLevel, charging, mode];
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success getRobotStatus %f, response: %@", robotInfo.voltage, final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error getRobotID %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
     ];
}

- (void)scheduleSDKInitWithConfig:(RASDKIoTConfiguration*)config command:(CDVInvokedUrlCommand*)command {
    [[RASDKAicuConnecter sharedConnector] initializeWithConfiguration:config completion:^(RASDKRobotInfo * _Nonnull info) {
        NSLog(@"SDK Initiated");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{'response': 'Connected to the robot'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        if(error.code  == RASDKErrorCodeTimeout && self.sdkInitCount < kMaxNumberOfSDKInits) {
            self.sdkInitCount++;
            [self scheduleSDKInitWithConfig: config command:command];
        }
        else {
            NSLog(@"SDK: initializeWithConfiguration error occured: \n%@", [error description]);
            //[self appendToConsoleOutput: [NSString stringWithFormat: @"SDK: initializeWithConfiguration error occured: \n%@", [error description]]];
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }
     ];
}

- (void)startRobot:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling startRobot. Permanent Map #1: %ld", (long)self.currentPermanent);
    
    [[RASDKAicuConnecter sharedConnector] getMaps:^(NSArray<RASDKMap*>* maps) {
        RASDKMap *map;
        
        NSLog(@"calling startRobot. Searching Maps");
        for (map in maps) {
            if (map.permanentFlag) {
                self.currentPermanent = map.mapId;
            }
        }
        
        NSLog(@"calling startRobot. Permanent Map #2: %ld", (long)self.currentPermanent);
        
        if  (self.currentPermanent) {
            RASDKCleanMapParameter *cleanMapParameter = [RASDKCleanMapParameter new];
            cleanMapParameter.mapId = self.currentPermanent;
            cleanMapParameter.cleaningParameterSet = RASDKCleaningParameterNormalMode;
            NSLog(@"calling cleanMap %@", cleanMapParameter);
            [[RASDKAicuConnecter sharedConnector] cleanMap:cleanMapParameter completion:^(RASDKCommand * _Nonnull robotInfo) {
                NSString *response = @"{'response': 'success'}";
                NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
                NSLog(@"success cleanMap");
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            } error:^(RASDKError * _Nonnull error) {
                NSLog(@"error cleanMap %@", error);
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        } else {
            NSLog(@"calling cleanStartOrContinue");
            [[RASDKAicuConnecter sharedConnector] cleanStartOrContinue:RASDKCleaningParameterNormalMode completion:^(RASDKCommand * _Nonnull robotInfo) {
                NSString *response = @"{'response': 'success'}";
                NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
                NSLog(@"success cleanStartOrContinue");
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            } error:^(RASDKError * _Nonnull error) {
                NSLog(@"error cleanStartOrContinue %@", error);
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        }
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapGetMaps %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)stopRobot:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling stopRobot");
    
    [[RASDKAicuConnecter sharedConnector] stop:^(RASDKCommand * _Nonnull robotInfo) {
        NSString *response = @"{'response': 'success'}";
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success stopRobot");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error stopRobot %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)sendHomeRobot:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling sendHomeRobot");
    
    [[RASDKAicuConnecter sharedConnector] goHome:^(RASDKCommand * _Nonnull robotInfo) {
        NSString *response = @"{'response': 'success'}";
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success sendHomeRobot");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error sendHomeRobot %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)startRobotDiscovery:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling startRobotDiscovery");
    if (self.rbc) {
        [self.rbc disconnect:self command:command];
    }
    
    self.rbc = [[RobartBluetoothCordova alloc] initWithPlugin:self command:command];
}

- (void)btRobotDisconnect:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling Bluetooth Disconnect");
    if (self.rbc) {
        [self.rbc disconnect:self command:command];
    }
    
    self.rbc = [[RobartBluetoothCordova alloc] disconnect:self command:command];
}

- (void)connectToWifi:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling connectToWifi");
    
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSString *ssid = [jsonObject valueForKey:@"ssid"];
    NSString *passphrase = [jsonObject valueForKey:@"passphrase"];
    
    NSLog(@"ssid=%@", ssid);
    
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitYear fromDate:[NSDate date]];
    NSLog(@"Time being set");
    
    RASDKTimeParameter* currentTime = [RASDKTimeParameter new];
    currentTime.day = [components day];
    currentTime.hour = [components hour];
    currentTime.min = [components minute];
    currentTime.month = [components month];
    currentTime.year = [components year];
    
    NSLog(@"%@", currentTime);
    
    NSLog(@"All time data %ld %ld %ld %ld %ld", (long)currentTime.day, (long)currentTime.hour, (long)currentTime.min, (long)currentTime.month, (long)currentTime.year);
    
    [[RASDKAicuConnecter sharedConnector] setRobotTime:currentTime completion:^() {
        NSLog(@"success:  Time Set");
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error setting time");
    }];
    
    [self.rbc connectToWifi:command ssid:ssid passphrase:passphrase];
}

- (void)scheduleList:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling scheduleList");
    
    [[RASDKAicuConnecter sharedConnector] getSchedule:^(RASDKSchedule * _Nonnull scheduleTasks) {
        NSLog(@"scheduleTasks: %@", scheduleTasks);
        
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSMutableString *response = [[NSMutableString alloc]init];
        
        RASDKScheduleTask *task;
        for (task in scheduleTasks.tasks) {
            NSNumber *day;
            NSMutableString *days = [[NSMutableString alloc]init];
            NSMutableString *days2 = [[NSMutableString alloc]init];
            
            for (day in task.date.daysOfWeek) {
                [days appendString:[NSString stringWithFormat:@"%@, ", day]];
            }
            
            if ([days length] > 0) {
                [days2 appendString:[NSString stringWithFormat:@"%@", [days substringToIndex:[days length] - 2]]];
            } else {
                [days2 appendString:@""];
            }
            
            [response1 appendString:[NSString stringWithFormat:@"{'task_id': %ld, 'enabled': %ld, 'time':{'days_of_week': [%@],'hour': %ld,'min': %ld,'sec':0}, 'task':{'map_id': %ld,'cleaning_parameter_set': 1,'cleaning_mode': 1 }}, ",(long)task.uid, (long)task.enabled, days2, (long)task.date.hour, (long)task.date.min, (long)task.mapId]];
        }
        
        if ([response1 length] > 0) {
            [response appendString:[NSString stringWithFormat:@"{'schedule': [%@]}", [response1 substringToIndex:[response1 length] - 2]]];
        } else {
            [response appendString:[NSString stringWithFormat:@"{'schedule': []}"]];
        }
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success scheduleList response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error scheduleList %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)scheduleEdit:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling scheduleEdit");
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    [[RASDKAicuConnecter sharedConnector] getMaps:^(NSArray<RASDKMap*>* maps) {
        RASDKMap *map;
        
        for (map in maps) {
            if (map.permanentFlag) {
                self.currentPermanent = map.mapId;
            }
        }
        
        NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
        
        if  (self.currentPermanent) {
            mapId = self.currentPermanent;
        }
        
        RASDKScheduleTaskParameter* task = [RASDKScheduleTaskParameter new];
        task.uid = [[jsonObject valueForKey:@"task_id"] integerValue];
        task.mapId = mapId;
        
        task.hour = [[jsonObject valueForKey:@"hour"] integerValue];
        task.min = [[jsonObject valueForKey:@"min"] integerValue];
        task.cleaningMode = RASDKCleaningModeCleanArea;
        task.cleaningParameter = RASDKCleaningParameterDefaultMode;
        task.enabled = 1;
        
        NSString *daysOfWeek = [jsonObject valueForKey:@"days_of_week"];
        
        NSLog(@"Checking daysOfWeek:  %@",daysOfWeek);
        
        NSMutableArray *myIntegers = [NSMutableArray array];
        if ([daysOfWeek rangeOfString:@"1"].location != NSNotFound) { NSLog(@"Number 1"); [myIntegers addObject:[NSNumber numberWithInteger:1]]; }
        if ([daysOfWeek rangeOfString:@"2"].location != NSNotFound) { NSLog(@"Number 2"); [myIntegers addObject:[NSNumber numberWithInteger:2]]; }
        if ([daysOfWeek rangeOfString:@"3"].location != NSNotFound) { NSLog(@"Number 3"); [myIntegers addObject:[NSNumber numberWithInteger:3]]; }
        if ([daysOfWeek rangeOfString:@"4"].location != NSNotFound) { NSLog(@"Number 4"); [myIntegers addObject:[NSNumber numberWithInteger:4]]; }
        if ([daysOfWeek rangeOfString:@"5"].location != NSNotFound) { NSLog(@"Number 5"); [myIntegers addObject:[NSNumber numberWithInteger:5]]; }
        if ([daysOfWeek rangeOfString:@"6"].location != NSNotFound) { NSLog(@"Number 6"); [myIntegers addObject:[NSNumber numberWithInteger:6]]; }
        if ([daysOfWeek rangeOfString:@"7"].location != NSNotFound) { NSLog(@"Number 7"); [myIntegers addObject:[NSNumber numberWithInteger:7]]; }
        
        task.daysOfWeek = myIntegers;
        
        NSLog(@"Task prepared: %@", task);
        [[RASDKAicuConnecter sharedConnector] modifyScheduledTask:task completion:^(RASDKCommand * _Nonnull sdkCommand) {
            NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
            NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
            
            NSLog(@"success scheduleEdit response: %@", final);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"error scheduleEdit %@", error);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error scheduleEdit %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)scheduleDelete:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling scheduleDelete");
    
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"Param: %@", param);
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"Json Object: %@", jsonObject);
    
    NSInteger taskId = [[jsonObject valueForKey:@"task_id"] intValue];
    
    NSLog(@"Task prepared: %ld", (long)taskId);
    
    [[RASDKAicuConnecter sharedConnector] deleteScheduledTaskWithId:taskId completion:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success scheduleEdit response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error scheduleDelete %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)scheduleAdd:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling scheduleAdd");
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSLog(@"Json Object: %@", jsonObject);
    
    [[RASDKAicuConnecter sharedConnector] getMaps:^(NSArray<RASDKMap*>* maps) {
        RASDKMap *map;
        
        for (map in maps) {
            if (map.permanentFlag) {
                self.currentPermanent = map.mapId;
            }
        }
        
        NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
        
        if  (self.currentPermanent) {
            mapId = self.currentPermanent;
        }
        
        RASDKScheduleTaskParameter* task = [RASDKScheduleTaskParameter new];
        
        task.mapId = mapId;
        task.hour = [[jsonObject valueForKey:@"hour"] integerValue];
        task.min = [[jsonObject valueForKey:@"min"] integerValue];
        task.cleaningMode = RASDKCleaningModeCleanArea;
        task.cleaningParameter = RASDKCleaningParameterDefaultMode;
        task.enabled = 1;
        
        NSString *daysOfWeek = [jsonObject valueForKey:@"days_of_week"];
        
        NSLog(@"Checking daysOfWeek:  %@",daysOfWeek);
        
        NSMutableArray *myIntegers = [NSMutableArray array];
        if ([daysOfWeek rangeOfString:@"1"].location != NSNotFound) { NSLog(@"Number 1"); [myIntegers addObject:[NSNumber numberWithInteger:1]]; }
        if ([daysOfWeek rangeOfString:@"2"].location != NSNotFound) { NSLog(@"Number 2"); [myIntegers addObject:[NSNumber numberWithInteger:2]]; }
        if ([daysOfWeek rangeOfString:@"3"].location != NSNotFound) { NSLog(@"Number 3"); [myIntegers addObject:[NSNumber numberWithInteger:3]]; }
        if ([daysOfWeek rangeOfString:@"4"].location != NSNotFound) { NSLog(@"Number 4"); [myIntegers addObject:[NSNumber numberWithInteger:4]]; }
        if ([daysOfWeek rangeOfString:@"5"].location != NSNotFound) { NSLog(@"Number 5"); [myIntegers addObject:[NSNumber numberWithInteger:5]]; }
        if ([daysOfWeek rangeOfString:@"6"].location != NSNotFound) { NSLog(@"Number 6"); [myIntegers addObject:[NSNumber numberWithInteger:6]]; }
        if ([daysOfWeek rangeOfString:@"7"].location != NSNotFound) { NSLog(@"Number 7"); [myIntegers addObject:[NSNumber numberWithInteger:7]]; }
        
        task.daysOfWeek = myIntegers;
        
        NSLog(@"Task prepared: %@", task);
        
        [[RASDKAicuConnecter sharedConnector] addScheduledTask:task completion:^(RASDKCommand * _Nonnull sdkCommand) {
            NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
            NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
            
            NSLog(@"success scheduleAdd response: %@", final);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"error scheduleAdd %@", error);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error scheduleAdd %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetRobotPosition:(CDVInvokedUrlCommand*)command
{
    [[RASDKAicuConnecter sharedConnector] getRobotPose:^(RASDKRobotPose * _Nonnull robotPose) {
        NSString *response = [NSString stringWithFormat:
                              @"{'map_id': %ld, 'x1': %f, 'y1': %f, 'heading': %f, 'valid': true, 'timestamp': %ld}",
                              (long)robotPose.mapId, robotPose.position.x, robotPose.position.y, (float)robotPose.heading, (long)robotPose.timestamp];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"mapGetRobotPosition error mapGetRobotPosition %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetMovableObjectsList:(CDVInvokedUrlCommand*)command
{
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSInteger mapId;
    mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    [[RASDKAicuConnecter sharedConnector] getNNMapWithId:mapId completion:^(RASDKNNMap* map) {
        
        RASDKPolygon *poly;
        RASDKSegment *segment;
        NSMutableString *lines = [[NSMutableString alloc]init];
        
        for (poly in map.polygons) {
            NSMutableString *polyLines = [[NSMutableString alloc]init];
            
            for (segment in poly.segments) {
                NSString *segmentResults = [NSString stringWithFormat:@"{'x1': %f, 'y1': %f, 'x2': %f, 'y2': %f}", segment.startPoint.x, segment.startPoint.y, segment.endPoint.x, segment.endPoint.y];
                [polyLines appendString:[NSString stringWithFormat:@"%@, ", segmentResults]];
            }
            
            if ([polyLines length] > 0) {
                [lines appendString:[NSString stringWithFormat:@"{'segments':[%@]}, ", [polyLines substringToIndex:[polyLines length] - 2]]];
            } else {
                [lines appendString:[NSString stringWithFormat:@"{'segments':[]}, "]];
            }
        }
        
        NSMutableString *polygons = [[NSMutableString alloc]init];
        if ([lines length] > 0) {
            [polygons appendString:[NSString stringWithFormat:@"[%@]", [lines substringToIndex:[lines length] - 2]]];
        } else {
            [polygons appendString:@"[]"];
        }
        
        NSString *response =
        [NSString stringWithFormat:@"{'map': {'map_id': %ld, 'polygons': %@}}", (long) map.mapId, polygons];
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"mapGetMovableObjectsList error mapGetMovableObjectsList %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetRoomLayout:(CDVInvokedUrlCommand*)command
{
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    [[RASDKAicuConnecter sharedConnector] getFeatureMapWithId:mapId completion:^(RASDKBaseMap* baseMap) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSMutableString *lines = [[NSMutableString alloc]init];
        
        RASDKSegment *poly;
        for (poly in baseMap.obstacles) {
            [response1 appendString:[NSString stringWithFormat:@"{'x1': %f, 'y1': %f, 'x2': %f, 'y2': %f}, ", poly.startPoint.x, poly.startPoint.y, poly.endPoint.x, poly.endPoint.y]];
        }
        
        if ([response1 length] > 0) {
            [lines appendString:[NSString stringWithFormat:@"[%@]", [response1 substringToIndex:[response1 length] - 2]]];
        } else {
            [lines appendString:[NSString stringWithFormat:@"[]"]];
        }
        
        NSString *valid = @"false";
        
        if (baseMap.dockingPose.valid) {
            valid = @"true";
        }
        
        NSString *response = [NSString stringWithFormat:@"{'map': {'map_id':%ld, 'lines': %@, 'docking_pose': {'x': %f, 'y': %f, 'heading': %f, 'valid': %@}}}", (long)mapId, lines, baseMap.dockingPose.postion.x, baseMap.dockingPose.postion.y, baseMap.dockingPose.heading, valid];
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetCleaningGrid:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling mapGetCleaningGrid");
    
    [[RASDKAicuConnecter sharedConnector] getCleaningGridMap:^(RASDKCleaningGridMap * cleaningGrid) {
        NSLog(@"cleaningGrid.gridRows: %lu, cleaningGrid.gridColumns: %lu", (unsigned long)cleaningGrid.gridRows, (unsigned long)cleaningGrid.gridColumns);
        
        NSMutableString *cleanedArea = [NSMutableString stringWithFormat:@""];
        for (int row = 0; row < cleaningGrid.gridRows; row++) {
            for (int column = 0; column < cleaningGrid.gridColumns; column++) {
                if ([cleaningGrid isGridCellCleanedAtRow:row column:column]) {
                    [cleanedArea appendString:@"1"];
                } else {
                    [cleanedArea appendString:@"0"];
                }
            }
        }
        NSLog(@"cleanedArea: %@", cleanedArea);
        NSString *encodedCleanedArea = [self encodeCleaningGrid: cleanedArea];
        NSLog(@"EncodedCleanedArea: %@", encodedCleanedArea);
        
        NSString *response = [NSString stringWithFormat:@"{'map_id': %ld,'lower_left_x':%f,'lower_left_y':%f,'size_x':%f,'size_y':%f,'resolution':%f,'cleaned': %@}", (long)cleaningGrid.mapId, cleaningGrid.bounds.origin.x, cleaningGrid.bounds.origin.y, cleaningGrid.bounds.size.height / 10, cleaningGrid.bounds.size.width / 10, cleaningGrid.gridCellSize, encodedCleanedArea];
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success mapGetCleaningGrid response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapGetCleaningGrid %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (NSString *)encodeCleaningGrid:(NSString *)text {
    NSMutableString *result = [[NSMutableString alloc] init];
    __block NSString *previousCharacter;
    __block int count = 0;
    
    if (text == Nil || text.length == 0) {
        return @"[]";
    }
    
    NSRange textRange = NSMakeRange(0, text.length);
    // Enumeration by composed character sequences is required to handle Emoji
    [text enumerateSubstringsInRange:textRange
                             options:NSStringEnumerationByComposedCharacterSequences
                          usingBlock:^(NSString *substring,
                                       NSRange substringRange,
                                       NSRange enclosingRange,
                                       BOOL *stop)
     {
         if (!previousCharacter) {
             previousCharacter = substring;
         }
         
         if ([substring isEqualToString:previousCharacter]) {
             count++;
         }
         
         if (![substring isEqualToString:previousCharacter]) {
             [result appendFormat:@"%d,", count];
             
             previousCharacter = substring;
             count = 1;
         }
         
         BOOL isLastCharacter = (substringRange.location + substringRange.length) == text.length;
         
         if (isLastCharacter) {
             [result appendFormat:@"%d", count];
         }
     }];
    
    if ([[text substringToIndex:1]  isEqual: @"1"]) {
        return [NSString stringWithFormat:@"[0,%@]", result];
    } else {
        return [NSString stringWithFormat:@"[1, %@]", result];
    }
}


- (void)robotGetRobotId:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling robotGetRobotId");
    
    [[RASDKAicuConnecter sharedConnector] getRobotID:^(RASDKRobotInfo * _Nonnull robotInfo) {
        [[RASDKAicuConnecter sharedConnector] getRobotName:^(RASDKRobotName * _Nonnull robotName) {
            NSString *response = [NSString stringWithFormat:@"{'name': '%@', 'unique_id': '%@', 'camlas_unique_id': '%@', 'model': '%@', 'firmware': '%@'}", robotName.name, robotInfo.uid, robotInfo.uidCamlas, robotInfo.model, robotInfo.firmware ];
            NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
            
            NSLog(@"success robotGetRobotId response: %@", final);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } error:^(RASDKError * _Nonnull error) {
            NSLog(@"error robotGetRobotId %@", error);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error robotGetRobotId %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)robotSetName:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling robotSetName");
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"Param: %@", param);
    // * {name: "Test"}
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"JsonObject: %@", jsonObject);
    
    
    NSString *name = [jsonObject valueForKey:@"name"];
    NSLog(@"name: %@", name);
    
    RASDKRobotNameParameter* parameter = [RASDKRobotNameParameter new];
    parameter.name = name;
    
    [[RASDKAicuConnecter sharedConnector] setRobotName:parameter completion:^() {
        NSString *response = @"{}";
        NSLog(@"success robotSetName response: %@", response);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:response];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error robotSetName %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)robotGetEventList:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling robotGetEventList");
    RASDKEventParameter *parameter = [RASDKEventParameter new];
    parameter.lastId = 0;
    
    [[RASDKAicuConnecter sharedConnector] getEventLog:parameter completion:^(RASDKEventList *eventList) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSMutableString *response = [[NSMutableString alloc]init];
        
        RASDKEvent *event;
        for (event in eventList.events) {
            [response1 appendString:[NSString stringWithFormat:@"{'id':'%ld','type':'%@','type_id': '%d','timestamp':{'year':'%ld','month':'%ld','day':'%ld','hour':'%ld','min':'%ld','sec':'%ld'}, 'map_id':'%ld','area_id':'%ld','source_type':'%u','source_id':'%ld','hierarchy':'%ld'}, ",
                                     (long)event.uid,  event.type, event.typeId, (long)event.timestamp.year, (long)event.timestamp.month,
                                     (long)event.timestamp.day, (long)event.timestamp.hour, (long)event.timestamp.min, (long)event.timestamp.sec, (long)event.mapId, (long)event.areaId, event.sourceType, (long)event.sourceId, (long)event.hierarchy]];
        }
        
        if ([response1 length] > 0) {
            [response appendString:[NSString stringWithFormat:@"{'robot_events': [%@]}", [response1 substringToIndex:[response1 length] - 2]]];
        } else {
            [response appendString:[NSString stringWithFormat:@"{'robot_events': []}"]];
        }
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success robotGetEventList response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error robotGetEventList %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapDelete:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling mapDelete");
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"Param: %@", param);
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"JsonObject: %@", jsonObject);
    
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    NSLog(@"MapId: %ld", (long)mapId);
    
    if (mapId == self.currentPermanent) {
        self.currentPermanent = 0;
    }
    
    [[RASDKAicuConnecter sharedConnector] deleteMap:mapId completion:^(RASDKCommand* sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success mapDelete response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapDelete %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetMaps:(CDVInvokedUrlCommand*)command
{
    NSLog(@"calling mapGetMaps");
    
    [[RASDKAicuConnecter sharedConnector] getMaps:^(NSArray<RASDKMap*>* maps) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSString *response;
        
        RASDKMap *map;
        int index = 0;
        
        for (map in maps) {
            index = index + 1;
            NSInteger permanent = 0;
            
            if (map.permanentFlag) {
                permanent = 1;
                self.currentPermanent = map.mapId;
            }
            
            [response1 appendString:[NSString stringWithFormat:@"{'map_id': %ld, 'permanent': %ld}, ", (long)map.mapId, (long)permanent]];
        }
        
        if ([response1 length] > 0) {
            response = [NSString stringWithFormat:@"{'maps': [%@]}", [response1 substringToIndex:[response1 length] - 2]];
        } else {
            response = [NSString stringWithFormat:@"{'maps': []}"];
        }
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success mapGetMaps response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapGetMaps %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)actionSetSuctionControl:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling actionSetSuctionControl");
    NSString* param = [command.arguments objectAtIndex:0];
    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSInteger cleaningParameterSet = [[jsonObject objectForKey:@"cleaning_parameter_set"]intValue];
    
    RASDKCleaningParameter p;
    
    if (cleaningParameterSet == 2) {
        p = RASDKCleaningParameterSilentMode; NSLog(@"%d", p);
    } else if (cleaningParameterSet == 1) {
        p = RASDKCleaningParameterNormalMode; NSLog(@"%d", p);
    } else if (cleaningParameterSet == 3) {
        p = RASDKCleaningParameterIntensiveMode;
    } else {
        p = RASDKCleaningParameterNormalMode; NSLog(@"%d", p);
    }
    
    NSLog(@"ActionSetSuctionControl Parameter: %d", p);
    
    [[RASDKAicuConnecter sharedConnector] setCleaningParameterSet:p completion:^() {
        NSString *response = [NSString stringWithFormat:@"{}"];
        NSLog(@"success actionSetSuctionControl response: %@", response);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:response];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error actionSetSuctionControl %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetAreas:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling mapGetAreas");
    NSString* param = [command.arguments objectAtIndex:0];
    NSLog(@"Param: %@", param);
    
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"JsonObject: %@", jsonObject);
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    NSLog(@"MapId: %ld", (long)mapId);
    
    if (self.currentPermanent) {
        mapId = self.currentPermanent;
    }
    
    [[RASDKAicuConnecter sharedConnector] getAreasForMap:mapId completion:^(RASDKMapAreas* data) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSString *response;
        
        RASDKMapArea *map;
        NSValue *pointValue;
        
        for (map in data.areas) {
            NSLog(@"%@", map);
            NSString *type;
            if (map.type == RASDKMapAreaTypeToBeCleaned) {
                type = @"to_be_cleaned";
            } else {
                type = @"room";
            }
            
            NSMutableString *points = [[NSMutableString alloc]init];
            
            for (pointValue in map.points) {
                CGPoint point = [pointValue CGPointValue];
                [points appendString:[NSString stringWithFormat:@"{'x': %f, 'y': %f}, ", point.x, point.y]];
            }
            if ([points length] > 0) {
                [response1 appendString:[NSString stringWithFormat:@"{'id': %ld, 'cleaning_parameter_set': 1, 'area_meta_data': '%@', 'area_type': '%@', 'points': [%@]}, ", (long)map.areaId, map.metaData, type, [points substringToIndex:[points length] - 2]]];
            } else {
                [response1 appendString:[NSString stringWithFormat:@"{'id': %ld, 'cleaning_parameter_set': 1, 'area_meta_data': '%@', 'area_type': '%@', 'points': []}, ", (long)map.areaId, map.metaData, type]];
            }
        }
        
        if ([response1 length] > 0) {
            response = [NSString stringWithFormat:@"{'map_id': %ld, 'areas': [%@]}", (long)data.mapId, [response1 substringToIndex:[response1 length] - 2]];
        } else {
            response = [NSString stringWithFormat:@"{'map_id': %ld, 'areas': []}", (long)data.mapId];
        }
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        
        NSLog(@"success mapGetAreas response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapGetAreas %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)actionSetExplore:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling actionSetExplore");
    
    [[RASDKAicuConnecter sharedConnector] explore:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success actionSetExplore response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error actionSetExplore %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)actionSetArea:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling actionSetArea");
    NSString* param = [command.arguments objectAtIndex:0];
    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSLog(@"Calling actionSetArea. Json Object: %@", jsonObject);
    RASDKMapAreaParameter *areaParameter = [RASDKMapAreaParameter new];
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    if  (self.currentPermanent) {
        mapId = self.currentPermanent;
    }
    
    areaParameter.mapId = mapId;
    
    NSString *areaType = [jsonObject objectForKey:@"area_type"];
    
    if ([areaType isEqual: @"to_be_cleaned"]) {
        areaParameter.type = RASDKMapAreaTypeToBeCleaned;
        areaParameter.state = RASDKMapAreaStateUnknown;
    } else {
        NSLog(@"Setting a restricted area");
        areaParameter.type = RASDKMapAreaTypeRoom;
        areaParameter.state = RASDKMapAreaStateBlocking;
    }
    
    NSString *areaMetaData = [jsonObject objectForKey:@"area_meta_data"];
    areaParameter.metaData = areaMetaData;
    areaParameter.cleaningParameter = RASDKCleaningParameterDefaultMode;
    areaParameter.floorType = RASDKMapAreaFloorTypeNone;
    areaParameter.roomType = RASDKMapAreaRoomTypeNone;
    
    NSMutableArray *points = [NSMutableArray array];
    
    NSNumber *x;
    NSNumber *y;
    
    int counter = 1;
    
    do
    {
        x = [jsonObject objectForKey:[NSString stringWithFormat:@"x%d", counter]];
        y = [jsonObject objectForKey:[NSString stringWithFormat:@"y%d", counter]];
        
        if (x != nil && y != nil) {
            NSLog(@" Preparing point (%@, %@)", x, y);
            CGPoint point = CGPointMake([x doubleValue], [y doubleValue]);
            [points addObject:[NSValue valueWithCGPoint:point]];
        }
        
        counter = counter + 1;
    } while( x != nil );
    
    areaParameter.points = points;
    
    [[RASDKAicuConnecter sharedConnector] addArea:areaParameter completion:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success actionSetArea response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error actionSetArea %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)actionModifyArea:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling actionModifyArea");
    NSString* param = [command.arguments objectAtIndex:0];
    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSLog(@"Json Object: %@", jsonObject);
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    if  (self.currentPermanent) {
        mapId = self.currentPermanent;
    }
    
    RASDKMapAreaParameter *areaParameter = [RASDKMapAreaParameter new];
    areaParameter.mapId = mapId;
    areaParameter.areaId = [[jsonObject objectForKey:@"area_id"] integerValue];
    
    NSString *areaType = [jsonObject objectForKey:@"area_type"];
    
    if ([areaType  isEqual: @"to_be_cleaned"]) {
        areaParameter.type = RASDKMapAreaTypeToBeCleaned;
        areaParameter.state = RASDKMapAreaStateUnknown;
    } else {
        areaParameter.type = RASDKMapAreaTypeRoom;
        areaParameter.state = RASDKMapAreaStateBlocking;
    }
    
    NSString *areaMetaData = [jsonObject objectForKey:@"area_meta_data"];
    areaParameter.metaData = areaMetaData;
    
    areaParameter.cleaningParameter = RASDKCleaningParameterDefaultMode;
    areaParameter.floorType = RASDKMapAreaFloorTypeNone;
    areaParameter.roomType = RASDKMapAreaRoomTypeNone;
    
    NSMutableArray *points = [NSMutableArray array];
    
    NSNumber *x;
    NSNumber *y;
    
    int counter = 1;
    
    do
    {
        x = [jsonObject objectForKey:[NSString stringWithFormat:@"x%d", counter]];
        y = [jsonObject objectForKey:[NSString stringWithFormat:@"y%d", counter]];
        
        if (x != nil && y != nil) {
            NSLog(@" Preparing point (%@, %@)", x, y);
            CGPoint point = CGPointMake([x doubleValue], [y doubleValue]);
            [points addObject:[NSValue valueWithCGPoint:point]];
        }
        
        counter = counter + 1;
    }while( x != nil );
    
    areaParameter.points = points;
    
    NSLog(@"RASDKMapAreaParameter object prepared %@", areaParameter.debugDescription);
    
    [[RASDKAicuConnecter sharedConnector] modifyArea:areaParameter completion:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success actionModifyArea response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error actionModifyArea %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapDeleteArea:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling mapDeleteArea");
    NSString* param = [command.arguments objectAtIndex:0];
    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSLog(@"Json Object: %@", jsonObject);
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    if  (self.currentPermanent) {
        mapId = self.currentPermanent;
    }
    
    RASDKDeleteAreaParameter *deleteAreaParameter = [RASDKDeleteAreaParameter new];
    
    deleteAreaParameter.mapId = mapId;
    deleteAreaParameter.areaId = [[jsonObject objectForKey:@"area_id"] integerValue];
    
    NSLog(@"RASDKDeleteAreaParameter object prepared %@", deleteAreaParameter);
    
    [[RASDKAicuConnecter sharedConnector] deleteArea:deleteAreaParameter completion:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success mapDeleteArea response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error mapDeleteArea %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getCommandResult:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling getCommandResult");
    
    [[RASDKAicuConnecter sharedConnector] getCommandResult:^(RASDKCommandResult * _Nonnull results) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSMutableString *lines = [[NSMutableString alloc]init];
        
        RASDKCommandInfo *cmd;
        
        for (cmd in results.commands) {
            NSString *status;
            if (cmd.status == RASDKCommandStatusUnknown) {
                status = @"unknown";
            } else if (cmd.status == RASDKCommandStatusQueued){
                status = @"queued";
            } else if (cmd.status == RASDKCommandStatusSkipped){
                status = @"skipped";
            } else if (cmd.status == RASDKCommandStatusExecuting){
                status = @"executing";
            } else if (cmd.status == RASDKCommandStatusDone){
                status = @"done";
            } else if (cmd.status == RASDKCommandStatusError){
                status = @"error";
            } else if (cmd.status == RASDKCommandStatusInterrupted){
                status = @"interrupted";
            } else if (cmd.status == RASDKCommandStatusAborted){
                status = @"aborted";
            }
            
            NSLog(@"Command ID %ld has this status: %@", (long) cmd.commandId, status);
            
            [response1 appendString:[NSString stringWithFormat:@"{'cmd_id': %ld, 'status': '%@'}, ", (long)cmd.commandId, status]];
        }
        
        if ([response1 length] > 0) {
            [lines appendString:[NSString stringWithFormat:@"[%@]", [response1 substringToIndex:[response1 length] - 2]]];
        } else {
            [lines appendString:[NSString stringWithFormat:@"[]"]];
        }
        
        NSString *final = [lines stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success getCommandResult response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error getCommandResult: %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)mapGetTiles:(CDVInvokedUrlCommand*)command
{
    NSString* param = [command.arguments objectAtIndex:0];
    NSArray *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    NSLog(@"mapGetTiles %ld", (long)mapId);
    
    [[RASDKAicuConnecter sharedConnector] getTileMapWithId:mapId completion:^(RASDKTileMap* baseMap) {
        NSMutableString *response1 = [[NSMutableString alloc]init];
        NSMutableString *lines = [[NSMutableString alloc]init];
        
        NSLog(@"%@", baseMap);
        RASDKSegment *poly;
        for (poly in baseMap.lines) {
            [response1 appendString:[NSString stringWithFormat:@"{'x1': %f, 'y1': %f, 'x2': %f, 'y2': %f}, ", poly.startPoint.x, poly.startPoint.y, poly.endPoint.x, poly.endPoint.y]];
        }
        
        if ([response1 length] > 0) {
            [lines appendString:[NSString stringWithFormat:@"[%@]", [response1 substringToIndex:[response1 length] - 2]]];
        } else {
            [lines appendString:[NSString stringWithFormat:@"[]"]];
        }
        
        NSString *valid = @"false";
        
        if (baseMap.dockingPose.valid) {
            valid = @"true";
        }
        
        NSString *response = [NSString stringWithFormat:@"{'map': {'map_id':%ld, 'lines': %@, 'docking_pose': {'x': %f, 'y': %f, 'heading': %f, 'valid': %@}}}", (long)mapId, lines, baseMap.dockingPose.postion.x, baseMap.dockingPose.postion.y, baseMap.dockingPose.heading, valid];
        
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"mapGetTiles response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"mapGetTiles error: %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)saveMap:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling saveMap");
    NSString* param = [command.arguments objectAtIndex:0];
    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData:[param dataUsingEncoding:NSUTF8StringEncoding] options:0 error:NULL];
    NSLog(@"Json Object: %@", jsonObject);
    
    NSInteger mapId = [[jsonObject valueForKey:@"map_id"] integerValue];
    
    [[RASDKAicuConnecter sharedConnector] saveMap:mapId completion:^(RASDKCommand * _Nonnull sdkCommand) {
        NSString *response = [NSString stringWithFormat:@"{'cmd_id': %ld}", (long) sdkCommand.uid];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success saveMap response: %@", final);
        self.currentPermanent = mapId;
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error saveMap %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getCurrentMapId:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling getCurrentMapId");
    
    [[RASDKAicuConnecter sharedConnector] getCurrentMapId:^(NSInteger mapId) {
        NSString *response = [NSString stringWithFormat:@"{'map_id': %ld}", (long) mapId];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success getCurrentMapId response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"error getCurrentMapId %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getFirmware:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling getFirmware. getRobotID");

    [[RASDKAicuConnecter sharedConnector] getRobotID:^(RASDKRobotInfo * _Nonnull robotInfo) {
        self.uid = robotInfo.uid;

        NSLog(@"Calling getFirmware. Robot Id: %@", self.uid);
        
        self.firmwareService = [RASDKServices createFirmwareService];
        self.firmwareService.delegate = self;
        
        NSLog(@"Calling getFirmware. latestAvailableFirmwareForUID");
        
        [self.firmwareService latestAvailableFirmwareForUID:self.uid completion:^(NSString *version) {
            NSString *response = [NSString stringWithFormat:@"{'firmware': '%@'}", version];
            NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
            NSLog(@"success getFirmware response: %@", final);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } error:^(RASDKError *error) {
            NSLog(@"error getFirmware %@", error);
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
        
    } error:^(RASDKError * _Nonnull error) {
        NSLog(@"Error getFirmware %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)downloadFirmware:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Calling downloadFirmware. Robot Id: %@", self.uid);
    [self.firmwareService downloadFirmwareForUID:self.uid completion:^{
        NSString *response = [NSString stringWithFormat:@"{'success': true}"];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success downloadFirmware response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } error:^(RASDKError *error) {
        NSLog(@"error downloadFirmware %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)installFirmware:(CDVInvokedUrlCommand*)command {
    NSLog(@"Calling installFirmware. Robot Id: %@", self.uid);
    const BOOL isStored = [self.firmwareService isFirmwareStoredOnDeviceForUID:self.uid];

    if(isStored) {
        NSLog(@"InstallFirmware. The FW is stored on the phone. Starting discovery");
        self.temporalCmd = command;
        self.robotDiscoveryService = [RASDKServices createRobotDiscoveryService];
        self.robotDiscoveryService.delegate = self;
        [self.robotDiscoveryService startDiscovery];
    } else {
        NSLog(@"Error installFirmware. The firmware is not stored on the device");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)discoveryDidFindRobot:(RASDKRobot *)robot {
    NSLog(@"InstallFirmware. Found a robot: %@", robot);
    [self.robotDiscoveryService stopDiscovery];
    NSLog(@"InstallFirmware. Installing. RobotID: %@", self.uid);
    NSLog(@"InstallFirmware. Installing. URL: %@", robot.URL);
    [self.firmwareService installFirmwareForUID:self.uid toEndpoint:robot.URL completion:^{
        NSString *response = [NSString stringWithFormat:@"{'success': true}"];
        NSString *final = [response stringByReplacingOccurrencesOfString:@"'" withString:@"\""];
        NSLog(@"success installFirmware response: %@", final);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:final];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.temporalCmd.callbackId];
    } error:^(RASDKError *error) {
        NSLog(@"error installFirmware %@", error);
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.temporalCmd.callbackId];
    }];
}
- (void)discoveryIsCurrentlyUpToDateWithRobots:(NSArray<RASDKRobot
                                                *> *)robots {
    NSLog(@"InstallFirmware. discoveryIsCurrentlyUpToDateWithRobots");
}
- (void)discoveryDidFailWithError:(RASDKError *)error {
    NSLog(@"error installFirmware %@", error);
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"{'error': 'Unexpected error. Please, check the native console'}"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.temporalCmd.callbackId];
}

@end
