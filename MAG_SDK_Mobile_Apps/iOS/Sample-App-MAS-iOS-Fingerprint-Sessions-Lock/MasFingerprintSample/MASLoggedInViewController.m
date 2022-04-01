//
//  MASLoggedInViewController.m
//  MasFingerprintSample
//
//  Created by Woods, Brendan on 2016-11-03.
//  Copyright © 2016 Ca Technologies. All rights reserved.
//

#import "MASLoggedInViewController.h"
#import "AppDelegate.h"

@interface MASLoggedInViewController ()

# pragma mark - Properties

@property (strong,nonatomic) NSMutableArray *responseProducts;

@property (weak, nonatomic) IBOutlet UITableView *responseInfoTableView;

@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *activityIndicatorView;

@end

@implementation MASLoggedInViewController


# pragma mark - Lifecycle


- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self getProtectedResponseInfo];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}


# pragma mark - IBActions


/**
 * Attempt to logout the user and return to main screen
 *
 * @param sender the UIButton that was touched
 */
- (IBAction)logoutButtonTouched:(UIButton *)sender
{
    //
    // Start the activity indicator
    //
    self.activityIndicatorView.hidden = NO;
    [self.activityIndicatorView startAnimating];
    
    //
    // Attempt to logout the user
    //
    [[MASUser currentUser] logout:NO completion:^(BOOL completed, NSError *error) {
        
        //
        // Stop the activity indicator view
        //
        self.activityIndicatorView.hidden = YES;
        [self.activityIndicatorView stopAnimating];
        
        //
        // Log any errors that may occur
        //
        if (error)
        {
            NSLog(@"ERROR:%@",[error localizedDescription]);
        }
        
        //
        // Success, return to root scene.
        //
        if (completed)
        {
            [self.navigationController popViewControllerAnimated:YES];
        }
    }];
}


/**
 * Attempt to lock the current session and display lock screen
 *
 * @param sender the UIButton that was touched
 */
- (IBAction)lockButtonTouched:(UIButton *)sender
{
    //
    // Attempt to lock the current session
    //
    [[MASUser currentUser] lockSessionWithCompletion:^(BOOL completed, NSError *error) {
        
        //
        // Log any errors that may have occured
        //
        if (error)
        {
            NSLog(@"ERROR:%@",[error localizedDescription]);
            return;
        }
        
        //
        // Success
        //
        if (completed)
        {
            //
            // Present the lock screen - custom or default.
            //
            [MASUser presentSessionLockScreenViewController:^(BOOL completed, NSError *error) {
                
                //
                // Log any errors that may have occured
                //
                if (error)
                {
                    NSLog(@"ERROR:%@",[error localizedDescription]);
                }
            }];
        }
    }];
}


/**
 * Used to toggle the background locking feature on and off
 *
 * @param sender the UISegmentedControl
 */
- (IBAction)lockOnBackGroundSegmentedControlChanged:(UISegmentedControl *)sender
{
    AppDelegate *sharedDelegate = (AppDelegate *)[[UIApplication sharedApplication]delegate];
    
    
    switch (sender.selectedSegmentIndex) {
            //
            // Turn on the background lock
            //
        case 0:
            sharedDelegate.lockSessionDuringBackground = YES;
            break;
            
            //
            // Turn off the background lock
            //
        case 1:
            sharedDelegate.lockSessionDuringBackground = NO;
            break;
            
    }
}

# pragma mark - Private


/**
 * Overide the property setter to update UI
 *
 * @param responseProducts the response products
 */
- (void)setResponseProducts:(NSMutableArray *)responseProducts
{
    //
    // Whenever we update the products, reload the UI
    //
    _responseProducts = responseProducts;
    
    [self updateUI];
}


/**
 * Attempt to hit the protected endpoint, and receive the proteced data response to prove authentication
 */
- (void)getProtectedResponseInfo
{
    //
    // Start the activity indicator
    //
    self.activityIndicatorView.hidden = NO;
    [self.activityIndicatorView startAnimating];
    
    //
    // Attempt to access protected endpoint. If successful repsonse is recieved, display the protected data.
    //
    [MAS getFrom:@"/protected/resource/products"
  withParameters:@{@"operation" : @"listProducts"}
      andHeaders:nil
      completion:^(NSDictionary *responseInfo, NSError *error) {
          
          //
          // Stop the activity indicator
          //
          self.activityIndicatorView.hidden = YES;
          [self.activityIndicatorView stopAnimating];
          
          //
          // Log any errors if they happen to occur.
          //
          if(error)
          {
              NSLog(@"ERROR:%@", [error localizedDescription]);
              return;
          }
          
          //
          // Success. Parse and update the protected data.
          //
          if(responseInfo)
          {
              NSMutableArray *responseItemsArray = [[NSMutableArray alloc]init];
              
              NSDictionary *responseBody = [responseInfo objectForKey:@"MASResponseInfoBodyInfoKey"];
              
              NSArray *products = [responseBody objectForKey:@"products"];
              
              for(NSDictionary *productDict in products)
              {
                  [responseItemsArray addObject:[productDict objectForKey:@"name"]];
              }
              
              self.responseProducts = responseItemsArray;
          }
      }];
}


- (void)updateUI
{
    [self.responseInfoTableView reloadData];
}


- (UIStatusBarStyle)preferredStatusBarStyle
{
    //
    // Just making the status bar light so it can be seen against the
    // darker background
    //
    return UIStatusBarStyleLightContent;
}

# pragma mark - UITableViewDelegate


-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    //
    // Create the cell with the proper title
    //
    UITableViewCell *cell = [_responseInfoTableView dequeueReusableCellWithIdentifier:@"cell"];
    
    cell.textLabel.text = [self.responseProducts objectAtIndex:indexPath.row];
    
    return cell;
}


# pragma mark - UITableViewDataSource


- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.responseProducts count];
}


- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    return @"Protected Data";
}

@end
