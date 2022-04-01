# Fingerprint Sessions Lock Sample App
Back to > [iOS Mobile SDK](https://github.com/CAAPIM/iOS-MAS-SDK)
<hr/>
This sample app uses the MASFoundation and MASUI frameworks of the MAS SDK.

<br>**Required:**
* Xcode, latest version installed from App Store
* CocoaPods, latest version installed from https://cocoapods.org/
* Device with Passcode and Fingerprint locks enabled
* Device required for fingerprint</br>

## Get Started
1. Open a terminal window to the top level folder of this Sample App (ie: ~/iOS - Fingerprint Sessions Lock).
2. In Terminal type: `pod update`    
   If this fails try: `pod install`
3. Open the .xcworkspace (ie: MasFingerprintSample.xcworkspace).
4. In the CA OAuth Manager, create an app, and export the msso_config file (https://you_server_name:8443/oauth/manager). For help with this file, see [iOS Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-2.html).
5. Copy the contents of the exported msso_config into the msso_config file in Xcode workspace.
6. Build and Deploy the app to a device.
