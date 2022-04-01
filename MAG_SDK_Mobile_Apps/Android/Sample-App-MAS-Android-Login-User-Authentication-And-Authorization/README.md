# Login User Authentication and Authorization App

**Required:**
* Latest version of Android Studio
* Device with Passcode and/or Fingerprint locks enabled

## Get Started
1. In Android Studio, open the project 'MASLoginUserAuthentication'.
2. In the CA OAuth Manager, create an app, and export the msso_config file (https://you_server_name:8443/oauth/manager). For help with this file, see [Android Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-2.html).
3. Copy the contents of the exported msso_config to src/main/assets/msso_config.json.
4. In Policy Manager, import the policy retrieveFlights.xml as /retrieveFlights  
a. Publish Web API -> Enter Service name as retrieveFlights and Gateway URL as retrieveFlights -> Finish  
b. File -> Import Policy (into your newly created api) -> Select retrieveFlights.xml
5. Build and Deploy the app to a device.
6. Log in to app with a valid user and it will retreive the flight info from the database as a json
