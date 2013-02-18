//
//  PushAirExtension.h
//  Pushwoosh SDK
//  (c) Pushwoosh 2013
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "PushNotificationManager.h"
#import "FlashRuntimeExtensions.h"

void PushwooshContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet);
void PushwooshContextFinalizer(FREContext ctx);
void PushwooshExtInitializer(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet );
void PushwooshExtFinalizer(void *extData);

@interface UIApplication(SupressWarnings)
- (void)application:(UIApplication *)application pw_didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)devToken;
- (void)application:(UIApplication *)application pw_didFailToRegisterForRemoteNotificationsWithError:(NSError *)err;
- (void)application:(UIApplication *)application pw_didReceiveRemoteNotification:(NSDictionary *)userInfo;

- (BOOL)application:(UIApplication *)application pw_didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;
@end

