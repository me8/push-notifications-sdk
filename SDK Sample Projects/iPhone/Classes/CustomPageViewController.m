//
//  CustomPageViewController.m
//  PushNotificationsApp
//
//  Created by Alexander Anisimov on 7/31/13.
//
//

#import "CustomPageViewController.h"

@implementation CustomPageViewController

- (void)viewDidLoad {
    [super viewDidLoad];
	self.view.backgroundColor = self.bgColor;
	self.titleLabel.text = [NSString stringWithFormat:@"Custom page with id %d", self.pageId];
}

- (void)viewDidUnload {
    [self setTitleLabel:nil];
    [super viewDidUnload];
}

- (IBAction)closeAction:(id)sender {
	[self dismissModalViewControllerAnimated:YES];
}

- (void)dealloc {
	[_bgColor release];
    [_titleLabel release];
    [super dealloc];
}

@end
