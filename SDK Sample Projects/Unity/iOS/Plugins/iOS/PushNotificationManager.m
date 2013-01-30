//
//  PushNotificationManager.m
//  Pushwoosh SDK
//  (c) Pushwoosh 2012
//

#import "PushNotificationManager.h"

#import "HtmlWebViewController.h"
#import "PWRequestManager.h"
#import "PWRegisterDeviceRequest.h"
#import "PWSetTagsRequest.h"
#import "PWSendBadgeRequest.h"
#import "PWAppOpenRequest.h"
#import "PWPushStatRequest.h"
#import "PWGetNearestZoneRequest.h"
#import "PWApplicationEventRequest.h"

#include <sys/socket.h> // Per msqr
#include <sys/sysctl.h>
#include <net/if.h>
#include <net/if_dl.h>
#import <CommonCrypto/CommonDigest.h>

#define kServiceHtmlContentFormatUrl @"http://cp.pushwoosh.com/content/%@"

@interface UIApplication(Pushwoosh)
- (void) pw_setApplicationIconBadgeNumber:(NSInteger) badgeNumber;
@end

@implementation PushNotificationManager

@synthesize appCode, appName, richPushWindow, pushNotifications, delegate;
@synthesize supportedOrientations, showPushnotificationAlert;

- (NSString *) stringFromMD5: (NSString *)val{
    
    if(val == nil || [val length] == 0)
        return nil;
    
    const char *value = [val UTF8String];
    
    unsigned char outputBuffer[CC_MD5_DIGEST_LENGTH];
    CC_MD5(value, strlen(value), outputBuffer);
    
    NSMutableString *outputString = [[NSMutableString alloc] initWithCapacity:CC_MD5_DIGEST_LENGTH * 2];
    for(NSInteger count = 0; count < CC_MD5_DIGEST_LENGTH; count++){
        [outputString appendFormat:@"%02x",outputBuffer[count]];
    }
    
    return [outputString autorelease];
}

////////////////////////////////////////////////////////////////////////////////
#pragma mark -
#pragma mark Private Methods

// Return the local MAC addy
// Courtesy of FreeBSD hackers email list
// Accidentally munged during previous update. Fixed thanks to erica sadun & mlamb.
- (NSString *) macaddress{
    
    int                 mib[6];
    size_t              len;
    char                *buf;
    unsigned char       *ptr;
    struct if_msghdr    *ifm;
    struct sockaddr_dl  *sdl;
    
    mib[0] = CTL_NET;
    mib[1] = AF_ROUTE;
    mib[2] = 0;
    mib[3] = AF_LINK;
    mib[4] = NET_RT_IFLIST;
    
    if ((mib[5] = if_nametoindex("en0")) == 0) {
        printf("Error: if_nametoindex error\n");
        return NULL;
    }
    
    if (sysctl(mib, 6, NULL, &len, NULL, 0) < 0) {
        printf("Error: sysctl, take 1\n");
        return NULL;
    }
    
    if ((buf = malloc(len)) == NULL) {
        printf("Could not allocate memory. error!\n");
        return NULL;
    }
    
    if (sysctl(mib, 6, buf, &len, NULL, 0) < 0) {
        printf("Error: sysctl, take 2");
        free(buf);
        return NULL;
    }
    
    ifm = (struct if_msghdr *)buf;
    sdl = (struct sockaddr_dl *)(ifm + 1);
    ptr = (unsigned char *)LLADDR(sdl);
    NSString *outstring = [NSString stringWithFormat:@"%02X:%02X:%02X:%02X:%02X:%02X", 
                           *ptr, *(ptr+1), *(ptr+2), *(ptr+3), *(ptr+4), *(ptr+5)];
    free(buf);
    
    return outstring;
}

////////////////////////////////////////////////////////////////////////////////
#pragma mark -
#pragma mark Public Methods

- (NSString *) uniqueDeviceIdentifier{
    NSString *macaddress = [self macaddress];
    NSString *bundleIdentifier = [[NSBundle mainBundle] bundleIdentifier];
    
    NSString *stringToHash = [NSString stringWithFormat:@"%@%@",macaddress,bundleIdentifier];
    NSString *uniqueIdentifier = [self stringFromMD5:stringToHash];
    
    return uniqueIdentifier;
}

- (NSString *) uniqueGlobalDeviceIdentifier{
    NSString *macaddress = [self macaddress];
    NSString *uniqueIdentifier = [self stringFromMD5:macaddress];
    
    return uniqueIdentifier;
}

static PushNotificationManager * instance = nil;

// this method is for backward compatibility
- (id) initWithApplicationCode:(NSString *)_appCode navController:(UIViewController *) _navController appName:(NSString *)_appName {
	return [self initWithApplicationCode:_appCode appName:_appName];
}

- (id) initWithApplicationCode:(NSString *)_appCode appName:(NSString *)_appName{
	if(self = [super init]) {
		self.supportedOrientations = PWOrientationPortrait | PWOrientationPortraitUpsideDown | PWOrientationLandscapeLeft | PWOrientationLandscapeRight;
		self.appCode = _appCode;
		self.appName = _appName;
		richPushWindow = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
		richPushWindow.windowLevel = UIWindowLevelStatusBar + 1.0f;
		
		internalIndex = 0;
		pushNotifications = [[NSMutableDictionary alloc] init];
		showPushnotificationAlert = TRUE;
		
		[[NSUserDefaults standardUserDefaults] setObject:_appCode forKey:@"Pushwoosh_APPID"];
		if(_appName) {
			[[NSUserDefaults standardUserDefaults] setObject:_appName forKey:@"Pushwoosh_APPNAME"];
		}
		
		instance = self;
	}
	
	return self;
}

+ (void)initializeWithAppCode:(NSString *)appCode appName:(NSString *)appName {
	[[NSUserDefaults standardUserDefaults] setObject:appCode forKey:@"Pushwoosh_APPID"];
	
	if(appName) {
		[[NSUserDefaults standardUserDefaults] setObject:appName forKey:@"Pushwoosh_APPNAME"];
	}
}

+ (BOOL) getAPSProductionStatus {
	NSString * provisioning = [[NSBundle mainBundle] pathForResource:@"embedded.mobileprovision" ofType:nil];
	if(!provisioning)
		return YES;	//AppStore
	
	NSString * contents = [NSString stringWithContentsOfFile:provisioning encoding:NSASCIIStringEncoding error:nil];
	if(!contents)
		return YES;

	NSRange start = [contents rangeOfString:@"<?xml"];
	NSRange end = [contents rangeOfString:@"</plist>"];
	start.length = end.location + end.length - start.location;
	
	NSString * profile =[contents substringWithRange:start];
	if(!profile)
		return YES;
	
	NSData * profileData = [profile dataUsingEncoding:NSUTF8StringEncoding];
	NSString *error = nil;;
	NSPropertyListFormat format;
	NSDictionary* plist = [NSPropertyListSerialization propertyListFromData:profileData mutabilityOption:NSPropertyListImmutable format:&format errorDescription:&error];
	
	NSDictionary * entitlements = [plist objectForKey:@"Entitlements"];
//	NSNumber * allowNumber = [entitlements objectForKey:@"get-task-allow"];
	
	//could be development or production
	NSString * apsGateway = [entitlements objectForKey:@"aps-environment"];
	
	if(!apsGateway) {
		UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Pushwoosh Error" message:@"Your provisioning profile does not have APS entry. Please make your profile push compatible." delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil, nil];
		[alert show];
		[alert release];
	}
	
	if([apsGateway isEqualToString:@"development"])
		return NO;
	
	return YES;
}

+ (NSString *) getAppIdFromBundle:(BOOL)productionAPS {
	NSString * appid = nil;
	if(!productionAPS) {
		appid = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_APPID_Dev"];
		if(appid)
			return appid;
	}
	
	return [[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_APPID"];
}

+ (PushNotificationManager *)pushManager {
	if(instance == nil) {
		NSString * appid = [self getAppIdFromBundle:[self getAPSProductionStatus]];
		
		if(!appid) {
			appid = [[NSUserDefaults standardUserDefaults] objectForKey:@"Pushwoosh_APPID"];

			if(!appid) {
				return nil;
			}
		}
		
		NSString * appname = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_APPNAME"];
		if(!appname)
			appname = [[NSUserDefaults standardUserDefaults] objectForKey:@"Pushwoosh_APPNAME"];
		
		if(!appname)
			appname = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
		
		if(!appname)
			appname = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
			
		if(!appname) {
			appname = @"";
		}
		
		instance = [[PushNotificationManager alloc] initWithApplicationCode:appid appName:appname ];
	}
	
	return instance;
}

- (void) showWebView {
	self.richPushWindow.alpha = 0.0f;
	self.richPushWindow.windowLevel = UIWindowLevelStatusBar + 1.0f;
	self.richPushWindow.hidden = NO;
	self.richPushWindow.transform = CGAffineTransformMakeScale(0.01, 0.01);
	
	[UIView animateWithDuration:0.3 animations:^{
		self.richPushWindow.transform = CGAffineTransformIdentity;
		self.richPushWindow.alpha = 1.0f;
	} completion:^(BOOL finished) {
	}];

}

- (void) showPushPage:(NSString *)pageId {
	NSString *url = [NSString stringWithFormat:kServiceHtmlContentFormatUrl, pageId];
	HtmlWebViewController *vc = [[HtmlWebViewController alloc] initWithURLString:url];
	vc.delegate = self;
	vc.supportedOrientations = supportedOrientations;

	self.richPushWindow.rootViewController = vc;
	[vc view];
	[vc release];
}

- (void)htmlWebViewControllerDidClose:(HtmlWebViewController *)viewController {
	
	self.richPushWindow.transform = CGAffineTransformIdentity;
	[UIView animateWithDuration:0.3 delay:0 options:UIViewAnimationOptionCurveEaseOut animations:^{
		self.richPushWindow.transform = CGAffineTransformMakeScale(0.01, 0.01);
		self.richPushWindow.alpha = 0.0f;
	} completion:^(BOOL finished) {
		self.richPushWindow.hidden = YES;

	}];
}

- (void) sendDevTokenToServer:(NSString *)deviceID {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
	NSString * appLocale = @"en";
	NSLocale * locale = (NSLocale *)CFLocaleCopyCurrent();
	NSString * localeId = [locale localeIdentifier];
	
	if([localeId length] > 2)
		localeId = [localeId stringByReplacingCharactersInRange:NSMakeRange(2, [localeId length]-2) withString:@""];
	
	[locale release]; locale = nil;
	
	appLocale = localeId;
	
	NSArray * languagesArr = (NSArray *) CFLocaleCopyPreferredLanguages();	
	if([languagesArr count] > 0)
	{
		NSString * value = [languagesArr objectAtIndex:0];
		
		if([value length] > 2)
			value = [value stringByReplacingCharactersInRange:NSMakeRange(2, [value length]-2) withString:@""];
		
		appLocale = [[value copy] autorelease];
	}
	
	[languagesArr release]; languagesArr = nil;
	
	PWRegisterDeviceRequest *request = [[PWRegisterDeviceRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
	request.pushToken = deviceID;
	request.language = appLocale;
	request.timeZone = [NSString stringWithFormat:@"%d", [[NSTimeZone localTimeZone] secondsFromGMT]];
	
	NSError *error = nil;
	if ([[PWRequestManager sharedManager] sendRequest:request error:&error]) {
		NSLog(@"Registered for push notifications: %@", deviceID);

		if([delegate respondsToSelector:@selector(onDidRegisterForRemoteNotificationsWithDeviceToken:)] ) {
			[delegate performSelectorOnMainThread:@selector(onDidRegisterForRemoteNotificationsWithDeviceToken:) withObject:[self getPushToken] waitUntilDone:NO];
		}
	} else {
		NSLog(@"Registered for push notifications failed");

		if([delegate respondsToSelector:@selector(onDidFailToRegisterForRemoteNotificationsWithError:)] ) {
			[delegate performSelectorOnMainThread:@selector(onDidFailToRegisterForRemoteNotificationsWithError:) withObject:error waitUntilDone:NO];
		}
	}
	
	[request release]; request = nil;
	[pool release]; pool = nil;
}

- (void) handlePushRegistrationString:(NSString *)deviceID {
	
	[[NSUserDefaults standardUserDefaults] setObject:deviceID forKey:@"PWPushUserId"];
	
	[self performSelectorInBackground:@selector(sendDevTokenToServer:) withObject:deviceID];
}

- (void) handlePushRegistration:(NSData *)devToken {
	NSMutableString *deviceID = [NSMutableString stringWithString:[devToken description]];
	
	//Remove <, >, and spaces
	[deviceID replaceOccurrencesOfString:@"<" withString:@"" options:1 range:NSMakeRange(0, [deviceID length])];
	[deviceID replaceOccurrencesOfString:@">" withString:@"" options:1 range:NSMakeRange(0, [deviceID length])];
	[deviceID replaceOccurrencesOfString:@" " withString:@"" options:1 range:NSMakeRange(0, [deviceID length])];
	
	[[NSUserDefaults standardUserDefaults] setObject:deviceID forKey:@"PWPushUserId"];
	
	[self performSelectorInBackground:@selector(sendDevTokenToServer:) withObject:deviceID];
}

- (void) handlePushRegistrationFailure:(NSError *) error {
	if([delegate respondsToSelector:@selector(onDidFailToRegisterForRemoteNotificationsWithError:)] ) {
		[delegate performSelectorOnMainThread:@selector(onDidFailToRegisterForRemoteNotificationsWithError:) withObject:error waitUntilDone:NO];
	}
}

- (NSString *) getPushToken {
	return [[NSUserDefaults standardUserDefaults] objectForKey:@"PWPushUserId"];
}

#pragma mark URL redirect handling flow

//This method is added to work with shorten urls
//According to ios 6, if user isn't logged in appstore, then when safari opens itunes url system will ask permission to run appstore.
//But still if application open appstore url, system will open it without any alerts.
- (void) openUrl: (NSURL *) url {
	//When opening nsurlconnection to some url if it has some redirect, then connection will ask delegate what to do.
	//But if url has no redirects, then THIS CODE WILL NOT WORK.
	//
	//Pushwoosh.com guarantee that any http/https url is shorten URL.
	//Unshort url and open it by usual way.
	if ([[url scheme] hasPrefix:@"http"]) {
		NSURLConnection *connection  = [[NSURLConnection alloc] initWithRequest:[NSMutableURLRequest requestWithURL:url] delegate:self];
		if (!connection) {
			return;
		}
		
		[connection release];
		return;
	}
	
	//If url has cusmtom scheme like facebook:// or itms:// we need to open it directly:
	[[UIApplication sharedApplication] openURL:url];
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
	NSLog(@"Url: %@", [response URL]);

	//as soon as all the redirects finished we can open the final URL
	[[UIApplication sharedApplication] openURL:[response URL]];
}

#pragma mark -
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
	if(buttonIndex != 1) {
		if(!alertView.tag)
			return;
		
		[pushNotifications removeObjectForKey:[NSNumber numberWithInt:alertView.tag]];
		return;
	}
	
	NSDictionary *lastPushDict = [pushNotifications objectForKey:[NSNumber numberWithInt:alertView.tag]];
	NSString *htmlPageId = [lastPushDict objectForKey:@"h"];
	if(htmlPageId) {
		[self showPushPage:htmlPageId];
	}
    
	NSString *linkUrl = [lastPushDict objectForKey:@"l"];	
	if(linkUrl) {
		[self openUrl:[NSURL URLWithString:linkUrl]];
	}
	
	if([delegate respondsToSelector:@selector(onPushAccepted: withNotification:)] ) {
		[delegate onPushAccepted:self withNotification:lastPushDict];
	}
	else
	if([delegate respondsToSelector:@selector(onPushAccepted: withNotification: onStart:)] ) {
		[delegate onPushAccepted:self withNotification:lastPushDict onStart:NO];
	}
	
	[pushNotifications removeObjectForKey:[NSNumber numberWithInt:alertView.tag]];
}

- (BOOL) handlePushReceived:(NSDictionary *)userInfo {
	
	BOOL isPushOnStart = NO;
	NSDictionary *pushDict = [userInfo objectForKey:@"aps"];
	if(!pushDict) {
		//try as launchOptions dictionary
		userInfo = [userInfo objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
		pushDict = [userInfo objectForKey:@"aps"];
		isPushOnStart = YES;
	}
	
	if (!pushDict)
		return NO;
	
	//check if the app is really running
	if([[UIApplication sharedApplication] respondsToSelector:@selector(applicationState)] && [[UIApplication sharedApplication] applicationState] != UIApplicationStateActive) {
		isPushOnStart = YES;
	}
	
	NSString *hash = [userInfo objectForKey:@"p"];
	
	[self performSelectorInBackground:@selector(sendStatsBackground:) withObject:hash];
	
	if([delegate respondsToSelector:@selector(onPushReceived: withNotification: onStart:)] ) {
		[delegate onPushReceived:self withNotification:userInfo onStart:isPushOnStart];
		return YES;
	}

	NSString *alertMsg = [pushDict objectForKey:@"alert"];
//	NSString *badge = [pushDict objectForKey:@"badge"];
//	NSString *sound = [pushDict objectForKey:@"sound"];
	NSString *htmlPageId = [userInfo objectForKey:@"h"];
//	NSString *customData = [userInfo objectForKey:@"u"];
	NSString *linkUrl = [userInfo objectForKey:@"l"];
	
	//the app is running, display alert only
	if(!isPushOnStart && showPushnotificationAlert) {
		UIAlertView *alert = [[UIAlertView alloc] initWithTitle:self.appName message:alertMsg delegate:self cancelButtonTitle:@"Cancel" otherButtonTitles:@"OK", nil];
		alert.tag = ++internalIndex;
		[pushNotifications setObject:userInfo forKey:[NSNumber numberWithInt:internalIndex]];
		[alert show];
		[alert release];
		return YES;
	}
	
	if(htmlPageId) {
		[self showPushPage:htmlPageId];
	}
    
	if(linkUrl) {
		[self openUrl:[NSURL URLWithString:linkUrl]];
	}
	
	if([delegate respondsToSelector:@selector(onPushAccepted: withNotification:)] ) {
		[delegate onPushAccepted:self withNotification:userInfo];
	}
	else
	if([delegate respondsToSelector:@selector(onPushAccepted: withNotification: onStart:)] ) {
		[delegate onPushAccepted:self withNotification:userInfo onStart:isPushOnStart];
	}

	return YES;
}

- (NSDictionary *) getApnPayload:(NSDictionary *)pushNotification {
	return [pushNotification objectForKey:@"aps"];
}

- (NSString *) getCustomPushData:(NSDictionary *)pushNotification {
	return [pushNotification objectForKey:@"u"];
}

- (void) sendStatsBackground:(NSString *)hash {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

	PWPushStatRequest *request = [[PWPushStatRequest alloc] init];
	request.appId = appCode;
	request.hash = hash;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
	
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"sendStats completed");
	} else {
		NSLog(@"sendStats failed");
	}
	
	[request release]; request = nil;
	
	[pool release]; pool = nil;
}

- (void) sendTagsBackground: (NSDictionary *) tags {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    PWSetTagsRequest *request = [[PWSetTagsRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
    request.tags = tags;
	
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"setTags completed");
	} else {
		NSLog(@"setTags failed");
	}
	
	[request release]; request = nil;

	
	[pool release]; pool = nil;
}

- (void) sendLocation: (CLLocation *) location {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	NSLog(@"Sending location: %@", location);
	
    PWGetNearestZoneRequest *request = [[PWGetNearestZoneRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
    request.coordinate = location.coordinate;
	
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"getNearestZone completed");
	} else {
		NSLog(@"getNearestZone failed");
	}
	
	[request release]; request = nil;
	
	NSLog(@"Locaiton sent");
	
	[pool release]; pool = nil;
}

- (void) sendAppOpenBackground {
	//it's ok to call this method without push token
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	PWAppOpenRequest *request = [[PWAppOpenRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
	
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"sending appOpen completed");
	} else {
		NSLog(@"sending appOpen failed");
	}
	
	[request release]; request = nil;
	[pool release]; pool = nil;
}

- (void) sendBadgesBackground: (NSNumber *) badge {
	if([[PushNotificationManager pushManager] getPushToken] == nil)
		return;
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

	PWSendBadgeRequest *request = [[PWSendBadgeRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
	request.badge = [badge intValue];
		
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"setBadges completed");
	} else {
		NSLog(@"setBadges failed");
	}
	
	[request release]; request = nil;
	[pool release]; pool = nil;
}

- (void) sendGoalBackground: (PWApplicationEventRequest *) request {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	if ([[PWRequestManager sharedManager] sendRequest:request]) {
		NSLog(@"sendGoals completed");
	} else {
		NSLog(@"sendGoals failed");
	}

	[pool release]; pool = nil;
}

- (void) sendBadges: (NSInteger) badge {
	[self performSelectorInBackground:@selector(sendBadgesBackground:) withObject:[NSNumber numberWithInt:badge]];
}

- (void) sendAppOpen {
	[self performSelectorInBackground:@selector(sendAppOpenBackground) withObject:nil];
}

- (void) setTags: (NSDictionary *) tags {
	[self performSelectorInBackground:@selector(sendTagsBackground:) withObject:tags];
}

- (void) recordGoal: (NSString *) goal {
	[self recordGoal:goal withCount:nil];
}

- (void) recordGoal: (NSString *) goal withCount: (NSNumber *) count {
	PWApplicationEventRequest *request = [[PWApplicationEventRequest alloc] init];
	request.appId = appCode;
	request.hwid = [self uniqueGlobalDeviceIdentifier];
	request.goal = goal;
	request.count = count;

	[self performSelectorInBackground:@selector(sendGoalBackground:) withObject:request];
	[request release];
}

//clears the notifications from the notification center
+ (void) clearNotificationCenter {
	
	UIApplication* application = [UIApplication sharedApplication];
	NSArray* scheduledNotifications = [NSArray arrayWithArray:application.scheduledLocalNotifications];
	application.scheduledLocalNotifications = scheduledNotifications;
}

- (void) dealloc {
	self.richPushWindow = nil;
	self.delegate = nil;
	self.appCode = nil;
	self.pushNotifications = nil;
	
	[super dealloc];
}

@end

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


#import <objc/runtime.h>
#import "PW_SBJsonWriter.h"

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
	
	[[UIApplication sharedApplication] registerForRemoteNotificationTypes:(UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert)];
	
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
