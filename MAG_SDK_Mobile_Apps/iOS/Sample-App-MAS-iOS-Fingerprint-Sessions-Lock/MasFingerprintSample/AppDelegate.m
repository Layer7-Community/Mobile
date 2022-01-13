//
//  AppDelegate.m
//  MasFingerprintSample
//
//  Created by Woods, Brendan on 2016-11-03.
//  Copyright © 2016 Ca Technologies. All rights reserved.
//

#import "AppDelegate.h"
#import <MASUI/MASUI.h>

@interface AppDelegate ()

@end

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    self.lockSessionDuringBackground = YES;
    
    return YES;
}


- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    
    
    //
    // If the lockSessionDuringBackground flag is on, and the current user is authenticated, lock the session before backgrounding the application
    //
    if(self.lockSessionDuringBackground == YES && [MASUser currentUser].isAuthenticated)
    {
        [[MASUser currentUser] lockSessionWithCompletion:^(BOOL completed, NSError *error) {
            
            if (error)
            {
                NSLog(@"ERROR:%@",[error localizedDescription]);
            }
        }];
    }
    
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    
    if (self.lockSessionDuringBackground == YES && [MASUser currentUser].isSessionLocked)
    {
        
        dispatch_async(dispatch_get_main_queue(), ^{
            
            [MASUser presentSessionLockScreenViewController:^(BOOL completed, NSError *error) {
                
                if (error)
                {
                    NSLog(@"ERROR:%@",[error localizedDescription]);
                }
                
            }];
        });
    }
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


@end
