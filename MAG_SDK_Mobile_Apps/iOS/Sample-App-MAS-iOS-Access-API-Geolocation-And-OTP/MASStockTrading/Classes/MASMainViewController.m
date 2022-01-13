//
//  MASMainViewController.m
//  MASStockTrading
//
//  Copyright (c) 2016 CA. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import <MASFoundation/MASFoundation.h>
#import <MASUI/MASUI.h>

#import "MASMainViewController.h"

@interface MASMainViewController ()

@property (nonatomic, weak) IBOutlet UIActivityIndicatorView *activityIndicatorView;
@property (nonatomic, weak) IBOutlet UILabel *appNameLabel;

@property (nonatomic, weak) IBOutlet UITextView *debugTextView;
@property (nonatomic, weak) IBOutlet UIButton *buyButton;
@property (nonatomic, weak) IBOutlet UIButton *sellButton;
@property (nonatomic, weak) IBOutlet UITextField *stockCodeField;
@property (nonatomic, weak) IBOutlet UITextField *numberOfShareField;

@end


@implementation MASMainViewController


# pragma mark - Lifecycle


- (void)viewDidLoad
{
    [super viewDidLoad];
    
    //
    // Application Name Label
    //
    NSString *appName = [NSBundle mainBundle].infoDictionary[@"CFBundleName"];
    self.appNameLabel.text = appName;
    
    //
    // Begin the MAS framework
    //
    [MAS setGrantFlow:MASGrantFlowPassword];
    
    
    [MAS setWillHandleOTPAuthentication:YES];
    
    //
    //  Following blocks are required to be implemented for custom OTP UI
    //
//    [MAS setOTPChannelSelectionBlock:^(NSArray *supportedOTPChannels, MASOTPGenerationBlock otpGenerationBlock) {
//        
//    }];
//    
//    [MAS setOTPCredentialsBlock:^(MASOTPFetchCredentialsBlock otpBlock) {
//        
//    }];
    
    [MAS start:^(BOOL completed, NSError *error) {
        
        //
        // Initialized SDK
        //
    }];

}


//
//  If you want to try out the simplest functioning app talking to an already setup Gateway:
//
//      1) Ensure you have run 'pod install' on the command line in the project root directory
//      2) Shutdown Xcode and restart using your <projectname>.xcworkspace file
//      3) You must have a running Gateway with at least MAG and OTK installed
//      4) You must have an application record created in your OTK Admin console
//      5) Obtain the msso_config.json for that application record and drag and drop it into
//         your project.
//      6) Uncommend the below code
//
//  That's it!!  Start it and and try it out
//

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    
    //
    // We recommend implementing this method in all view controllers (or
    // a common UIViewController superclass of all your view controllers).
    //
    // Seeing this called can save you much time in the rare but painful case
    // that it happens.  It certainly doesn't hurt to do it.
    //
    
    NSLog(@"didReceiveMemoryWarning called");
}


- (UIStatusBarStyle)preferredStatusBarStyle
{
    //
    // Just making the status bar light so it can be seen against the
    // darker background
    //
    return UIStatusBarStyleLightContent;
}


- (IBAction)buyStock:(id)sender
{
    [MAS postTo:[NSString stringWithFormat:@"/trade?requestCode=%@",[[NSUUID UUID] UUIDString]] withParameters:@{@"stock":self.stockCodeField.text, @"shares":self.numberOfShareField.text} andHeaders:@{@"action":@"Buy"} completion:^(NSDictionary *responseInfo, NSError *error) {
        
        if (error)
        {
            self.debugTextView.text = error.localizedDescription;
        }
        else {
            self.debugTextView.text = [NSString stringWithFormat:@"%@",responseInfo];
        }
    }];
}

- (IBAction)sellStock:(id)sender
{
    [MAS postTo:[NSString stringWithFormat:@"/trade?requestCode=%@",[[NSUUID UUID] UUIDString]] withParameters:@{@"stock":self.stockCodeField.text, @"shares":self.numberOfShareField.text} andHeaders:@{@"action":@"Sell"} completion:^(NSDictionary *responseInfo, NSError *error) {
        
        if (error)
        {
            self.debugTextView.text = error.localizedDescription;
        }
        else {
            self.debugTextView.text = [NSString stringWithFormat:@"%@",responseInfo];
        }
    }];
}

@end
