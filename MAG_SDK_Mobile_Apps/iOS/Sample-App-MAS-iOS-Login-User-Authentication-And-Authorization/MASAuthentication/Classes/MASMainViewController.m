//
//  MASMainViewController.m
//  MASAuthentication
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
@property (nonatomic, weak) IBOutlet UILabel *authenticationStatusLabel;

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
    // Start activity indicator
    //
    [self.activityIndicatorView startAnimating];
    
    //
    // Begin the MAS framework
    //
    __block MASMainViewController *blockSelf = self;
    
    //
    // Setting flow of SDK
    //
    [MAS setGrantFlow:MASGrantFlowPassword];
    
    //
    //  Change this value for custom login dialog or default login dialog through MASUI
    //
    [MAS setWillHandleAuthentication:NO];
    
    [MAS setUserAuthCredentials:^(MASAuthCredentialsBlock  _Nonnull authCredentialBlock) {
        
        __block MASAuthCredentialsBlock blockAuthCredBlock = authCredentialBlock;
        
        UIAlertController *alertController = [UIAlertController
                                              alertControllerWithTitle:@"User Authentication"
                                              message:@"Please enter username and password"
                                              preferredStyle:UIAlertControllerStyleAlert];
        
        [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
            textField.placeholder = NSLocalizedString(@"Username", @"Username");
        }];
        
        [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
            textField.placeholder = NSLocalizedString(@"Password", @"Password");
            textField.secureTextEntry = YES;
        }];
        
        UIAlertAction *loginAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK action")
                                                              style:UIAlertActionStyleDefault
                                                            handler:^(UIAlertAction *action) {
                                                                UITextField *username = alertController.textFields.firstObject;
                                                                UITextField *password = alertController.textFields.lastObject;
                                                                
                                                                MASAuthCredentialsPassword *authCredentials = [MASAuthCredentialsPassword initWithUsername:username.text password:password.text];
                                                                blockAuthCredBlock(authCredentials, NO, nil);
                                                            }];
        
        [alertController addAction:loginAction];
        
        [blockSelf presentViewController:alertController animated:YES completion:nil];
    }];
    
    [MAS start:^(BOOL completed, NSError *error)
     {
         //
         // Stop activity indicator
         //
         [blockSelf.activityIndicatorView stopAnimating];
         
         //
         // Handle Error
         //
         if(error)
         {
             UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"MAS.start Error"
                                                                                      message:[error localizedDescription]
                                                                               preferredStyle:UIAlertControllerStyleAlert];
             UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                     style:UIAlertActionStyleDefault
                                                                   handler:nil];
             [alertController addAction:defaultAction];
             
             
             UIAlertAction *wipeAction = [UIAlertAction actionWithTitle:@"Wipe local cache"
                                                                  style:UIAlertActionStyleDefault
                                                                handler:^(UIAlertAction *action)
                                          {
                                              [[MASDevice currentDevice] resetLocally];
                                          }];
             
             [alertController addAction:wipeAction];
             
             [blockSelf presentViewController:alertController animated:YES completion:nil];
             
             return;
         }
         else if (completed)
         {
             UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"MAS.start Succeeded"
                                                                                      message:@"Your app is registered with your Gateway!!"
                                                                               preferredStyle:UIAlertControllerStyleAlert];
             
             UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                     style:UIAlertActionStyleDefault
                                                                   handler:nil];
             [alertController addAction:defaultAction];
             
             [blockSelf presentViewController:alertController animated:YES completion:nil];
         }
     }];
    
    [self updateAuthenticationStatus];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateAuthenticationStatus) name:MASDidStartNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateAuthenticationStatus) name:MASDeviceDidDeregisterNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateAuthenticationStatus) name:MASUserDidLogoutNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateAuthenticationStatus) name:MASUserDidAuthenticateNotification object:nil];
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

- (void)updateAuthenticationStatus
{
    switch ([MASApplication currentApplication].authenticationStatus) {
        case MASAuthenticationStatusLoginWithUser:
            self.authenticationStatusLabel.text = [NSString stringWithFormat:@"Authentication status: authenticated as %@", [MASUser currentUser].objectId];
            break;
        case MASAuthenticationStatusLoginAnonymously:
            self.authenticationStatusLabel.text = @"Authentication status: anonymously authenticated";
            break;
            
        default:
            self.authenticationStatusLabel.text = @"Authentication status: not authenticated";
            break;
    }
}


- (IBAction)explicitLogin:(id)sender
{
    //
    // block self
    //
    __block MASMainViewController *blockSelf = self;
    
    //
    // in main thread
    //
    dispatch_async (dispatch_get_main_queue(), ^{
        
        //
        // Create UIAletController
        //
        UIAlertController *alertController = [UIAlertController
                                              alertControllerWithTitle:@"Explicit User Login"
                                              message:@"Please enter username and password"
                                              preferredStyle:UIAlertControllerStyleAlert];
        
        //
        // Username field
        //
        [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
            textField.placeholder = NSLocalizedString(@"Username", @"Username");
        }];
        
        //
        // Password field
        //
        [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
            textField.placeholder = NSLocalizedString(@"Password", @"Password");
            textField.secureTextEntry = YES;
        }];
        
        //
        // Action button
        //
        UIAlertAction *loginAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK action")
                                                              style:UIAlertActionStyleDefault
                                                            handler:^(UIAlertAction *action) {
                                                                UITextField *username = alertController.textFields.firstObject;
                                                                UITextField *password = alertController.textFields.lastObject;
                                                                
                                                                //
                                                                // Explicit login
                                                                //
                                                                [MASUser loginWithUserName:username.text password:password.text completion:^(BOOL completed, NSError *error) {
                                                                    
                                                                    NSString *alertText = @"";
                                                                    
                                                                    if (error)
                                                                    {
                                                                        alertText = error.localizedDescription;
                                                                    }
                                                                    else {
                                                                        alertText = @"Login was successful";
                                                                    }
                                                                    
                                                                    //
                                                                    // Display the result as an alert
                                                                    //
                                                                    UIAlertController *loginResultAlertController = [UIAlertController alertControllerWithTitle:@"Login Result"
                                                                                                                                                        message:alertText
                                                                                                                                                 preferredStyle:UIAlertControllerStyleAlert];
                                                                    
                                                                    UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                                                                            style:UIAlertActionStyleDefault
                                                                                                                          handler:nil];
                                                                    [loginResultAlertController addAction:defaultAction];
                                                                    
                                                                    [blockSelf presentViewController:loginResultAlertController animated:YES completion:nil];
                                                                    [blockSelf updateAuthenticationStatus];
                                                                }];
                                                            }];
        
        [alertController addAction:loginAction];
        
        [blockSelf presentViewController:alertController animated:YES completion:nil];
    });
}


- (IBAction)invokeAPI:(id)sender
{
    //
    // Invoke protected endpoint
    //
    [MAS getFrom:@"/protected/resource/products" withParameters:@{@"operation":@"listProducts"} andHeaders:nil completion:^(NSDictionary *responseInfo, NSError *error) {
        
        __block id blockSelf = self;
        
        dispatch_async (dispatch_get_main_queue(), ^
                        {
                            NSString *alertText = @"";
                            
                            if (error)
                            {
                                alertText = error.localizedDescription;
                            }
                            else {
                                
                                alertText = @"Invoking API Successful";
                            }
                            
                            UIAlertController *alertControllerInvoke = [UIAlertController alertControllerWithTitle:@"MAS Invoke API"
                                                                                                           message:alertText
                                                                                                    preferredStyle:UIAlertControllerStyleAlert];
                            
                            UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                                    style:UIAlertActionStyleDefault
                                                                                  handler:nil];
                            [alertControllerInvoke addAction:defaultAction];
                            
                            [blockSelf presentViewController:alertControllerInvoke animated:YES completion:nil];
                            
                            DLog(@"endpoint result: %@", responseInfo);
                        });
    }];
}


- (IBAction)deregisterDevice:(id)sender
{
    //
    // De-register
    //
    if ([MASDevice currentDevice])
    {
        [[MASDevice currentDevice] deregisterWithCompletion:nil];
    }
}


@end
