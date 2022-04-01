//
//  AppDelegate.m
//  StorageApp
//
//  Copyright (c) 2016 CA. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import "AppDelegate.h"
#import <MASFoundation/MASFoundation.h>
#import <MASStorage/MASStorage.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    //
    //Monitory Network Activity
    //
    [MAS setGatewayNetworkActivityLogging:YES];
    

    //
    //Enable Local Storage
    //
    [MAS enableLocalStorage];
    
    return YES;

}


- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options
{
    //
    //  Handle incoming authorization code response from SFSafariViewController
    //
    [[MASAuthorizationResponse sharedInstance] application:app openURL:url options:options];
    return YES;
}

@end
