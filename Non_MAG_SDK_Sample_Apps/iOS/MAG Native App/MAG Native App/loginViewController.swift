//
//  loginViewController.swift
//  MAG Native App
//
//  Created by Sean O'Connell on 21/10/2021.
//

import UIKit
import CryptoKit

extension Digest {
    var bytes: [UInt8] { Array(makeIterator()) }
    var data: Data { Data(bytes) }

    var hexStr: String {
        bytes.map { String(format: "%02X", $0) }.joined()
    }
}

let privKeyAttrApplicationLabel = "com.brcm.mag.sampleapp.privateKey.kSecAttrApplicationLabel".data(using: .utf8)!
let privateKeyTag = "com.brcm.mag.sampleapp.privateKey.kSecAttrApplicationTag".data(using: .utf8)!
let publicKeyTag = "com.brcm.mag.sampleapp.publicKey.kSecAttrApplicationTag".data(using: .utf8)!
let magClientDeviceCertificateTag = "com.brcm.mag.sampleapp.magClientDeviceCertificate".data(using: .utf8)!
let magServerDeviceCertificateTag = "com.brcm.mag.sampleapp.magServerDeviceCertificate".data(using: .utf8)!

class loginViewController: UIViewController {

    private let objectCommonName: [UInt8] = [0x06, 0x03, 0x55, 0x04, 0x03]
    private let objectOrganizationName: [UInt8] = [0x06, 0x03, 0x55, 0x04, 0x0A]
    private let objectOrganizationalUnitName: [UInt8] = [0x06, 0x03, 0x55, 0x04, 0x0B]
    private let objectDomainComponent: [UInt8] = [0x06, 0x0a, 0x09, 0x92, 0x26, 0x89, 0x93, 0xf2, 0x2c, 0x64, 0x01, 0x19]
    private let sequenceTag: UInt8 = 0x30
    private let setTag: UInt8 = 0x31

    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
       
        
        userPassword.enablePasswordToggle()
        
//        self.hideKeyboardWhenTappedAround()
        
        
    }
    
    @IBOutlet weak var userName: UITextField!
    
    @IBOutlet weak var userPassword: UITextField!
    
    
    struct MagInitData: Codable {
        var client_id: String
        var client_secret: String
        var client_expiration: Int
    }
    
    
    struct MagRetrieveAccessTokenResponseData: Codable {
        var access_token: String
        var token_type: String
        var expires_in: Int
        var refresh_token: String
        var scope: String
        var id_token: String? = ""
        var id_token_type: String? = ""
    }
    
    
    func loginViewController() {
        
    }
    
    @IBAction func loginHandler(_ sender: Any) {
        

        
        if (userName.text!.isEmpty  || userPassword.text!.isEmpty) {
            
            let alertController = UIAlertController(title: "Login Error", message: "Both a valid username and password must be provided", preferredStyle: UIAlertController.Style.alert)
            alertController.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: nil))
                present(alertController, animated: true, completion: nil)
            
            
             
        } else {
            
            
            //
            // We a username and Password to try out with the MAG Server
            //
            
            //
            // Check if we have a valid (non expored) Client Id and Secret already from the MAG Server as obtained from a previous registeration cycle
            //
            
                       
            if (MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientSecret") == "" || Int(Date().timeIntervalSince1970) >= Int(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientExpiration"))!) {
                
                // Step 1: Generate a MAG Client Device Identifier and store it in the keyChain
            
            
                if (MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier") == "") {

                    let data = UUID().uuidString.data(using: .utf8)
                
                    let digest = SHA256.hash(data: data!)
                    NSLog("Generated magClientDeviceIdentifier : [ \(digest.hexStr) ]")
                
                    MagClientData().updateKeyChainField(fieldName: "magClientDeviceIdentifier", fieldValue: digest.hexStr)
                }
            
                // Step 2: Call the /connect/client/initialize endpoint to get the client ID and secret from the master id
            
                let magServerServiceURL="/connect/client/initialize"
                let request = NSMutableURLRequest(url: URL(string: MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL") + magServerServiceURL )!)
            
                let session = URLSession.shared
                
                
                
                request.httpMethod = "POST"
                request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
                request.addValue("application/json", forHTTPHeaderField: "Accept")
            
                //
                // Add the randomly generated device-id as a header
                //
                                
                let utf8str = MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier").data(using: .utf8)
                let encodedDeviceId:String = utf8str!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
                
                print("Encoded Device Id Header: [\(encodedDeviceId)]")
                request.addValue(encodedDeviceId, forHTTPHeaderField: "device-id")
                
            
                //
                // Generate the required 25 bytes random nonce value
                //
            
            
                let paramString = String(format:"nonce=%@&client_id=%@",randomNonce(length: 25),MagClientData().getKeyChainFieldValue(fieldName: "magClientMasterClientId"))
            
                request.httpBody = paramString.data(using: String.Encoding.utf8)
            
                let task = session.dataTask(with: request as URLRequest) { data, response, error in

                
                
                    guard let httpResponse = response as? HTTPURLResponse else {
                        return
                    }
                
                    if httpResponse.statusCode == 200 {
                    
                        NSLog("JSON String: \(String(describing: String(data: data!, encoding: .utf8)))")
                    
                        guard let data=data, error == nil else {
                            NSLog("Invalid response received from the MAG Server")
                            return
                        }
                    
                        var result: MagInitData?
                    
                        do {
                            result = try JSONDecoder().decode(MagInitData.self, from: data)
                        
                        }
                        catch {
                            NSLog ("Unexpected JSON payload \(error.localizedDescription)")
                        }
                    
                        guard let jsonResponse = result else {
                            return
                        }
                        NSLog ("Returned Client Id: [ \(jsonResponse.client_id)]")
                        NSLog ("Returned Client Secret: [ \(jsonResponse.client_secret)]")
                        NSLog ("Returned Client Expiration: [ \(jsonResponse.client_expiration)]")
                    
                        // Store the response data into the KeyChain store
                    
                        MagClientData().updateKeyChainField(fieldName: "magOauthClientId", fieldValue: jsonResponse.client_id)
                        MagClientData().updateKeyChainField(fieldName: "magOauthClientSecret", fieldValue: jsonResponse.client_secret)
                        MagClientData().updateKeyChainField(fieldName: "magOauthClientExpiration", fieldValue: String(jsonResponse.client_expiration))
                 
                    
                        // Now we need register the user's device with the MAG Server, if not previously carried out
                        
                        
                        self.registerMagClient()
                        
                        
                    }
                
                }.resume()
            
            } else {
                NSLog ("App already has a valid registeration configuration")
                NSLog("ID Token = \(MagClientData().getKeyChainFieldValue(fieldName: "magRegistrationIdToken"))")
                self.registerMagClient()
                
            }
            
            
        }
    }
    @IBAction func dismissHandler(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    
    @IBAction func editingDidEnd(_ sender: UITextField) {
        sender.resignFirstResponder()
    }
    
    func randomNonce(length: Int) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<length).map{ _ in letters.randomElement()! })
    }
    

    //
    // Function to register the device with MAG Server
    //
    func registerMagClient() -> Void {
        
        let magServerServiceURL="/connect/device/register"
        
        NSLog("About to register the MAG Client App with the MAG Server")
        
        
        
        //
        // Need to generate an RSA Key pair of 2048 bits, if it doesn't already exist
        //
        
        
        let query: CFDictionary = [kSecClass as String: kSecClassKey,
                                            kSecAttrApplicationTag as String: privateKeyTag,
                                            kSecAttrApplicationLabel as String: privKeyAttrApplicationLabel,
                                            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
  //                                          kSecAttrLabel as String: privKeyAttrApplicationLabel,
                                            kSecReturnRef as String: true] as CFDictionary

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query, &item) //5
        
        var privateKey: SecKey? = nil
        var publicKey: SecKey? = nil
        
        //
        // Do I already have a private and public key pair for this device?
        // Skip the key generation process if I do
        //
        
        if (status != errSecSuccess) {
            NSLog("MAG Client private key has not been generated")
            let attributes: CFDictionary =
                        [kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                         kSecAttrKeySizeInBits as String: 2048,
                         kSecPrivateKeyAttrs as String:
                            [kSecAttrIsPermanent as String: true,
                             kSecAttrApplicationLabel as String: privKeyAttrApplicationLabel,
                             //kSecAttrLabel as String: privKeyAttrApplicationLabel,
                             kSecAttrApplicationTag as String: privateKeyTag ],
                         kSecPublicKeyAttrs as String:
                            [kSecAttrIsPermanent: true,
                              kSecAttrApplicationTag: publicKeyTag]
                        ] as CFDictionary

            var error: Unmanaged<CFError>?

            do {
                guard SecKeyCreateRandomKey(attributes, &error) != nil else {
                                 throw error!.takeRetainedValue() as Error
                }
                
             
                
                let query: CFDictionary = [kSecClass as String: kSecClassKey, //1
                                                   kSecAttrApplicationTag as String: privateKeyTag, // 2
                                                   kSecAttrKeyType as String: kSecAttrKeyTypeRSA, //3
                                                   kSecReturnRef as String: true] as CFDictionary //4

                var item: CFTypeRef?
                let status = SecItemCopyMatching(query, &item) //5
                if (status == errSecSuccess) {
                    privateKey = item as! SecKey
                    NSLog("MAG Client RSA key created")
                }
                
                
            } catch {
                NSLog(error.localizedDescription)
            }

        } else {
            privateKey = item as! SecKey
        }

        
        
  
        //
        // Do I have a valid device certificate registered on this device, if so then I just invoke the
        // token endpoint instead of goign through the whole registration process
        //
        
        let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                       kSecAttrPublicKeyHash as String: privKeyAttrApplicationLabel,
                                       kSecReturnData as String: kCFBooleanTrue]
                                       //kSecReturnRef as String: kCFBooleanTrue]
                
       
        
        let certificateStatus = SecItemCopyMatching(getquery as CFDictionary, &item)
        var validCertificateFound:Bool = false
        
        if (certificateStatus == errSecSuccess) {
            //
            // Certificate exists so only invoke the token endpoint on the MAG Server
            //
            
            let certData = item as? Data
            
            //
            // Need to check to see if the certificate has expired? If so - we haven't found a valid certificate and therefore need to get a new one
            // from the MAG server
            //
            // We have to traverse across the raw DER encoding of the certificate as IOS doesn't provide a means of extracting the NotAfter date without the use of third party libraries
            //
            
            // Skip the first 14 bytes as these are headers
            let initialOffset:Int = 14
            
            // Jump over the serial number
            var offset = initialOffset + Int(certData![initialOffset])
            
            // Jump the Object Identifier Sequence
            offset = offset + Int(certData![offset+2]) + 2
            
            // Jump the Issuer DN sequence
            offset = offset + Int(certData![offset+2]) + 2
            
            // Jump the NotBefore Sequence
            offset = offset + Int(certData![offset+4] + 7)
            
            // Extract the NotAfter sequence value
            
            let year = "\(Character(UnicodeScalar(certData![offset])))\(Character(UnicodeScalar(certData![offset+1])))"
            let month = "\(Character(UnicodeScalar(certData![offset+2])))\(Character(UnicodeScalar(certData![offset+3])))"
            let day = "\(Character(UnicodeScalar(certData![offset+4])))\(Character(UnicodeScalar(certData![offset+5])))"
            let hour = "\(Character(UnicodeScalar(certData![offset+6])))\(Character(UnicodeScalar(certData![offset+7])))"
            let min = "\(Character(UnicodeScalar(certData![offset+8])))\(Character(UnicodeScalar(certData![offset+9])))"
            let sec = "\(Character(UnicodeScalar(certData![offset+10])))\(Character(UnicodeScalar(certData![offset+11])))"
            
            var notValidAfterDate = year + month + day + hour + min + sec + "Z"
            
            let dateFormatter = DateFormatter()
            dateFormatter.locale = Locale(identifier: "en_US_POSIX") // set locale to reliable US_POSIX
            dateFormatter.dateFormat = "yyMMddHHmmssZ"
            let notAfterDate = dateFormatter.date(from:notValidAfterDate)!
            
            if (Int(Date().timeIntervalSince1970) <= Int(notAfterDate.timeIntervalSince1970)) {
                validCertificateFound=true
            }
            
        }
        
        if (!validCertificateFound) {
        
        
            //
            // Should have at least the private key by now - need to create a CSR
            //
        
            if (privateKey != nil) {
                publicKey = SecKeyCopyPublicKey(privateKey!)
                let publicKeyExportable = SecKeyCopyExternalRepresentation(publicKey!, nil)
            
                
                let csrOrgName:String = "Broadcom Software"
                let csrSubjectDN: String = "cn="+userName.text! + ",ou=\(MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier")),o=\(csrOrgName),dc=\(MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName"))"
            
                NSLog("CSR Subject DN is [\(csrSubjectDN)]")
            
            
            
                //
                // Create the Certificate Request Data Blob - coode adopted from [https://github.com/cbaker6/CertificateSigningRequest]
                // as Swift IOS lacks native support for creating a PKCS10 CSR request
                //
            
                let signedCSR: String = generateCertificateRequest(privateKey: privateKey!);
            
                let request = NSMutableURLRequest(url: URL(string: MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL") + magServerServiceURL )!)
        
                let session = URLSession.shared
            
                request.httpMethod = "POST"
                request.addValue("text/plain", forHTTPHeaderField: "Content-Type")
                request.addValue("application/json", forHTTPHeaderField: "Accept")
            
                // Add in the following headers
            
                var utf8str = MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier").data(using: .utf8)
                let encodedDeviceId:String = utf8str!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
            
                print("Encoded Device Id Header: [\(encodedDeviceId)]")
                request.addValue(encodedDeviceId, forHTTPHeaderField: "device-id")
            
                utf8str = MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName").data(using: .utf8)
                let encodedDeviceName:String = utf8str!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
            
                print("Encoded Device Name Header: [\(encodedDeviceName)]")
                request.addValue(encodedDeviceName, forHTTPHeaderField: "device-name")
            
                //
                // User Authorization Header
                //
            
                var authorisationData: String = userName.text! + ":" + userPassword.text!
            
                NSLog("User Authorisation Header Content is : [\(authorisationData)]")
            
                utf8str = authorisationData.data(using: .utf8)
            
                var encodedAuthorisationHeader:String = "Basic \(utf8str!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0)))"
        
                print("Encoded User Authorization Header: [\(encodedAuthorisationHeader)]")
                request.addValue(encodedAuthorisationHeader, forHTTPHeaderField: "Authorization")
            
                //
                // Client Authorization Header
                //
 
                authorisationData = "\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientId")):\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientSecret"))"
            
                NSLog("Client Authorisation Header Content is : [\(authorisationData)]")
            
                utf8str = authorisationData.data(using: .utf8)
            
                encodedAuthorisationHeader = "Basic \(utf8str!.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0)))"
                print("Encoded Client Authorization Header: [\(encodedAuthorisationHeader)]")
            
                request.addValue(encodedAuthorisationHeader, forHTTPHeaderField: "client-authorization")
            
                //
                // Insert the Base64Encoded Signed CSR into the body
                //
            
                request.httpBody = signedCSR.data(using: String.Encoding.utf8)
            
                let task = session.dataTask(with: request as URLRequest) { data, response, error in

            
                    guard let httpResponse = response as? HTTPURLResponse else {
                        return
                    }
            
                    if httpResponse.statusCode == 200 {
                
                        //
                        // Extract the mag_identifier from the response header
                        //
                        if let magIdentifier = httpResponse.allHeaderFields["mag-identifier"] as? String {
                            NSLog("MAG Server supplied Identifier is: [\(magIdentifier)]")
                            MagClientData().updateKeyChainField(fieldName: "magRegistrationIdentifier", fieldValue: magIdentifier)
                        }
                    
                        if let magSuppliedIdToken = httpResponse.allHeaderFields["id-token"] as? String {
                            NSLog("MAG Server supplied ID Token is: [\(magSuppliedIdToken)]")
                            MagClientData().updateKeyChainField(fieldName: "magRegistrationIdToken", fieldValue: magSuppliedIdToken)
                        }
                    
                        if let magSuppliedIdTokenType = httpResponse.allHeaderFields["id-token-type"] as? String {
                            NSLog("MAG Server supplied ID Token Type is: [\(magSuppliedIdTokenType)]")
                            MagClientData().updateKeyChainField(fieldName: "magRegistrationIdTokenType", fieldValue: magSuppliedIdTokenType)
                        }
                    
                        if let magDeviceStatus = httpResponse.allHeaderFields["device-status"] as? String {
                            NSLog("MAG Server Device Status is: [\(magDeviceStatus)]")
                            MagClientData().updateKeyChainField(fieldName: "magDeviceStatus", fieldValue: magDeviceStatus)
                        }
                        
                        if let magServerName = httpResponse.allHeaderFields["Server"] as? String {
                            NSLog("MAG Server Name is: [\(magServerName)]")
                        }
                   
                        //
                        // Is the device set to be 'activated' in the MAG Server DB?
                        //
                        NSLog("MAG Server Device Status is: [\(MagClientData().getKeyChainFieldValue(fieldName: "magDeviceStatus"))]")
                        if ( MagClientData().getKeyChainFieldValue(fieldName: "magDeviceStatus") != "activated") {
                            NSLog("Error - MAG Client App is not designated as activated")
                            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now()) {
                                let alertController = UIAlertController(title: "Error", message: "App Instance is not activated", preferredStyle: UIAlertController.Style.alert)
                                alertController.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: nil))
                                self.present(alertController, animated: true, completion: nil)
                                    }
                        } else {
                            DispatchQueue.main.async {
                                self.dismiss (animated: true, completion: nil)
                            }
                        }
                                            
                        //
                        // Need to extract the MAG supplied client certificate from the response body
                        //
                        
                        if let data = data {
                            
                            var base64EncodedCertificate = String(data: data, encoding: .utf8)
                            MagClientData().updateKeyChainField(fieldName: "base64EncodedMagGeneratedClientCertificate", fieldValue: base64EncodedCertificate!)
                            print("Base 64 MAG Supplied Certificate is: \n \(MagClientData().getKeyChainFieldValue(fieldName: "base64EncodedMagGeneratedClientCertificate"))")
                            
                            //
                            // Lets store the newly generated MAG provided client-certificate into the Keychain
                            //
                            
                            //
                            // Need to delete the associated ublic key as theer are situations where the presence of the public key confuses the identity matching code
                            //
                            
                            let deletePublicKeyQuery: [String: Any] = [ kSecClass as String: kSecClassKey,
                                                                        kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                                                                        kSecAttrApplicationTag as String: publicKeyTag,
                                                                        kSecReturnRef as String: true]
                            
                            
                            let deletePublicKeyStatus = SecItemDelete(deletePublicKeyQuery as CFDictionary)
                            
                            if (deletePublicKeyStatus == errSecSuccess) {
                            
                                
                                // remove the header string
                                let offset = ("-----BEGIN CERTIFICATE-----").count
                                let index = base64EncodedCertificate!.index(base64EncodedCertificate!.startIndex, offsetBy: offset+1)
                            
                                var base64EncodedCertificateWithoutHeaderFooter:String = String (base64EncodedCertificate![index...])
                                // remove the tail string
                                let tailWord = "-----END CERTIFICATE-----"
                                if let lowerBound = base64EncodedCertificateWithoutHeaderFooter.range(of: tailWord)?.lowerBound {
                                    base64EncodedCertificateWithoutHeaderFooter = String (base64EncodedCertificateWithoutHeaderFooter[...lowerBound])
                                }
                            
                                print("Cleaned up Base 64 MAG Supplied Certificate is: \n" + base64EncodedCertificateWithoutHeaderFooter)
                            
                                let certData = NSData(base64Encoded: base64EncodedCertificateWithoutHeaderFooter,
                                                  options:NSData.Base64DecodingOptions.ignoreUnknownCharacters)!
                            
                                let cert = SecCertificateCreateWithData(kCFAllocatorDefault, certData)
                            
                                let addquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                               kSecValueRef as String: cert,
                                                               kSecAttrPublicKeyHash as String: privKeyAttrApplicationLabel,
                                                               kSecAttrLabel as String: magClientDeviceCertificateTag]
                            
                                let certificateAdditionalStatus = SecItemAdd(addquery as CFDictionary, nil)
                            
                                if (certificateAdditionalStatus == errSecSuccess) {
                                    NSLog("Stored the MAG Client Certificate into the device keyChain: [\(base64EncodedCertificate)]")
                                
                                
                                    //
                                    // Now to retrieve the Access Token for the user, using the MAG Supplied JWT ID Token
                                    //
                                    self.retrieveAccessToken(clientUsername: self.userName.text!,isIDTokenRequired: false)
                                
                                    //
                                    // We can store the successfully authenticated username
                                    //
                                    MagClientData().updateKeyChainField(fieldName: "magUserName", fieldValue: self.userName.text!)
                                } else {
                                    NSLog("ERROR -> Failed to store the MAG Client Certificate into the device keyChain: [\(base64EncodedCertificate)]")
                                }
                            
                            }
                            
                        
                            
                        }
                      
                    } else {
                        
                        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now()) {
                            let alertController = UIAlertController(title: "Authentication Failure", message: "Invalid login credentials were supplied", preferredStyle: UIAlertController.Style.alert)
                            alertController.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: nil))
                            self.present(alertController, animated: true, completion: nil)
                                }
                        
                        
                        
                    }
            
                }.resume()

            
            }
        
        } else {
            NSLog("A MAG Client Certificate already is registered - skipping registeration process")
            self.retrieveAccessToken(clientUsername: self.userName.text!,isIDTokenRequired: true)
        }
    }
    
    
    public func retrieveAccessToken(clientUsername: String, isIDTokenRequired: Bool) -> Void {
        
        //
        // Retrieve the Access Token using either the originally provided ID Token or the provided the user credentials & client ID/Secret (thereby avoiding having
        // to do a complete re-registration cycle)
        //
        if (MagClientData().getKeyChainFieldValue(fieldName: "magGeneratedAccessToken") == "") {
            NSLog("INFO: Retrieving an new access token for the session")
            
            let magServerServiceURL="/auth/oauth/v2/token"
            let request = NSMutableURLRequest(url: URL(string: MagClientData().getKeyChainFieldValue(fieldName: "magServerBaseURL") + magServerServiceURL )!)
            
            NSLog("INFO: About to register the MAG Client App with the MAG Server")
            
            
            let session = URLSession.shared
            
            request.httpMethod = "POST"
            request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            request.addValue("application/json", forHTTPHeaderField: "Accept")
            request.addValue((MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdentifier")), forHTTPHeaderField: "mag-identifier")
            
            print (MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdToken"))
            
            let urlEncodedScope = MagClientData().getKeyChainFieldValue(fieldName:"magClientScope").addingPercentEncoding(withAllowedCharacters: .alphanumerics)
            var requestParameters: String = "scope=" + urlEncodedScope! + "&client_id=\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientId"))&client_secret=\(MagClientData().getKeyChainFieldValue(fieldName: "magOauthClientSecret"))"
            
            if (isIDTokenRequired) {
                
                var urlEncodedUserName = userName.text!.addingPercentEncoding(withAllowedCharacters: .alphanumerics)
                var urlEncodedUserPassword = userPassword.text!.addingPercentEncoding(withAllowedCharacters: .alphanumerics)
                
                requestParameters=requestParameters + "&grant_type=password&username=" + urlEncodedUserName! + "&password=" + urlEncodedUserPassword!
            } else {
                requestParameters=requestParameters + "&assertion=\(MagClientData().getKeyChainFieldValue(fieldName:"magRegistrationIdToken"))&grant_type=\(MagClientData().getKeyChainFieldValue(fieldName: "magRegistrationIdTokenType"))"
            }
                
            
            request.httpBody = requestParameters.data(using: String.Encoding.utf8)
            request.addValue("application/json", forHTTPHeaderField: "Accept")
            
            let task = session.dataTask(with: request as URLRequest) { data, response, error in

        
                guard let httpResponse = response as? HTTPURLResponse else {
                    return
                }
        
                if httpResponse.statusCode == 200 {
                    
                    NSLog("JSON String: \(String(describing: String(data: data!, encoding: .utf8)))")
                
                    guard let data=data, error == nil else {
                        NSLog("Invalid response received from the MAG Server")
                        return
                    }
                
                    var result: MagRetrieveAccessTokenResponseData?
                
                    do {
                        result = try JSONDecoder().decode(MagRetrieveAccessTokenResponseData.self, from: data)
                    
                    }
                    catch {
                        NSLog ("Unexpected JSON payload \(error.localizedDescription)")
                    }
                
                    guard let jsonResponse = result else {
                        return
                    }
                    NSLog ("Returned Access Token: [ \(jsonResponse.access_token)]")

                    MagClientData().updateKeyChainField(fieldName: "magGeneratedAccessToken", fieldValue: jsonResponse.access_token)
                    MagClientData().updateKeyChainField(fieldName: "magGeneratedAccessTokenType", fieldValue: jsonResponse.token_type)
                    MagClientData().updateKeyChainField(fieldName: "magGeneratedRefreshAccessToken", fieldValue: jsonResponse.refresh_token)
                    MagClientData().updateKeyChainField(fieldName: "magGeneratedAccessTokenScope", fieldValue: jsonResponse.scope)
                    
                    MagClientData().updateKeyChainField(fieldName: "magGeneratedAccessTokenExpiresIn", fieldValue: String(jsonResponse.expires_in))
                    
                    if (isIDTokenRequired) {
                        MagClientData().updateKeyChainField(fieldName: "magRegistrationIdToken", fieldValue: jsonResponse.id_token!)
                        MagClientData().updateKeyChainField(fieldName: "magRegistrationIdTokenType", fieldValue: jsonResponse.id_token_type!)
                        MagClientData().updateKeyChainField(fieldName: "magUserName", fieldValue: clientUsername)
                    }
                    
                    
                    
                    DispatchQueue.main.async {
                        
                        self.dismiss (animated: true, completion: nil)
                        NotificationCenter.default.post(name: Notification.Name.didReceiveRefreshRequest, object: nil)
                    }
                    
                    
                                        
                  
                } else {
                    
                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now()) {
                        let alertController = UIAlertController(title: "Authentication Failure", message: "Failed to get an accesss token", preferredStyle: UIAlertController.Style.alert)
                        alertController.addAction(UIAlertAction(title: "OK", style: UIAlertAction.Style.default, handler: nil))
                        self.present(alertController, animated: true, completion: nil)
                    }
                    
                }
        
            }.resume()
        } else {
            NSLog("WARNING: Have an existing access token - ignoring the request")
        }
        
        
        
    }
    
    
    func generateCertificateRequest(privateKey: SecKey) -> String {
        
        let publicKey:SecKey = SecKeyCopyPublicKey(privateKey)!
        let publicKeyExportable = SecKeyCopyExternalRepresentation(publicKey, nil)
        
        var encodedSignedCSR = ""
        
        let publicKeyBytes = publicKeyExportable! as Data
        
        let csrOrgName:String = "Broadcom Software"
        let csrSubjectDN: String = "cn="+userName.text! + ",ou=\(MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier")),o=\(csrOrgName),dc=\(MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName"))"
        
        NSLog("CSR Subject DN is [\(csrSubjectDN)]")
        
        do {
            var csrInfo = Data(capacity: 256)
            //Add version
            
            let version: [UInt8] = [0x02, 0x01, 0x00] // ASN.1 Representation of integer with value 1
            csrInfo.append(version, count: version.count)
            
            var subject = Data(capacity: 256)
            
            addSubjectNameItem(objectDomainComponent, value: MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceName"), into: &subject)
            addSubjectNameItem(objectOrganizationName, value: csrOrgName, into: &subject)
            addSubjectNameItem(objectOrganizationalUnitName, value: MagClientData().getKeyChainFieldValue(fieldName: "magClientDeviceIdentifier"), into: &subject)
            addSubjectNameItem(objectCommonName, value: userName.text!, into: &subject)
            
            enclose(&subject, by: sequenceTag)
                            
            csrInfo.append(subject)
            
            

            //
            // Need to add in the RSA Public Key
            //
            var publicKeyInfo = Data(capacity: 390)
            
            let objectRSAEncryptionNULL: [UInt8] = [0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00]
            
            publicKeyInfo.append(objectRSAEncryptionNULL, count: objectRSAEncryptionNULL.count)
            enclose(&publicKeyInfo, by: sequenceTag)
            
            var publicKeyASN = Data(capacity: 260)
            
            let mod = getPublicKeyMod(publicKeyBytes)
            let integer: UInt8 = 0x02 //Integer
            
            publicKeyASN.append(integer)
            appendDERLength(mod.count, into: &publicKeyASN)
            publicKeyASN.append(mod)

            let exp = getPublicKeyExp(publicKeyBytes)
            publicKeyASN.append(integer)
            appendDERLength(exp.count, into: &publicKeyASN)
            publicKeyASN.append(exp)

            enclose(&publicKeyASN, by: sequenceTag)
            
            prependByte(0x00, into: &publicKeyASN) //Prepend 0 (?)
            appendBITSTRING(publicKeyASN, into: &publicKeyInfo)

            enclose(&publicKeyInfo, by: sequenceTag) // Enclose into SEQUENCE
            
            
            csrInfo.append(publicKeyInfo)
            
            // Add attributes
            
            let attributes: [UInt8] = [0xA0, 0x00]
            csrInfo.append(attributes, count: attributes.count)
            enclose(&csrInfo, by: sequenceTag) // Enclose into SEQUENCE
            
            
            //
            // Need to sign the CSR data blob with the private key
            //
            
            var signature = [UInt8](repeating: 0, count: 256) // Enough space for a 2058 bit key'd signature
            var signatureLen: Int = signature.count

            var error: Unmanaged<CFError>?
            
                            
            do {
                
            
                guard let signatureData = SecKeyCreateSignature(privateKey,
                                                            .rsaSignatureMessagePKCS1v15SHA256,
                                                            csrInfo as CFData, &error) as Data? else {
                    if error != nil {
                        NSLog("Error in creating signature: \(error!.takeRetainedValue())")
                        
                    }
                    throw error!.takeRetainedValue() as Error

                }
                signatureData.copyBytes(to: &signature, count: signatureData.count)
                signatureLen = signatureData.count

            
                if !SecKeyVerifySignature(publicKey, .rsaSignatureMessagePKCS1v15SHA256,
                                                  csrInfo as CFData, signatureData as CFData, &error) {
                    NSLog("Signature verification failed with error [\(error!.takeRetainedValue())")
                    throw error!.takeRetainedValue() as Error

                }
                
                NSLog("CSR Signature is Valid")
                
                var certificationRequest = Data(capacity: 1024)
                certificationRequest.append(csrInfo)
                let sequenceObjectSHA256WithRSAEncryption: [UInt8] =
                    [0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 1, 1, 0x0B, 0x05, 0x00]
                
                
                certificationRequest.append(sequenceObjectSHA256WithRSAEncryption, count: sequenceObjectSHA256WithRSAEncryption.count)

                var signData = Data(capacity: 257)
                let zero: UInt8 = 0 // Prepend zero
                
                signData.append(zero)
                signData.append(signature, count: signatureLen)
                appendBITSTRING(signData, into: &certificationRequest)

                enclose(&certificationRequest, by: sequenceTag) // Enclose into SEQUENCE
                
                let base64CSR = certificationRequest.base64EncodedString(options: NSData.Base64EncodingOptions(rawValue: 0))
                    .addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed)
                
                // NSLog("Base64 Encoded CSR is [\(base64CSR)]")
                
                //
                // Need to add the footer and header to the CSR
                //
                
                let head = "-----BEGIN CERTIFICATE REQUEST-----\n"
                let foot = "-----END CERTIFICATE REQUEST-----\n"
                var isMultiple = false
                var newCSRString = head

                //Check if string size is a multiple of 64
                
                if base64CSR!.count % 64 == 0 {
                    isMultiple = true
                }

                for (integer, character) in base64CSR!.enumerated() {
                     newCSRString.append(character)

                    if (integer != 0) && ((integer + 1) % 64 == 0) {
                        newCSRString.append("\n")
                    }

                    if (integer == base64CSR!.count-1) && !isMultiple {
                        newCSRString.append("\n")
                    }

                }

                newCSRString += foot
                
                print("Formatted Encoded CSR is :\n" + newCSRString)
                
                encodedSignedCSR = newCSRString;
                //
                // Armed with the CSR, lets invoke the MAG device register endpoint with the user credentials and client_id/secret
                //
                
                
                
                
            
            } catch {
                NSLog(error.localizedDescription)
            }

                            
            
        }
        
        return encodedSignedCSR;
    }
    
    func addSubjectNameItem(_ what: [UInt8], value: String, into: inout Data ) {

        if what.count != 5 && what.count != 12 {
            NSLog("Error: appending to a non-subject item")
            return
        }
        var subjectItem = Data(capacity: 128)

        subjectItem.append(what, count: what.count)
        appendUTF8String(string: value, into: &subjectItem)
        enclose(&subjectItem, by: sequenceTag)
        enclose(&subjectItem, by: setTag)

        into.append(subjectItem)
    }
    
    func appendUTF8String(string: String, into: inout Data) {

            let strType: UInt8 = 0x0C //UTF8STRING
            into.append(strType)
            appendDERLength(string.lengthOfBytes(using: String.Encoding.utf8), into: &into)
            into.append(string.data(using: String.Encoding.utf8)!)
    }

    func appendDERLength(_ length: Int, into: inout Data) {

            assert(length < 0x8000)

            if length < 128 {
                let dLength = UInt8(length)
                into.append(dLength)

            } else if length < 0x100 {

                var dLength: [UInt8] = [0x81, UInt8(length & 0xFF)]
                into.append(&dLength, count: dLength.count)

            } else if length < 0x8000 {

                let preRes: UInt = UInt(length & 0xFF00)
                let res = UInt8(preRes >> 8)
                var dLength: [UInt8] = [0x82, res, UInt8(length & 0xFF)]
                into.append(&dLength, count: dLength.count)
        }
    }
    
    func enclose(_ data: inout Data, by: UInt8) {

            var newData = Data(capacity: data.count + 4)

            newData.append(by)
            appendDERLength(data.count, into: &newData)
            newData.append(data)

            data = newData
    }
    
    func getPublicKeyExp(_ publicKeyBits: Data) -> Data {

            var iterator = 0

            iterator+=1 // TYPE - bit stream - mod + exp
            _ = derEncodingGetSizeFrom(publicKeyBits, at: &iterator) // Total size
            iterator+=1 // TYPE - bit stream mod
            let modSize = derEncodingGetSizeFrom(publicKeyBits, at: &iterator)
            iterator += modSize

            iterator+=1 // TYPE - bit stream exp
            let expSize = derEncodingGetSizeFrom(publicKeyBits, at: &iterator)

            let range: Range<Int> = iterator ..< (iterator + expSize)

            return publicKeyBits.subdata(in: range)
        }

    func getPublicKeyMod(_ publicKeyBits: Data) -> Data {

            var iterator = 0

            iterator+=1 // TYPE - bit stream - mod + exp
            _ = derEncodingGetSizeFrom(publicKeyBits, at: &iterator)

            iterator+=1 // TYPE - bit stream mod
            let modSize = derEncodingGetSizeFrom(publicKeyBits, at: &iterator)

            let range: Range<Int> = iterator ..< (iterator + modSize)

            return publicKeyBits.subdata(in: range)
    }
    
    func derEncodingGetSizeFrom(_ buf: Data, at iterator: inout Int) -> Int {

            var data = [UInt8](repeating: 0, count: buf.count)
            buf.copyBytes(to: &data, count: buf.count)

            var itr = iterator
            var numOfBytes = 1
            var ret = 0

            if data[itr] > 0x80 {
                numOfBytes = Int((data[itr] - 0x80))
                itr += 1
            }

            for index in 0 ..< numOfBytes {
                ret = (ret * 0x100) + Int(data[itr + index])
            }

            iterator = itr + numOfBytes

         return ret
    }
    
    func prependByte(_ byte: UInt8, into: inout Data) {

            var newData = Data(capacity: into.count + 1)

            newData.append(byte)
            newData.append(into)

            into = newData
    }
    
    func appendBITSTRING(_ data: Data, into: inout Data) {

            let strType: UInt8 = 0x03 //BIT STRING
            into.append(strType)
            appendDERLength(data.count, into: &into)
            into.append(data)
    }
    
    
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}

extension UITextField {
    
    
fileprivate func setPasswordToggleImage(_ button: UIButton) {
    if(isSecureTextEntry){
        button.setImage(UIImage(named: "openEye"), for: .normal)
    }else{
        button.setImage(UIImage(named: "slashedEye"), for: .normal)

    }
}

func enablePasswordToggle(){
    let button = UIButton(type: .custom)
    setPasswordToggleImage(button)
    button.imageEdgeInsets = UIEdgeInsets(top: 0, left: -16, bottom: 0, right: 0)
    button.frame = CGRect(x: CGFloat(self.frame.size.width - 25), y: CGFloat(25), width: CGFloat(25), height: CGFloat(25))
    button.addTarget(self, action: #selector(self.togglePasswordView), for: .touchUpInside)
    self.rightView = button
    self.rightViewMode = .always
}
@IBAction func togglePasswordView(_ sender: Any) {
    self.isSecureTextEntry = !self.isSecureTextEntry
    setPasswordToggleImage(sender as! UIButton)
}
}

extension UIViewController {
    func hideKeyboardWhenTappedAround() {
        let tap = UITapGestureRecognizer(target: self, action: #selector(UIViewController.dismissKeyboard))
        tap.cancelsTouchesInView = false
        view.addGestureRecognizer(tap)
    }
    
    @objc func dismissKeyboard() {
        view.endEditing(true)
    }
}

