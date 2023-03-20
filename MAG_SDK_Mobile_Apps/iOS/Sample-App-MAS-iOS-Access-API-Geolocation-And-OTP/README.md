# Geolocation and One-Time Password Sample App
Back to > [iOS Mobile SDK](https://github.com/CAAPIM/iOS-MAS-SDK)
<hr/>
This sample app uses the MASFoundation and MASUI frameworks of the MAS SDK.

<br>**Required:**
* Xcode, latest version installed from App Store
* Cocoapods, latest version installed from https://cocoapods.org/</br>

## Get Started
1. Open a terminal window to the top level folder of this Sample App (ie: ~/iOS - Access API with Geolocation and One-Time Password)
2. In the terminal type: `pod update`   
   If this fails try: `pod install`
3. Open the .xcworkspace (ie: MASStockTrading.xcworkspace).
4. In the CA OAuth Manager, create an app, and export the msso_config file (https://you_server_name:8443/oauth/manager). For help with this file, see [iOS Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-3.html).
5. Copy the contents of the exported msso_config into the msso_config file in the Xcode workspace.
6. In Policy Manager, import the policy tradePolicy.xml as /trade  
a. Publish Web API -> Enter Service name as trade and Gateway URL as trade -> Finish  
b. File -> Import Policy (into your newly created api) -> Select tradePolicy.xml
7. Build and Deploy the app to a device or Simulator.
8. To trigger Login, add any stock code and add in a value for stocks.
9. To trigger OTP code, add stock value over 10,000 shares.
