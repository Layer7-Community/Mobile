//
//  StorageItemViewController.h
//  StorageApp
//
//  Copyright (c) 2016 CA. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import <UIKit/UIKit.h>
#import <MASFoundation/MASFoundation.h>

@interface StorageItemViewController : UIViewController

@property (nonatomic) MASObject *selectedItem;
@property (nonatomic) BOOL isCloudStorageItem;
@property (nonatomic) NSInteger mode;
@end
