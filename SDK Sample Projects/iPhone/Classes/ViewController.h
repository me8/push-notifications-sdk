//
//  ViewController.h
//  PushNotificationsApp
//
//  (c) Pushwoosh 2012
//

#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import "PushNotificationManager.h"

@interface ViewController : UIViewController<UITextFieldDelegate, PushNotificationDelegate> {
}

@property (nonatomic, retain) IBOutlet UITextField *aliasField;
@property (nonatomic, retain) IBOutlet UITextField *favNumField;
@property (nonatomic, retain) IBOutlet UILabel *statusLabel;

- (IBAction) submitAction:(id)sender;


//succesfully registered for push notifications
- (void) onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token;

//failed to register for push notifications
- (void) onDidFailToRegisterForRemoteNotificationsWithError:(NSError *)error;

//user pressed OK on the push notification
- (void) onPushAccepted:(PushNotificationManager *)pushManager withNotification:(NSDictionary *)pushNotification;

//received tags from the server
- (void) onTagsReceived:(NSDictionary *)tags;

//error receiving tags from the server
- (void) onTagsFailedToReceive:(NSError *)error;

@end
