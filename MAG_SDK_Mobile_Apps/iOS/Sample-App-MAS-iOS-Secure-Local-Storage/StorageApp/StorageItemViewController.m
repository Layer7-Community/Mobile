//
//  StorageItemViewController.m
//  StorageApp
//
//  Copyright (c) 2016 CA. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import "StorageItemViewController.h"
#import <MASStorage/MASStorage.h>

@interface StorageItemViewController ()

@property (nonatomic) MASObject *objectDetails;
@property (nonatomic, weak) IBOutlet UIButton *encryptDataBtn;
@property (nonatomic, weak) IBOutlet UILabel *resultText;
@property (nonatomic, weak) IBOutlet UIImageView *resultImage;

@end

@implementation StorageItemViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.navigationItem.title = [self.selectedItem objectForKey:@"key"];
    
    UIBarButtonItem *updateButton = [[UIBarButtonItem alloc] initWithTitle:@"Update" style:UIBarButtonItemStylePlain target:self action:@selector(updateDataToStorage)];
    
    self.navigationItem.rightBarButtonItem = updateButton;
    
    [self getStorageItemDetails];
    
    self.encryptDataBtn.hidden = YES; //self.isCloudStorageItem;
}

- (NSString *)payloadTypeAsString
{
    //
    // Confirm there is a payload
    //
    NSString *value = nil;
    
    if([self.objectDetails objectForKey:@"value"])
    {
        //
        // Check if the payload has already been converted to a string or needs
        // to be converted
        //
        if ([[self.objectDetails objectForKey:@"value"] isKindOfClass:[NSString class]]) {
            
            value = (NSString *)[self.objectDetails objectForKey:@"value"];
        }
        else {
            
            // Decode the payload
            value = [[NSString alloc] initWithData:[self.objectDetails objectForKey:@"value"] encoding:NSUTF8StringEncoding];
        }
    }
    
    return value;
}

- (UIImage *)payloadTypeAsImage
{
    UIImage *value = nil;

    //
    // Confirm there is a payload
    //
    if(self.objectDetails)
    {
        NSData *imageData = [self.objectDetails objectForKey:@"value"];
        
        //
        // Check if there is metadata inside the payload. If so, change the way of loading the image
        //
        NSString *base64String = [[NSString alloc] initWithData:[self.objectDetails objectForKey:@"value"] encoding:NSUTF8StringEncoding];
        
        if (base64String) {
            
            NSURL *imageUrl = [NSURL URLWithString:base64String];
            imageData = [NSData dataWithContentsOfURL:imageUrl];
        }
        
        UIImage *image= [UIImage imageWithData:imageData];
        
        if(image != nil){
            
            value = image;
        }
    }
    
    return value;
}


- (void)getStorageItemDetails
{
//    if (self.isCloudStorageItem) {
//
//        [MASCloudStorage findObjectUsingKey:[self.selectedItem objectForKey:@"key"] mode:self.mode completion:^(MASObject *object, NSError *error) {
//
//            self.objectDetails = object;
//            self.resultText.text = [self payloadTypeAsString];
//            self.resultImage.image = [self payloadTypeAsImage];
//        }];
//    }
//    else {
        
        [MASLocalStorage findObjectUsingKey:[self.selectedItem objectForKey:@"key"] mode:self.mode completion:^(MASObject *object, NSError *error) {

            self.objectDetails = object;
            self.resultText.text = [self payloadTypeAsString];
            self.resultImage.image = [self payloadTypeAsImage];

        }];
//    }
}

- (void)updateDataToStorage
{
//    if (self.isCloudStorageItem) {
//
//        NSString *newString = @"Testing Update";
//
//        //Update Cloud Storage Item
//        [MASCloudStorage saveObject:newString withKey:[self.objectDetails objectForKey:@"key"] type:@"text/plain" mode:self.mode completion:^(BOOL success, NSError *error) {
//
//            if (!error) {
//
//                UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Alert"
//                                                                               message:@"CloudStorageItem updated Successfully"
//                                                                        preferredStyle:UIAlertControllerStyleAlert];
//
//                UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
//                                                                        style:UIAlertActionStyleDefault
//                                                                      handler:^(UIAlertAction * action) {}];
//
//                [alert addAction:defaultAction];
//
//                [self presentViewController:alert animated:YES completion:nil];
//            }
//            else {
//
//                UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Failure"
//                                                                               message:[error localizedDescription]
//                                                                        preferredStyle:UIAlertControllerStyleAlert];
//
//                UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
//                                                                        style:UIAlertActionStyleDefault
//                                                                      handler:^(UIAlertAction * action) {}];
//
//                [alert addAction:defaultAction];
//
//                [self presentViewController:alert animated:YES completion:nil];
//            }
//        }];
//    }
//    else {
        
        NSString *newString = @"Testing Update";
        
        //Update Local Storage Item
        [MASLocalStorage saveObject:newString withKey:[self.objectDetails objectForKey:@"key"] type:[self.objectDetails objectForKey:@"type"] mode:self.mode completion:^(BOOL success, NSError *error) {
            
            if (!error) {
                
                UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Alert"
                                                                               message:@"LocalStorageItem updated Successfully"
                                                                        preferredStyle:UIAlertControllerStyleAlert];
                
                UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                        style:UIAlertActionStyleDefault
                                                                      handler:^(UIAlertAction * action) {}];
                
                [alert addAction:defaultAction];
                
                [self presentViewController:alert animated:YES completion:nil];
            }
            else {
                
                UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Failure"
                                                                               message:[error localizedDescription]
                                                                        preferredStyle:UIAlertControllerStyleAlert];
                
                UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
                                                                        style:UIAlertActionStyleDefault
                                                                      handler:^(UIAlertAction * action) {}];
                
                [alert addAction:defaultAction];
                
                [self presentViewController:alert animated:YES completion:nil];
            }

        }];
//    }
}


- (IBAction)encryptData:(id)sender
{
    NSString *myString = @"Encrypt this message!";
    
    [MASLocalStorage saveObject:myString withKey:@"contact" type:@"string" mode:0 password:@"BigMikePwd!" completion:^(BOOL success, NSError *error) {
       
        NSLog(@"Success");
    }];
}

@end
