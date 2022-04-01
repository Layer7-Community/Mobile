# SampleApp-MagTraining-SessionTransfer-BLE

**Required:**
* Latest version of Android Studio

## Get Started
1. In Android Studio, open the project 'SampleApp-MagTraining-SessionTransfer-BLE'.
2. In the CA OAuth Manager, create an app, and export the msso_config file (https://you_server_name:8443/oauth/manager). For help with this file, see [Android Guide](http://techdocs.broadcom.com/content/broadcom/techdocs/us/en/ca-enterprise-software/layer7-api-management/mobile-sdk-for-ca-mobile-api-gateway/2-2.html).
3. Copy the contents of the exported msso_config to src/main/assets/msso_config.json.
4. Build and Deploy the app to a device.

Mobile SDK Proximity login support includes Near Field Communication (NFC) for Android devices.

With NFC login enabled, your can transfer a current user session on one device to another device. You do not have to log into the application on the new device.

