//
//  StorageTableViewController.m
//  StorageApp
//
//  Copyright (c) 2016 CA. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import "StorageTableViewController.h"
#import "StorageItemViewController.h"
#import <MASFoundation/MASFoundation.h>
#import <MASStorage/MASStorage.h>

static NSString *SampleUser = @"YOU USERNAME";
static NSString *SampleUserPassword = @"YOU PASSWORD";

//Action Sheet
static const NSInteger ADD_SHEET = 10;
static const NSInteger ADD_TEXT_SHEET = 0;
static const NSInteger ADD_IMAGE_SHEET = 1;
static const NSInteger ADD_DATA_SHEET = 2;
static const NSInteger ADD_OVERIMAGE_SHEET = 3;
static const NSInteger ADD_INVALID_DATA_SHEET = 4;

@interface StorageTableViewController ()

@property (nonatomic) NSMutableArray *storageItems;
@property (nonatomic) UILabel *messageLabel;
@property (nonatomic) UIBarButtonItem *storageButton;
@property (nonatomic, weak) IBOutlet UISegmentedControl *segment;

@end

@implementation StorageTableViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.navigationItem.title = @"Cloud Storage";

    UIBarButtonItem *addButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAction target:self action:@selector(handleAddButtonTapped)];

    self.navigationItem.rightBarButtonItem = addButton;

    self.storageButton = [[UIBarButtonItem alloc] initWithTitle:@"Local" style:UIBarButtonItemStylePlain target:self action:@selector(switchStorage)];

    self.navigationItem.leftBarButtonItem = self.storageButton;
    
    self.storageItems = [NSMutableArray array];

    
    // Initialize the refresh control.
    self.refreshControl = [[UIRefreshControl alloc] init];
    self.refreshControl.backgroundColor = [UIColor lightGrayColor];
    self.refreshControl.tintColor = [UIColor whiteColor];
    [self.refreshControl addTarget:self
                            action:@selector(refreshStorageList)
                  forControlEvents:UIControlEventValueChanged];
    
    
    //
    //Start the SDK
    //
    __block typeof(self) blockSelf = self;
    [MAS start:^(BOOL completion, NSError *error) {
        
        if (completion) {
            
            //
            //Registering the Device to a specific user
            //
            [MASUser loginWithUserName:SampleUser password:SampleUserPassword completion:^(BOOL completed, NSError *error) {
                
                [blockSelf refreshStorageList];
            }];
            
        }
        else {
            
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
        }
    }];

    
}


- (void)viewWillAppear:(BOOL)animated
{
    [self.tableView deselectRowAtIndexPath:[self.tableView indexPathForSelectedRow] animated:YES];
}

- (void)viewDidAppear:(BOOL)animated {

    if ([self.storageButton.title isEqualToString:@"Local"]) {

        [self switchStorage];

        self.navigationItem.leftBarButtonItem.enabled = NO;
        self.navigationItem.leftBarButtonItem = nil;
    }
}


#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    if ([self.storageItems count] > 0) {
        
        self.tableView.separatorStyle = UITableViewCellSeparatorStyleSingleLine;
        return 1;
        
    } else {
        
        // Display a message when the table is empty
        self.messageLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, self.view.bounds.size.height)];
        
        self.messageLabel.text = @"No data is currently available. Please pull down to refresh.";
        self.messageLabel.textColor = [UIColor blackColor];
        self.messageLabel.numberOfLines = 0;
        self.messageLabel.textAlignment = NSTextAlignmentCenter;
        self.messageLabel.font = [UIFont fontWithName:@"Palatino-Italic" size:20];
        [self.messageLabel sizeToFit];
        [self.messageLabel setHidden:NO];
        self.tableView.backgroundView = self.messageLabel;
        self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
        
    }
    
    return 0;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.storageItems count];
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *cellIdentifier = @"STORAGE_IDENTIFIER";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentifier forIndexPath:indexPath];
    
    if (!cell) {
        
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:cellIdentifier];
    }
    
    MASObject *storageItem = [self.storageItems objectAtIndex:indexPath.row];
    cell.textLabel.text = [storageItem objectForKey:@"key"];
    cell.detailTextLabel.text = [storageItem objectForKey:@"modifiedDate"];
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    return cell;
}


// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    return YES;
}


// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        
//        if ([self.storageButton.title isEqualToString:@"Local"]) {
//            [MASCloudStorage deleteObjectUsingKey:[[self.storageItems objectAtIndex:indexPath.row] objectForKey:@"key"] mode:self.segment.selectedSegmentIndex completion:^(BOOL success, NSError *error) {
//
//                if (!error) {
//
//                    [self.storageItems removeObjectAtIndex:indexPath.row];
//
//                    if ([self.storageItems count] == 0) {
//
//                        [tableView reloadData];
//                    }
//                    else {
//
//                        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
//                    }
//                }
//                else {
//
//                    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Failure"
//                                                                                   message:[error localizedDescription]
//                                                                            preferredStyle:UIAlertControllerStyleAlert];
//
//                    UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
//                                                                            style:UIAlertActionStyleDefault
//                                                                          handler:^(UIAlertAction * action) {}];
//
//                    [alert addAction:defaultAction];
//
//                    [self presentViewController:alert animated:YES completion:nil];
//                }
//
//            }];
//        }
//        else {

            [MASLocalStorage deleteObjectUsingKey:[[self.storageItems objectAtIndex:indexPath.row] objectForKey:@"key"] mode:self.segment.selectedSegmentIndex completion:^(BOOL success, NSError *error) {
                
                if (!error) {
                    
                    [self.storageItems removeObjectAtIndex:indexPath.row];
                    
                    if ([self.storageItems count] == 0) {
                        
                        [tableView reloadData];
                    }
                    else {
                        
                        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
                    }
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
        //}
    } else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }
}

#pragma mark - UITableView delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [self performSegueWithIdentifier:@"StorageItemSegue" sender:self];
}


#pragma mark - Navigation

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    NSIndexPath *index = [self.tableView indexPathForSelectedRow];
    StorageItemViewController *storageItemViewController = [segue destinationViewController];
    storageItemViewController.selectedItem = self.storageItems[index.row];
    storageItemViewController.mode = self.segment.selectedSegmentIndex;
    
    if ([self.storageButton.title isEqualToString:@"Local"]) {
        
        storageItemViewController.isCloudStorageItem = YES;
    }
    else {
        
        storageItemViewController.isCloudStorageItem = NO;
    }
}


#pragma mark - Methods

- (void)switchStorage
{
    if ([self.storageButton.title isEqualToString:@"Local"]) {

        [self.storageButton setTitle:@"Cloud"];
        
        [self.navigationItem setTitle:@"Local Storage"];
        
        [self.segment removeAllSegments];
        
        [self.segment insertSegmentWithTitle:@"App Segment" atIndex:0 animated:YES];
        [self.segment insertSegmentWithTitle:@"AppUser Seg." atIndex:1 animated:YES];
        
        [self.segment setSelectedSegmentIndex:0];
    }
    else {
        
        [self.storageButton setTitle:@"Local"];
        
        [self.navigationItem setTitle:@"Cloud Storage"];
        
        [self.segment insertSegmentWithTitle:@"User Segment" atIndex:0 animated:YES];
        
        [self.segment setSelectedSegmentIndex:1];

    }
    
    [self refreshStorageList];
}

- (IBAction)segmentedControlIndexChanged
{
    [self refreshStorageList];
}


#pragma mark - ADD method

- (void)handleAddButtonTapped
{
    UIActionSheet *addActionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:self cancelButtonTitle:@"cancel" destructiveButtonTitle:nil otherButtonTitles:@"Add data of type String",@"Add data of type Image",nil];
    
    addActionSheet.tag = ADD_SHEET;
    
    [addActionSheet showInView:self.view];
    
}

- (void)addDataToStorage:(NSUInteger)type
{
//    if ([self.storageButton.title isEqualToString:@"Local"]) {
//
//        [self addDataToCloudStorage:type];
//    }
//    else {
        
        [self addDataToLocalStorage:type];
//    }
}


- (void)addDataToLocalStorage:(NSUInteger)type
{
    __weak typeof(self) weakSelf = self;
    
    NSString *newString = [NSString stringWithFormat:@"NewLocalStorage%d",(int)[self.storageItems count] +1];
    NSObject *messageData;
    NSString *messageType;
    
    switch (type) {
            
        case ADD_TEXT_SHEET:
        {
            messageData = @"This is a TEXT message";
            messageType = @"text/plain";
            break;
        }
            
        case ADD_IMAGE_SHEET:
        {
            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"butters" ofType:@"png"];
            messageData = [NSData dataWithContentsOfFile:filePath];
            messageType = @"image/jpg";
            break;
        }
            
        case ADD_DATA_SHEET:
        {
            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"image" ofType:@"jpg"];
            messageData = [NSData dataWithContentsOfFile:filePath];
            messageType = @"application/octet-stream";
            break;
        }
            
        case ADD_OVERIMAGE_SHEET:
        {
            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"oversize" ofType:@"jpg"];
            messageData = [NSData dataWithContentsOfFile:filePath];
            messageType = @"image/png";
            break;
        }

        case ADD_INVALID_DATA_SHEET:
        {
            UIImage *myImage = [UIImage imageNamed:@"image.jpg"];
            messageData = myImage;
            messageType = @"image/png";
            break;
        }
    }
    
    [MASLocalStorage saveObject:messageData withKey:newString type:messageType mode:self.segment.selectedSegmentIndex completion:^(BOOL success, NSError *error) {
        
        if (!error) {
            
            [weakSelf refreshStorageList];
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
}


//- (void)addDataToCloudStorage:(NSUInteger)type
//{
//    __weak typeof(self) weakSelf = self;
//
//    NSString *newString = [NSString stringWithFormat:@"NewCloudStorage%d",(int)[self.storageItems count] +1];
//    NSObject *messageData;
//    NSString *messageType;
//
//    switch (type) {
//
//        case ADD_TEXT_SHEET:
//        {
//            messageData = @"This is a TEXT message";
//            messageType = @"text/plain";
//            break;
//        }
//
//        case ADD_IMAGE_SHEET:
//        {
//            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"butters" ofType:@"png"];
//            messageData = [NSData dataWithContentsOfFile:filePath];
//            messageType = @"image/png";
//            break;
//        }
//
//        case ADD_DATA_SHEET:
//        {
//            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"image" ofType:@"jpg"];
//            messageData = [NSData dataWithContentsOfFile:filePath];
//            messageType = @"application/octet-stream";
//            break;
//        }
//
//        case ADD_OVERIMAGE_SHEET:
//        {
//            NSString* filePath = [[NSBundle mainBundle] pathForResource:@"oversize" ofType:@"jpg"];
//            messageData = [NSData dataWithContentsOfFile:filePath];
//            messageType = @"image/png";
//            break;
//        }
//
//        case ADD_INVALID_DATA_SHEET:
//        {
//            UIImage *myImage = [UIImage imageNamed:@"image.jpg"];
//            messageData = myImage;
//            messageType = @"image/png";
//            break;
//        }
//    }
//
//    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
//
//    [MASCloudStorage saveObject:messageData withKey:newString type:messageType mode:self.segment.selectedSegmentIndex completion:^(BOOL success, NSError *error) {
//
//        if (!error) {
//
//            [weakSelf refreshStorageList];
//        }
//        else {
//            NSString *errorMessage = [NSString stringWithFormat:@"%@ : %@",[error localizedDescription],[error userInfo][MASResponseInfoBodyInfoKey]];
//
//            UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Failure"
//                                                                           message:errorMessage                                                                    preferredStyle:UIAlertControllerStyleAlert];
//
//            UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
//                                                                    style:UIAlertActionStyleDefault
//                                                                  handler:^(UIAlertAction * action) {}];
//
//            [alert addAction:defaultAction];
//
//            [self presentViewController:alert animated:YES completion:nil];
//        }
//
//        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
//    }];
//}


#pragma mark - GET methods

- (void)refreshStorageList
{
//    if ([self.storageButton.title isEqualToString:@"Local"]) {
//
//        [self getDataFromCloudStorageMode:self.segment.selectedSegmentIndex];
//    }
//    else {
        
        [self getDataFromLocalStorageMode:self.segment.selectedSegmentIndex];
    //}
}


- (void)getDataFromLocalStorageMode:(NSInteger)mode
{
    __weak typeof(self) weakSelf = self;
    
    __block NSMutableArray *newStorageList;
    
    newStorageList = [[NSMutableArray alloc] init];

    
    //
    //List Local Storage Data
    //
    [MASLocalStorage findObjectsUsingMode:mode completion:^(NSArray *objects, NSError *error) {
        
        if (!error) {
            
            if ([objects count] > 0) {
                
                newStorageList = [[NSMutableArray alloc] initWithArray:objects];
                
                [weakSelf.messageLabel setHidden:YES];

            }
            else {
                
                newStorageList = [[NSMutableArray alloc] init];
            }
            
            
            // End the refreshing
            if (weakSelf.refreshControl) {
                
                NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
                [formatter setDateFormat:@"MMM d, h:mm a"];
                NSString *title = [NSString stringWithFormat:@"Last update: %@", [formatter stringFromDate:[NSDate date]]];
                NSDictionary *attrsDictionary = [NSDictionary dictionaryWithObject:[UIColor whiteColor]
                                                                            forKey:NSForegroundColorAttributeName];
                NSAttributedString *attributedTitle = [[NSAttributedString alloc] initWithString:title attributes:attrsDictionary];
                weakSelf.refreshControl.attributedTitle = attributedTitle;
            }
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

        [weakSelf setStorageItems:newStorageList];
        
        [weakSelf.tableView reloadData];
        
        [weakSelf.refreshControl endRefreshing];
    }];
}


//- (void)getDataFromCloudStorageMode:(NSInteger)mode
//{
//    __weak typeof(self) weakSelf = self;
//
//    __block NSMutableArray *newStorageList;
//
//     newStorageList = [[NSMutableArray alloc] init];
//
//    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible: YES];
//
//
//    //
//    //List Cloud Storage Data
//    //
//    [MASCloudStorage findObjectsUsingMode:mode completion:^(NSArray *objects, NSError *error) {
//
//        if (!error) {
//
//            if ([objects count] > 0) {
//
//                newStorageList = [[NSMutableArray alloc] initWithArray:objects];
//
//                [weakSelf.messageLabel setHidden:YES];
//            }
//            else {
//
//                newStorageList = [[NSMutableArray alloc] init];
//            }
//
//
//            // End the refreshing
//            if (weakSelf.refreshControl) {
//
//                NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
//                [formatter setDateFormat:@"MMM d, h:mm a"];
//                NSString *title = [NSString stringWithFormat:@"Last update: %@", [formatter stringFromDate:[NSDate date]]];
//                NSDictionary *attrsDictionary = [NSDictionary dictionaryWithObject:[UIColor whiteColor]
//                                                                            forKey:NSForegroundColorAttributeName];
//                NSAttributedString *attributedTitle = [[NSAttributedString alloc] initWithString:title attributes:attrsDictionary];
//                weakSelf.refreshControl.attributedTitle = attributedTitle;
//            }
//        }
//        else {
//
//            UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Failure"
//                                                                           message:[error localizedDescription]
//                                                                    preferredStyle:UIAlertControllerStyleAlert];
//
//            UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK"
//                                                                    style:UIAlertActionStyleDefault
//                                                                  handler:^(UIAlertAction * action) {}];
//
//            [alert addAction:defaultAction];
//
//            [self presentViewController:alert animated:YES completion:nil];
//        }
//
//        [weakSelf.refreshControl endRefreshing];
//
//        [weakSelf setStorageItems:newStorageList];
//
//        [weakSelf.tableView reloadData];
//
//        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible: NO];
//
//    }];
//}


#pragma mark - UIActionSheet delegate

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
    [self.tableView deselectRowAtIndexPath:[self.tableView indexPathForSelectedRow] animated:YES];
    
    if (buttonIndex == actionSheet.cancelButtonIndex) {
        return;
    }
    
    if (actionSheet.tag == ADD_SHEET) {
        
        [self addDataToStorage:buttonIndex];
    }
}

//- (void)listCloudObjects
//{
//    [MASCloudStorage findObjectsUsingMode:MASCloudStorageSegmentApplication completion:^(NSArray *objects, NSError *error) {
//      [MASCloudStorage findObjectUsingKey:@"key" mode:MASCloudStorageSegmentApplication completion:^(MASObject *object, NSError *error) {
//
//      }];
//    }];
//
//    [MASLocalStorage findObjectsUsingMode:MASLocalStorageSegmentApplication completion:^(NSArray *objects, NSError *error) {
//
//    }];
//}

- (void)saveObjectToCloud
{
    NSString *myString = @"Hello World";

    UIImage *myImage = [UIImage imageNamed:@"someImageName"];
    NSData *myData = UIImagePNGRepresentation(myImage);

    [MASLocalStorage saveObject:myData withKey:@"someKey" type:@"image/jpg" mode:MASLocalStorageSegmentApplication completion:^(BOOL success, NSError *error) {
        
    }];
}


@end
