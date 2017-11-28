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
- (void)btRobotDisconnect:(CDVInvokedUrlCommand*)command;
- (void)startRobotDiscovery:(CDVInvokedUrlCommand*)command;
- (void)connectToWifi:(CDVInvokedUrlCommand*)command;
- (void)robotGetRobotId:(CDVInvokedUrlCommand*)command;

- (void)getFirmware:(CDVInvokedUrlCommand*)command;

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

