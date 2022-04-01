//
//  AppDelegate.h
//  MasFingerprintSample
//
//  Created by Woods, Brendan on 2016-11-03.
//  Copyright © 2016 Ca Technologies. All rights reserved.
//

#import <UIKit/UIKit.h>

/**
 * The UIApplicationDelegate protocol defines methods that are called by the singleton UIApplication
 * object in response to important events in the lifetime of your app.
 *
 * The app delegate works alongside the app object to ensure your app interacts properly with the
 * system and with other apps. Specifically, the methods of the app delegate give you a chance to
 * respond to important changes. For example, you use the methods of the app delegate to respond to
 * state transitions, such as when your app moves from foreground to background execution, and to
 * respond to incoming notifications. In many cases, the methods of the app delegate are the only
 * way to receive these important notifications.
 */


@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

@property BOOL lockSessionDuringBackground;

@end

