//
//  CustomPageViewController.h
//  PushNotificationsApp
//
//  Created by Alexander Anisimov on 7/31/13.
//
//

#import <UIKit/UIKit.h>

@interface CustomPageViewController : UIViewController

@property (retain, nonatomic) UIColor *bgColor;
@property (nonatomic) NSInteger pageId;

@property (retain, nonatomic) IBOutlet UILabel *titleLabel;

- (IBAction)closeAction:(id)sender;

@end
