//
//  PushRuntime.m
//  Pushwoosh SDK
//  (c) Pushwoosh 2012
//

#import "PushRuntime.h"
#import "PushNotificationManager.h"
#import <CoreLocation/CoreLocation.h>
#import <objc/runtime.h>

#import "PW_SBJsonWriter.h"

void registerForRemoteNotifications() {
	[[UIApplication sharedApplication] registerForRemoteNotificationTypes:(UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
}

void * _getPushToken()
{
	return (void *)[[[PushNotificationManager pushManager] getPushToken] UTF8String];
}

char * g_tokenStr = 0;
char * g_registerErrStr = 0;
char * g_pushMessageStr = 0;
char * g_listenerName = 0;
void setListenerName(char * listenerName)
{
	free(g_listenerName); g_listenerName = 0;
	int len = strlen(listenerName);
	g_listenerName = malloc(len+1);
	strcpy(g_listenerName, listenerName);
	
	if(g_tokenStr) {
		UnitySendMessage(g_listenerName, "onRegisteredForPushNotifications", g_tokenStr);
		free(g_tokenStr); g_tokenStr = 0;
	}
	
	if(g_registerErrStr) {
		UnitySendMessage(g_listenerName, "onFailedToRegisteredForPushNotifications", g_registerErrStr);
		free(g_registerErrStr); g_registerErrStr = 0;
	}
	
	if(g_pushMessageStr) {
		UnitySendMessage(g_listenerName, "onPushNotificationsReceived", g_pushMessageStr);
		free(g_pushMessageStr); g_pushMessageStr = 0;
	}
}

void setIntTag(char * tagName, int tagValue)
{
	NSString *tagNameStr = [[NSString alloc] initWithUTF8String:tagName];
	NSDictionary * dict = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithInt:tagValue], tagNameStr, nil];
	[[PushNotificationManager pushManager] setTags:dict];
	[tagNameStr release];
}

void setStringTag(char * tagName, char * tagValue)
{
	NSString *tagNameStr = [[NSString alloc] initWithUTF8String:tagName];
	NSString *tagValueStr = [[NSString alloc] initWithUTF8String:tagValue];
	
	NSDictionary *dict = [NSDictionary dictionaryWithObjectsAndKeys:tagValueStr, tagNameStr, nil];
	[[PushNotificationManager pushManager] setTags:dict];
	[tagNameStr release];
	[tagValueStr release];
}

void sendLocation(double lat, double lon)
{
	CLLocation * location = [[CLLocation alloc] initWithLatitude:lat longitude:lon];
	[[PushNotificationManager pushManager] sendLocation:location];
	[location release];
}

void startLocationTracking()
{
	[[PushNotificationManager pushManager] startLocationTracking];
}

void startLocationTrackingWithMode(char * mode)
{
	NSString *modeString = [[NSString alloc] initWithUTF8String:mode];
	
	[[PushNotificationManager pushManager] startLocationTracking:modeString];
	
	[modeString release];
}

void stopLocationTracking()
{
	[[PushNotificationManager pushManager] stopLocationTracking];
}

@implementation UIApplication(Pushwoosh)

//succesfully registered for push notifications
- (void) onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token
{
	const char * str = [token UTF8String];
	if(!g_listenerName) {
		g_tokenStr = malloc(strlen(str)+1);
		strcpy(g_tokenStr, str);
		return;
	}
	
	UnitySendMessage(g_listenerName, "onRegisteredForPushNotifications", str);
}

//failed to register for push notifications
- (void) onDidFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
	const char * str = [[error description] UTF8String];
	if(!g_listenerName) {
		g_registerErrStr = malloc(strlen(str)+1);
		strcpy(g_registerErrStr, str);
		return;
	}
	
	UnitySendMessage(g_listenerName, "onFailedToRegisteredForPushNotifications", str);
}

//handle push notification, display alert, if this method is implemented onPushAccepted will not be called, internal message boxes will not be displayed
- (void) onPushAccepted:(PushNotificationManager *)pushManager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	PW_SBJsonWriter * json = [[PW_SBJsonWriter alloc] init];
	NSString *jsonRequestData =[json stringWithObject:pushNotification];
	[json release]; json = nil;
	
	const char * str = [jsonRequestData UTF8String];
	
	if(!g_listenerName) {
		g_pushMessageStr = malloc(strlen(str)+1);
		strcpy(g_pushMessageStr, str);
		return;
	}
	
	UnitySendMessage(g_listenerName, "onPushNotificationsReceived", str);
}

BOOL dynamicDidFinishLaunching(id self, SEL _cmd, id application, id launchOptions) {
	BOOL result = YES;
	
	if ([self respondsToSelector:@selector(application:pw_didFinishLaunchingWithOptions:)]) {
		result = (BOOL) [self application:application pw_didFinishLaunchingWithOptions:launchOptions];
	} else {
		[self applicationDidFinishLaunching:application];
		result = YES;
	}
	
	BOOL autoRegisterMode = ![[[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_NOAUTOREGISTER"] boolValue];
	if (autoRegisterMode) {
		[[UIApplication sharedApplication] registerForRemoteNotificationTypes:(UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
	}
	
	if(![PushNotificationManager pushManager].delegate) {
		[PushNotificationManager pushManager].delegate = (NSObject<PushNotificationDelegate> *)[UIApplication sharedApplication];
	}
	
	[[PushNotificationManager pushManager] handlePushReceived:launchOptions];
	[[PushNotificationManager pushManager] sendAppOpen];
	
	return result;
}

void dynamicDidRegisterForRemoteNotificationsWithDeviceToken(id self, SEL _cmd, id application, id devToken) {
	if ([self respondsToSelector:@selector(application:pw_didRegisterForRemoteNotificationsWithDeviceToken:)]) {
		[self application:application pw_didRegisterForRemoteNotificationsWithDeviceToken:devToken];
	}
	
	[[PushNotificationManager pushManager] handlePushRegistration:devToken];
}

void dynamicDidFailToRegisterForRemoteNotificationsWithError(id self, SEL _cmd, id application, id error) {
	if ([self respondsToSelector:@selector(application:pw_didFailToRegisterForRemoteNotificationsWithError:)]) {
		[self application:application pw_didFailToRegisterForRemoteNotificationsWithError:error];
	}
	
	NSLog(@"Error registering for push notifications. Error: %@", error);
	
	[[PushNotificationManager pushManager] handlePushRegistrationFailure:error];
}

void dynamicDidReceiveRemoteNotification(id self, SEL _cmd, id application, id userInfo) {
	if ([self respondsToSelector:@selector(application:pw_didReceiveRemoteNotification:)]) {
		[self application:application pw_didReceiveRemoteNotification:userInfo];
	}
	
	[[PushNotificationManager pushManager] handlePushReceived:userInfo];
}


- (void) pw_setDelegate:(id<UIApplicationDelegate>)delegate {
	Method method = nil;
	method = class_getInstanceMethod([delegate class], @selector(application:didFinishLaunchingWithOptions:));
	
	if (method) {
		class_addMethod([delegate class], @selector(application:pw_didFinishLaunchingWithOptions:), (IMP)dynamicDidFinishLaunching, "v@:::");
		method_exchangeImplementations(class_getInstanceMethod([delegate class], @selector(application:didFinishLaunchingWithOptions:)), class_getInstanceMethod([delegate class], @selector(application:pw_didFinishLaunchingWithOptions:)));
	} else {
		class_addMethod([delegate class], @selector(application:didFinishLaunchingWithOptions:), (IMP)dynamicDidFinishLaunching, "v@:::");
	}
	
	method = class_getInstanceMethod([delegate class], @selector(application:didRegisterForRemoteNotificationsWithDeviceToken:));
	if(method) {
		class_addMethod([delegate class], @selector(application:pw_didRegisterForRemoteNotificationsWithDeviceToken:), (IMP)dynamicDidRegisterForRemoteNotificationsWithDeviceToken, "v@:::");
		method_exchangeImplementations(class_getInstanceMethod([delegate class], @selector(application:didRegisterForRemoteNotificationsWithDeviceToken:)), class_getInstanceMethod([delegate class], @selector(application:pw_didRegisterForRemoteNotificationsWithDeviceToken:)));
	}
	else {
		class_addMethod([delegate class], @selector(application:didRegisterForRemoteNotificationsWithDeviceToken:), (IMP)dynamicDidRegisterForRemoteNotificationsWithDeviceToken, "v@:::");
	}
	
	method = class_getInstanceMethod([delegate class], @selector(application:didFailToRegisterForRemoteNotificationsWithError:));
	if(method) {
		class_addMethod([delegate class], @selector(application:pw_didFailToRegisterForRemoteNotificationsWithError:), (IMP)dynamicDidFailToRegisterForRemoteNotificationsWithError, "v@:::");
		method_exchangeImplementations(class_getInstanceMethod([delegate class], @selector(application:didFailToRegisterForRemoteNotificationsWithError:)), class_getInstanceMethod([delegate class], @selector(application:pw_didFailToRegisterForRemoteNotificationsWithError:)));
	}
	else {
		class_addMethod([delegate class], @selector(application:didFailToRegisterForRemoteNotificationsWithError:), (IMP)dynamicDidFailToRegisterForRemoteNotificationsWithError, "v@:::");
	}
	
	method = class_getInstanceMethod([delegate class], @selector(application:didReceiveRemoteNotification:));
	if(method) {
		class_addMethod([delegate class], @selector(application:pw_didReceiveRemoteNotification:), (IMP)dynamicDidReceiveRemoteNotification, "v@:::");
		method_exchangeImplementations(class_getInstanceMethod([delegate class], @selector(application:didReceiveRemoteNotification:)), class_getInstanceMethod([delegate class], @selector(application:pw_didReceiveRemoteNotification:)));
	}
	else {
		class_addMethod([delegate class], @selector(application:didReceiveRemoteNotification:), (IMP)dynamicDidReceiveRemoteNotification, "v@:::");
	}
	
	[self pw_setDelegate:delegate];
}

- (void) pw_setApplicationIconBadgeNumber:(NSInteger) badgeNumber {
	[self pw_setApplicationIconBadgeNumber:badgeNumber];
	
	[[PushNotificationManager pushManager] sendBadges:badgeNumber];
}

+ (void) load {
	method_exchangeImplementations(class_getInstanceMethod(self, @selector(setApplicationIconBadgeNumber:)), class_getInstanceMethod(self, @selector(pw_setApplicationIconBadgeNumber:)));
	method_exchangeImplementations(class_getInstanceMethod(self, @selector(setDelegate:)), class_getInstanceMethod(self, @selector(pw_setDelegate:)));
	
	UIApplication *app = [UIApplication sharedApplication];
	NSLog(@"Initializing application: %@, %@", app, app.delegate);
}

@end
