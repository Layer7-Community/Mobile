//
//  ViewController.swift
//  MAG Native App
//
//  Created by Sean O'Connell on 18/10/2021.
//

import UIKit
import CoreLocation
import MapKit


let appIssuer = "com.brcm.ios.magnativeapp"
let userOfKeyChain = "magClient"



//
// JSON response from Protected API

struct productInfo: Codable {
    var id: Int
    var name: String
    var price: String
}

struct protectedAPIDataResponse: Codable {
    var products: [productInfo]
    var device_geo: String
}
//
//
// The following structure holds the MAG Client data that is exchnaged with the App from the remote MAG Server
//
struct MagClientData: Codable {
    var magServerBaseURL: String? = ""
    var magSSLPinningEnabled: String = "false"
    var magClientMasterClientId: String? = ""
    var magClientScope: String? = ""
    var magOauthClientId: String? = ""
    var magOauthClientSecret: String? = ""
    var magOauthClientExpiration: String? = ""
    var magUserName: String? = ""
    var magUserPassword: String? = ""
    var magClientDeviceName: String? = ""
    var magClientDeviceIdentifier: String? = ""
    var base64EncodedMagGeneratedClientCertificate: String? = ""
    var magRegistrationIdentifier: String? = ""
    var magDeviceStatus: String? = ""
    var magRegisterationIdToken: String? = ""
    var magRegisterationIdTokenType: String? = ""
    var magGeneratedAccessToken: String? = ""
    var magGeneratedAccessTokenType: String? = ""
    var magGeneratedAccessTokenExpiresIn: String? = ""
    var magGeneratedAccessTokenScope: String? = ""
    var magGeneratedRefreshAccessToken: String? = ""
    
    
    func getKeyChainFieldValue(fieldName:String) -> String {
        
        let result = KeychainHelper.standard.read(appIssuer: appIssuer,
                                                  userOfKeyChain: userOfKeyChain,
                                                  type: MagClientData.self)
        if (result == nil) {
            return ""
        } else {
            switch fieldName{
            case "magServerBaseURL":
                return (result?.magServerBaseURL)!
            case "magSSLPinningEnabled":
                return (result?.magSSLPinningEnabled)!
            case "magClientMasterClientId":
                return (result?.magClientMasterClientId)!
            case "magOauthClientId":
                return (result?.magOauthClientId)!
            case "magClientScope":
                return (result?.magClientScope)!
            case "magOauthClientSecret":
                return (result?.magOauthClientSecret)!
            case "magOauthClientExpiration":
                return (result?.magOauthClientExpiration)!
            case "magUserName":
                return (result?.magUserName)!
            case "magUserPassword":
                return (result?.magUserPassword)!
            case "magClientDeviceName":
                return (result?.magClientDeviceName)!
            case "magClientDeviceIdentifier":
                return (result?.magClientDeviceIdentifier)!
            case "base64EncodedMagGeneratedClientCertificate":
                return (result?.base64EncodedMagGeneratedClientCertificate)!
            case "magRegistrationIdentifier":
                return (result?.magRegistrationIdentifier)!
            case "magDeviceStatus":
                return (result?.magDeviceStatus)!
            case "magRegistrationIdToken":
                return (result?.magRegisterationIdToken)!
            case "magRegistrationIdTokenType":
                return (result?.magRegisterationIdTokenType)!
            case "magGeneratedAccessToken":
                return (result?.magGeneratedAccessToken)!
            case "magGeneratedAccessTokenType":
                return (result?.magGeneratedAccessTokenType)!
            case "magGeneratedAccessTokenExpiresIn":
                return (result?.magGeneratedAccessTokenExpiresIn)!
            case "magGeneratedAccessTokenScope":
                return (result?.magGeneratedAccessTokenScope)!
            case "magGeneratedRefreshAccessToken":
                return (result?.magGeneratedRefreshAccessToken)!
            default: return ""
            }
            
        }
        
    }
    
    func updateKeyChainField(fieldName:String, fieldValue:String) -> Void {
        
        
        let result = KeychainHelper.standard.read(appIssuer: appIssuer,
                                                  userOfKeyChain: userOfKeyChain,
                                                  type: MagClientData.self)
     
        var unWrappedResult = MagClientData()
        
        if (result != nil) {
            unWrappedResult = result!
        
        }
        
        switch fieldName {
            case "magClientMasterClientId":
                unWrappedResult.magClientMasterClientId = fieldValue
            case "magSSLPinningEnabled":
                unWrappedResult.magSSLPinningEnabled = fieldValue
            case "magClientScope":
                unWrappedResult.magClientScope = fieldValue
            case "magServerBaseURL":
                unWrappedResult.magServerBaseURL = fieldValue
            case "magOauthClientId":
                unWrappedResult.magOauthClientId = fieldValue
            case "magOauthClientSecret":
                unWrappedResult.magOauthClientSecret = fieldValue
            case "magOauthClientExpiration":
                unWrappedResult.magOauthClientExpiration = fieldValue
            case "magUserName":
                unWrappedResult.magUserName = fieldValue
            case "magUserPassword":
                unWrappedResult.magUserPassword = fieldValue
            case "magClientDeviceName":
                unWrappedResult.magClientDeviceName = fieldValue
            case "magClientDeviceIdentifier":
                unWrappedResult.magClientDeviceIdentifier = fieldValue
            case "base64EncodedMagGeneratedClientCertificate":
                unWrappedResult.base64EncodedMagGeneratedClientCertificate = fieldValue
            case "magRegistrationIdentifier":
                unWrappedResult.magRegistrationIdentifier = fieldValue
            case "magDeviceStatus":
                unWrappedResult.magDeviceStatus = fieldValue
            case "magRegistrationIdToken":
                unWrappedResult.magRegisterationIdToken = fieldValue
            case "magRegistrationIdTokenType":
            unWrappedResult.magRegisterationIdTokenType = fieldValue
            case "magGeneratedAccessToken":
                unWrappedResult.magGeneratedAccessToken = fieldValue
            case "magGeneratedAccessTokenType":
                unWrappedResult.magGeneratedAccessTokenType = fieldValue
            case "magGeneratedAccessTokenExpiresIn":
                unWrappedResult.magGeneratedAccessTokenExpiresIn = fieldValue
            case "magGeneratedAccessTokenScope":
                unWrappedResult.magGeneratedAccessTokenScope = fieldValue
            case "magGeneratedRefreshAccessToken":
                unWrappedResult.magGeneratedRefreshAccessToken = fieldValue
            default: break
        }
        
        func updateKeyChainField(fieldName:String, fieldValue:String) -> Void {
        }
        
        KeychainHelper.standard.save(unWrappedResult, appIssuer: appIssuer, userOfKeyChain: userOfKeyChain)
        
    }
}



class ViewController: UIViewController {

    @IBOutlet weak var dialogTitleUserStatus: UILabel!
    @IBOutlet weak var jsonResponseTextView: UITextView!
    
    @IBOutlet weak var loginButton: UIButton!
    
    @IBOutlet weak var logoutButton: UIButton!
    @IBOutlet weak var deRegisterButton: UIButton!
    
    @IBOutlet weak var sessionLockLabel: UILabel!
    
    @IBOutlet weak var sessionLockSwitch: UISwitch!
    
    @IBOutlet weak var dialogBottomUserStatusLabel: UILabel!
    
    @IBOutlet weak var protectedAPIButton: UIButton!
    
    var magServerURL: String = ""
    var magServerPemCertificates: [String] = []
    var magServerSSLPinning: Bool = false;
    var currectDeviceLocation = ""
    
    
    let locationManager = CLLocationManager()
    
    
    struct magServerClientIds: Decodable {
        var magServerClientId: String
        var magServerClientRegisteredScope: String
        var magServerClientStatus: String
        var redirect_uri: String
        var environment: String
        var registered_by: String
    }
    
    var magServerClientConfig: [magServerClientIds] = []

    
    
    
    
    
    //
    // Struct required for reading in the contents of the MAG Generated msso_config.json file
    //
    struct serverData: Decodable {
        var server: Server
        var oauth: OAuth
        var mag: Mag
    }
    struct Server : Decodable {
        var hostname: String
        var port: Int
        var prefix: String
        var server_certs: [[String]]
        
    }
    
    struct OAuth : Decodable {
        var client: Client
    }
    
    struct Client : Decodable {
        var organization: String
        var client_name: String
        var client_type: String
        var registered_by: String
        var client_ids: [Client_IDs]
    }
    
    struct Client_IDs: Decodable {
        var client_id: String
        var client_secret: String
        var scope: String
        var redirect_uri: String
        var status: String
        var registered_by: String
        var environment: String
    }
    
    struct Mag: Decodable {
        var mobile_sdk: Mobile_SDK
    }
    
    struct Mobile_SDK:Decodable {
        var enable_public_key_pinning: Bool
    }
    
    func onReceiveRefreshRequest(notification: Notification) {
        refreshDialogStatus()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        refreshDialogStatus()
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view.
        

        
        let observer = NotificationCenter.default.addObserver(
            forName: NSNotification.Name.didReceiveRefreshRequest,
                object: nil, queue: nil,
                using: onReceiveRefreshRequest)
        
        loadMAGServerConfig()
        
        
        refreshDialogStatus();
        
        
        
        // Store the device name into the keyChain if not already done
        
        
        if (MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName") == "") {
            MagClientData().updateKeyChainField(fieldName: "magClientDeviceName", fieldValue: UIDevice.current.name)
        }
        
        //
        // To Do : Get the Geo Location of the device which can be sent over to the MAG Server duirng an API call
        //
        
        locationManager.requestAlwaysAuthorization()
        locationManager.requestWhenInUseAuthorization()
          

        if CLLocationManager.locationServicesEnabled(){
            locationManager.delegate = self
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
          
        } else {
            NSLog ("Failed to get the GPS location")
        }
             
        locationManager.requestLocation()
        
    }

    override func didReceiveMemoryWarning() {
            super.didReceiveMemoryWarning()
            // Dispose of any resources that can be recreated.
        }

    
    @IBAction func loginInitiated(_ sender: Any) {
        
        // Lets collect the user login credentials
        
        
        
    }
    
    //
    // Expected JSON response we get from the unprotected API available on the MAG
    // server
    //
    struct unProtectedJsonResponse:Codable {
        let TimeStamp: String
    }
    
    
    
    @IBAction func unProtectedAPInitiated(_ sender: Any) {
        
        let unProtectedUrl:String = magServerURL + "/unprotected/products"
        
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(), delegateQueue: nil)
        
        //let session = URLSession.shared
        
        
        let unprotectedTask = session.dataTask(with: URL(string:unProtectedUrl)!, completionHandler: { data, response, error in
            
            guard let data=data, error == nil else {
                NSLog("UnProtected API - Invalid response received from the MAG Server")
                return
            }
            
            var result: unProtectedJsonResponse?
            
            do {
                result = try JSONDecoder().decode(unProtectedJsonResponse.self, from: data)
            }
            catch {
                NSLog ("Unexpected JSON payload \(error.localizedDescription)")
            }
            
            guard let jsonResponse = result else {
                return
            }
            
            let gatewayTimestamp = "Gateway Timestamp: " + jsonResponse.TimeStamp
            
            NSLog (gatewayTimestamp)
            
            //
            // Update the main text view with the result
            //
            
            DispatchQueue.main.async {
                self.jsonResponseTextView.textAlignment = .center
                self.jsonResponseTextView.text = gatewayTimestamp
            }
            
            
        }).resume()
        //unprotectedTask.resume()
        self.jsonResponseTextView.text=""
        
       
        
    }
    
    @IBAction func protectedAPIInitiated(_ sender: Any) {
        
        
        let protectedUrl:String = magServerURL + "/protected/resource/products?operation=listProducts"
        
        let request = NSMutableURLRequest(url: URL(string: protectedUrl )!)
        
        NSLog("INFO: About to register the MAG Client App with the MAG Server")
        
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(), delegateQueue: nil)
        
        // let session = URLSession.shared
        
        request.httpMethod = "GET"
        request.addValue((MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdentifier")), forHTTPHeaderField: "mag-identifier")
        request.addValue("Bearer " + (MagClientData().getKeyChainFieldValue(fieldName:"magGeneratedAccessToken")), forHTTPHeaderField: "Authorization")
        let clientCert = MagClientData().getKeyChainFieldValue(fieldName:"base64EncodedMagGeneratedClientCertificate").data(using: .utf8)
        let encodedCert:String = clientCert!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
        
        request.addValue(encodedCert, forHTTPHeaderField: "x-cert")
        locationManager.requestLocation()
        request.addValue(currectDeviceLocation, forHTTPHeaderField: "geo-location")
        
        let task = session.dataTask(with: request as URLRequest) { data, response, error in

    
            guard let httpResponse = response as? HTTPURLResponse else {
                return
            }
    
            if httpResponse.statusCode == 200 {
                NSLog("Got a response from the protected API - now to parse it")
                
                guard let data=data, error == nil else {
                    NSLog("UnProtected API - Invalid response received from the MAG Server")
                    return
                }
                
                var result: protectedAPIDataResponse?
                
                do {
                    result = try JSONDecoder().decode(protectedAPIDataResponse.self, from: data)
                }
                catch {
                    NSLog ("Unexpected JSON payload \(error.localizedDescription)")
                }
                
                guard let jsonResponse = result else {
                    return
                }
                
                let gatewayTimestamp = "Geo-Location: " + jsonResponse.device_geo
                
                var displayProtectedAPIResponse: String = "Products\n"
                
                for item in jsonResponse.products
                {
                    displayProtectedAPIResponse = displayProtectedAPIResponse + "\nId: \(item.id), Name: \(item.name), Price: \(item.price)\n"
                }
                
                displayProtectedAPIResponse = displayProtectedAPIResponse + "\nDevice Loc: \(jsonResponse.device_geo)\n\n"
                NSLog("MAG Client Device Geo-Locations is [\(jsonResponse.device_geo)]")
                
                DispatchQueue.main.async {
                    self.jsonResponseTextView.textAlignment = .center
                    self.jsonResponseTextView.text = displayProtectedAPIResponse
                }
                
            } else if (httpResponse.statusCode == 401) {
                //
                // Refresh the access token
                //
                NSLog("Need to refersh the access token for user \(MagClientData().getKeyChainFieldValue(fieldName: "magUserName"))")
                
                loginViewController().retrieveAccessToken(clientUsername: MagClientData().getKeyChainFieldValue(fieldName: "magUserName"), isIDTokenRequired: false)
                
                //
                // Send a signal to press the ProtectedAPI
                //
                self.protectedAPIButton.sendActions(for: .touchUpInside)
                
            } else {

                //
                // Refresh the access token
                //
                print ("MAG Server returned error is " + String(httpResponse.statusCode))
            }
        }.resume()
        
    }
 
    func loadMAGServerConfig() -> Void {
        
        //
        // Have we already processed the msso_config file?
        //
        
        var currentMagClientMasterClientId = MagClientData().getKeyChainFieldValue(fieldName: "magClientMasterClientId")
        
        //
        // Prevent reloading of the configuration data
        //  if (currentMagClientMasterClientId != "") {
        //    return
        //}
        
        if let url = Bundle.main.url(forResource: "msso_config", withExtension: "json") {
            do {
                let data = try Data(contentsOf: url)
                let decoder = JSONDecoder()
                let jsonData = try decoder.decode(serverData.self, from: data)
                
                magServerURL = "https://" + jsonData.server.hostname + ":\(jsonData.server.port)"
                var magClientMasterClientId: String = ""
                
                NSLog(" MAG Server URL is: [" + magServerURL + "]")
                
                
                //
                // Process the MAG Server Certificates supplied in the MSSO config file
                //

                let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                               kSecAttrLabel as String: magServerDeviceCertificateTag,
                                               kSecReturnRef as String: kCFBooleanTrue]
                

                var item: CFTypeRef?
                let certificateStatus = SecItemCopyMatching(getquery as CFDictionary, &item)
                var validCertificateFound:Bool = false
                
                //
                // Has the MAG Server certificate already been imported from a previous session
                //
                if (certificateStatus != errSecSuccess) {
                    
                    for (index, item) in jsonData.server.server_certs.enumerated()
                    {
                        var base64PemCertificate: String
                    
                        base64PemCertificate=""
                        for certificateEncodedBytes in item
                        {
                            base64PemCertificate = base64PemCertificate + certificateEncodedBytes + "\n"
                        }
                        magServerPemCertificates.append(base64PemCertificate)
                    
                        base64PemCertificate = base64PemCertificate.replacingOccurrences(of: "-----BEGIN CERTIFICATE-----\n", with: "")
                        base64PemCertificate = base64PemCertificate.replacingOccurrences(of: "\n-----END CERTIFICATE-----\n", with: "")
                        //
                        // Add the certificates to the Trust Store for SSL Pinning, if SSL Pinning is enabled
                        //
                    
                        if (jsonData.mag.mobile_sdk.enable_public_key_pinning) {
                            NSLog ("SSL Pinning has been enabled")
                        
                            if (MagClientData().getKeyChainFieldValue(fieldName: "magSSLPinningEnabled") == "" || MagClientData().getKeyChainFieldValue(fieldName: "magSSLPinningEnabled") == "false") {
                                MagClientData().updateKeyChainField(fieldName: "magSSLPinningEnabled", fieldValue: "true")
                            }
                            
                            let certData = NSData(base64Encoded: base64PemCertificate,
                                          options:NSData.Base64DecodingOptions.ignoreUnknownCharacters)!
                    
                            let cert = SecCertificateCreateWithData(kCFAllocatorDefault, certData)
                        
                        
                            let addquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                       kSecValueRef as String: cert,
                                                       kSecAttrLabel as String: magServerDeviceCertificateTag]
                    
                            let certificateAdditionalStatus = SecItemAdd(addquery as CFDictionary, nil)
                    
                            if (certificateAdditionalStatus == errSecSuccess) {
                                NSLog("Added in MSSO Config Certificate in the keychain")
                            }


                        }
                    }
                }
                var magClientScope: String = ""
                //
                // Process the config files supplied client Ids
                //
                for (index, item) in jsonData.oauth.client.client_ids.enumerated()
                {
                    NSLog ("MAG Server Client Client Id is [" + item.client_id + "]")
                    magServerClientConfig.append(magServerClientIds(magServerClientId:item.client_id,
                                                                    magServerClientRegisteredScope:item.scope,
                                                                    magServerClientStatus: item.status,
                                                                    redirect_uri: item.redirect_uri,
                                                                    environment: item.environment,
                                                                    registered_by: item.registered_by))

                    // Just using the first entry in the client info array
                    magClientMasterClientId = item.client_id
                    magClientScope = item.scope
                    break;
                }
                
                do {
                    
                    let magServerBaseURL = MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL")
                    let currentMagClientScope = MagClientData().getKeyChainFieldValue(fieldName: "magClientScope")
                    
                    currentMagClientMasterClientId = MagClientData().getKeyChainFieldValue(fieldName: "magClientMasterClientId")
                    
                    if (magServerBaseURL == "") {
                        MagClientData().updateKeyChainField(fieldName: "magServerBaseURL", fieldValue: magServerURL)
                    }
                    
                    if (currentMagClientMasterClientId == "") {
                        MagClientData().updateKeyChainField(fieldName: "magClientMasterClientId", fieldValue: magClientMasterClientId)
                    }
                    
                    if (currentMagClientScope == "") {
                        MagClientData().updateKeyChainField(fieldName: "magClientScope", fieldValue: magClientScope)
                    }
                    
                    
                    
                    
                }
                
                
            } catch {
                NSLog("error:\(error)")
         
            }
        }
    }
    
    func refreshDialogStatus() -> Void {
        
        self.jsonResponseTextView.text=""
        
        
        let magSupppliedAccessToken = MagClientData().getKeyChainFieldValue(fieldName: "magGeneratedAccessToken")
        
        if (magSupppliedAccessToken == "") {
            logoutButton.isHidden = true
            loginButton.isHidden = false
            deRegisterButton.isHidden = true
            sessionLockLabel.isHidden = true
            sessionLockSwitch.isHidden = true
            protectedAPIButton.isHidden = true
            self.dialogTitleUserStatus.text="Please Login to Authenticate"
            self.dialogBottomUserStatusLabel.textColor = UIColor.red //UIColor.black
            self.dialogBottomUserStatusLabel.text="Not Authenticated"
        } else {
            loginButton.isHidden = true;
            logoutButton.isHidden = false
            deRegisterButton.isHidden = false
            sessionLockLabel.isHidden = false
            sessionLockSwitch.isHidden = false
            protectedAPIButton.isHidden = false
            let magUserName = MagClientData().getKeyChainFieldValue(fieldName: "magUserName")
            if ( magUserName != "") {

                dialogTitleUserStatus.text = "Authenticated User [" + magUserName + "]"
                self.dialogBottomUserStatusLabel.textColor = UIColor.green
                self.dialogBottomUserStatusLabel.text="Authenticated"
            }
        }
              
    }
    
    @IBAction func logoutInitiated(_ sender: Any) {
        
        //
        // Remove the previously registered MAG supplied Access Token. User will have to re-autenticate to get another one
        //
        
        let result = KeychainHelper.standard.read(appIssuer: appIssuer,
                                                  userOfKeyChain: userOfKeyChain,
                                                  type: MagClientData.self)
        
        result?.updateKeyChainField(fieldName: "magGeneratedAccessToken", fieldValue: "")
        
        //
        // Need to initiate the logout MAG Server endpoint
        //
        
        // let session = URLSession.shared
        
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(), delegateQueue: nil)
        
        
        let request = NSMutableURLRequest(url: URL(string: MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL") + "/connect/session/logout" )!)
        
        request.httpMethod = "POST"
        
        request.addValue((MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdentifier")), forHTTPHeaderField: "mag-identifier")
        var requestParameters: String = "client_id=\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientId"))&client_secret=\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientSecret"))"
        
        requestParameters=requestParameters + "&id_token=\(MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdToken"))&id_token_type=\(MagClientData().getKeyChainFieldValue(fieldName: "magRegistrationIdTokenType"))&logout_apps=true"
        request.httpBody = requestParameters.data(using: String.Encoding.utf8)
        
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        
        
        let task = session.dataTask(with: request as URLRequest) { data, response, error in

    
            guard let httpResponse = response as? HTTPURLResponse else {
                return
            }
            if httpResponse.statusCode == 200 {
                NSLog("User [\(MagClientData().getKeyChainFieldValue(fieldName: "magUserName"))] as been successfully logged out")
                
            }
            MagClientData().updateKeyChainField(fieldName: "magGeneratedAccessToken", fieldValue: "")
            
        }.resume()
        
        refreshDialogStatus()
    }
    
    @IBAction func deRegisterInitiated(_ sender: Any) {
        
        //
        // Contact /connect/device/remove ad de-register
        //
        // Destroy any MAG client local security configuration in the KeyChain and refresh the dialog
        
        
        //
        // Need to tell the MAG Server to remove the device entry from its internal DB
        //
        
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(), delegateQueue: nil)
        
        
        let request = NSMutableURLRequest(url: URL(string: MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL") + "/connect/device/remove" )!)
        
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        request.addValue((MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdentifier")), forHTTPHeaderField: "mag-identifier")
        request.addValue("Bearer " + (MagClientData().getKeyChainFieldValue(fieldName:"magGeneratedAccessToken")), forHTTPHeaderField: "Authorization")
        let clientCert = MagClientData().getKeyChainFieldValue(fieldName:"base64EncodedMagGeneratedClientCertificate").data(using: .utf8)
        let encodedCert:String = clientCert!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
        
        request.addValue(encodedCert, forHTTPHeaderField: "x-cert")
        locationManager.requestLocation()
        request.addValue(currectDeviceLocation, forHTTPHeaderField: "geo-location")
        request.httpMethod = "DELETE"
                
        let task = session.dataTask(with: request as URLRequest) { data, response, error in

    
            let result = KeychainHelper.standard.read(appIssuer: appIssuer,
                                                      userOfKeyChain: userOfKeyChain,
                                                      type: MagClientData.self)
            
           
            // Tidy up things from the previous sesssion
            
            let authenticatedUserName = MagClientData().getKeyChainFieldValue(fieldName: "magUserName")
            
            
            //
            // Delete all the retrieved confiration session data
            //
            
            KeychainHelper.standard.delete(appIssuer: appIssuer, userOfKeyChain: userOfKeyChain)
            
            //
            // Reset the device Name
            //
            if (MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName") == "") {
                MagClientData().updateKeyChainField(fieldName: "magClientDeviceName", fieldValue: UIDevice.current.name)
            }
            
            //
            // reset the need for SSL pinning checking
            //
            let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                           kSecAttrLabel as String: magServerDeviceCertificateTag,
                                           kSecReturnRef as String: kCFBooleanTrue]
            

            var item: CFTypeRef?
            let certificateStatus = SecItemCopyMatching(getquery as CFDictionary, &item)

            
            //
            // Has the MAG Server certificate already been imported from a previous session - therefore SSL pinnig is required
            //
            if (certificateStatus == errSecSuccess) {
                
                MagClientData().updateKeyChainField(fieldName: "magSSLPinningEnabled", fieldValue: "true")
            
            }
            
            self.loadMAGServerConfig()
            
            
            //
            // Delete the MAG Client generated Private Key and retrieved Certificate
            //
            
            do {
                
                

                
                let deleteKeyQuery: [String: Any] = [ kSecClass as String: kSecClassKey,
                                                            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                                                            kSecAttrApplicationLabel as String: privKeyAttrApplicationLabel,
                                                            kSecAttrApplicationTag as String: privateKeyTag,
                                                            kSecReturnRef as String: true]
                
                
                let deletePrivateKeyStatus = SecItemDelete(deleteKeyQuery as CFDictionary)
                
                if (deletePrivateKeyStatus != errSecSuccess) {
                    NSLog("Failed to delete the MAG Client Private Key")
                    
                }
                let deleteCertQuery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                               kSecAttrLabel as String: magClientDeviceCertificateTag,
                                               kSecAttrPublicKeyHash as String: privKeyAttrApplicationLabel,
                                               kSecReturnRef as String: kCFBooleanTrue]
                
                let deleteCertKeyStatus = SecItemDelete(deleteCertQuery as CFDictionary)
                if (deleteCertKeyStatus != errSecSuccess) {
                    NSLog("Failed to delete the MAG CLient Certificate")
                    
                }
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                return
            }
            if httpResponse.statusCode == 200 {
                NSLog("User [\(authenticatedUserName)] as been successfully de-registered")
                
            }
            DispatchQueue.main.async {
                self.refreshDialogStatus()
            }
            
            
        }.resume()
        
        
        
        
        
    }
    
    @IBAction func sessionLockInitiated(_ sender: Any) {
        let alertController = UIAlertController(title: "Warning", message: "Session Lock not implemented", preferredStyle: UIAlertController.Style.alert)
        alertController.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: nil))
        self.present(alertController, animated: true, completion: nil)
        sessionLockSwitch.setOn(false, animated: false)
    }
}



extension ViewController: CLLocationManagerDelegate {

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
         if let location = locations.last {
            NSLog("User's Current Location: Lat: \(location.coordinate.latitude), Lon: = \(location.coordinate.longitude)")
             
             currectDeviceLocation = "\(location.coordinate.latitude),\(location.coordinate.longitude)"
            
             // Close updating location to peserve the device's battery
             
             manager.stopUpdatingLocation()
      
           }

      }
      
      func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
                NSLog("Failed to find user's location: \(error.localizedDescription)")
                manager.stopUpdatingLocation()
      }
    

    
    
}

extension Notification.Name {
    static let didReceiveRefreshRequest = Notification.Name("didReceiveRefreshRequest")

}

