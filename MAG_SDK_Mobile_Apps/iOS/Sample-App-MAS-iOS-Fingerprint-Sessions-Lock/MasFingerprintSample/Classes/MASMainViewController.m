//
//  MASMainViewController.m
//  MasFingerprintSample
//
//  Created by Woods, Brendan on 2016-11-03.
//  Copyright © 2016 Ca Technologies. All rights reserved.
//

#import "MASMainViewController.h"
#import <MASFoundation/MASFoundation.h>
#import <MASUI/MASUI.h>

@interface MASMainViewController ()

#pragma mark - Properties

@property (nonatomic, weak) IBOutlet UIActivityIndicatorView *activityIndicatorView;

@property (weak, nonatomic) IBOutlet UIButton *loginButton;

@end

@implementation MASMainViewController


# pragma mark - Lifecycle


- (void)viewDidLoad
{
    [super viewDidLoad];
    
    //
    // Start activity indicator
    //
    self.activityIndicatorView.hidesWhenStopped = YES;
    [self.activityIndicatorView startAnimating];
    
    
    //[MAS setGatewayNetworkActivityLogging:true];
    
    //
    // Set the grant flow to password
    //
    [MAS setGrantFlow:MASGrantFlowPassword];
    
    //
    // Begin the MAS framework
    //
    __block MASMainViewController *blockSelf = self;
    [MAS startWithDefaultConfiguration:YES completion:^(BOOL completed, NSError *error)
     {
         //
         // Stop activity indicator
         //
         [blockSelf.activityIndicatorView stopAnimating];
         
         //
         // Handle any errors that may occur
         //
         if(error)
         {
             dispatch_async (dispatch_get_main_queue(), ^
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
                             });
         }
         
         //
         // Success
         //
         if (completed && [MASUser currentUser] && [MASUser currentUser].isAuthenticated)
         {
             [self performSegueWithIdentifier:@"loginSegue" sender:self];
         }
     }];
}


- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:YES];
    
    //
    // Check if session is locked. Display lock screen if so
    //
    if ([[MASUser currentUser] isSessionLocked])
    {
        [MASUser presentSessionLockScreenViewController:^(BOOL completed, NSError *error) {
            
            if (error)
            {
                NSLog(@"ERROR:%@",[error localizedDescription]);
            }
        }];
    }
    else if ([[MASUser currentUser] isAuthenticated])
    {
        [self performSegueWithIdentifier:@"loginSegue" sender:self];
    }
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    
    NSLog(@"didReceiveMemoryWarning called");
}


# pragma mark - IBActions


/**
 * Attempt to login the user. If the user is already authenticated, proceed to next scene.
 * If the session is locked, display the lock screen.
 *
 * @param sender the UIButton that was touched
 */
- (IBAction)loginButtonTouched:(UIButton *)sender
{
    //
    // If user is already authenticated, proceed
    //
    if([[MASUser currentUser]isAuthenticated])
    {
        [self performSegueWithIdentifier:@"loginSegue" sender:self];
    }
    
    //
    // If the session is currently locked, present the lock screen
    //
    if ([[MASUser currentUser] isSessionLocked])
    {
        [MASUser presentSessionLockScreenViewController:^(BOOL completed, NSError *error) {
            
            //
            // Log any errors that may occur
            //
            if (error)
            {
                NSLog(@"ERROR:%@",[error localizedDescription]);
                return;
            }
            
            //
            // Success, proceed
            //
            if (completed)
            {
            }
        }];
    }
    
    //
    // User is not authenticated, take the user to a login screen - custom or default
    //
    else
    {
        [MASUser presentLoginViewControllerWithCompletion:^(BOOL completed, NSError *error) {
            
            //
            // Log any errors that happen to occur
            //
            if(error)
            {
                NSLog(@"ERROR:%@",[error localizedDescription]);
            }
            
            //
            // Successfully logged in, proceed
            //
            if(completed)
            {
                [self performSegueWithIdentifier:@"loginSegue" sender:self];
            }
        }];
    }
}


# pragma mark - Private


- (UIStatusBarStyle)preferredStatusBarStyle
{
    //
    // Just making the status bar light so it can be seen against the
    // darker background
    //
    return UIStatusBarStyleLightContent;
}

@end
