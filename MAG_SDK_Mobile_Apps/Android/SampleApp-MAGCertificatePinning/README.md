# SampleApp-MAGCertificatePinning

**Required:**
* Latest version of Android Studio

## Get Started
1. In Android Studio, open the project 'SampleApp-MAGCertificatePinning'.
2. In the CA OAuth Manager, create an app, and export the msso_config file (https://you_server_name:8443/oauth/manager). For help with this file, see [Android Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-2.html).
3. Copy the contents of the exported msso_config to src/main/assets/msso_config.json.
4. Create a libs folder in src/main/libs and put latest arr files of mas-foundation and masui.
5. In Policy Manager, import the policy retrieveFlights.xml as /retrieveFlights
		a. Publish Web API -> Enter Service name as retrieveFlights and Gateway URL as retrieveFlights -> Finish
		b. File -> Import Policy (into your newly created api) -> Select retrieveFlights.xml
6. Build and Deploy the app to a device.

What does this app does?
1. Checks if the certificate is expired

## App Functionalities 
1. Successful Endpoint Access : To check if the ednpoint from the server is accessible.
2. Enable/Disable SSL Pinning : To enable or disable sslPinning.
3. Invalid Hostname : To validate hostname present in msso.config file.
4. Test with Expired Certificate : To verify if the certificate is expired. Copy the content of msso_config.json to msso_config_2.json and update the server_certs 	 array with the certificate you want to verify.
5. Test Intermediate Pinning Mode : Sample code to implement Intermediate pinning mode.
6. Test All Certificates Pinning Mode : Sample code to implement all pinning mode.
7. Test Public Key Hash Pinning : Sample code to implement Public Key Hash Pinning.


