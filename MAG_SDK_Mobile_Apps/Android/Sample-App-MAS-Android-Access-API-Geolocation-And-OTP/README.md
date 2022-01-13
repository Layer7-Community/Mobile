# Access API Sample App

**Required:**
* Latest version of Android Studio 
* Device with Passcode and/or Fingerprint locks enabled

## Get Started
1. In Android Studio, open the project 'MASAccessAPISample'.
2. In the CA OAuth Manager, create an app, and export the msso_config (https://you_server_name:8443/oauth/manager). For more info, see [Android Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-2.html).
3. Copy the contents of the exported msso_config to: src/main/assets/msso_config.json.
4. Import /utils/tradePolicy.xml into your policy manager as an endpoint with the resolution path '/trade'.
5. Build and Deploy the app to a device.
