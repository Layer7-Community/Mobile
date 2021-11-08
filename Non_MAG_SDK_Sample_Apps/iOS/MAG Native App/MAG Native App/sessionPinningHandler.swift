//
//  sessionPinningHandler.swift
//  MAG Native App
//
//  Created by Sean O'Connell on 05/11/2021.
//

import Foundation

class NSURLSessionPinningDelegate: NSObject, URLSessionDelegate {

    
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust {
            
            if (MagClientData().getKeyChainFieldValue(fieldName: "magSSLPinningEnabled") == "true") {
                
                let serverCredential = getServerUrlCredential(protectionSpace: challenge.protectionSpace)
            
                guard serverCredential != nil else {
                    completionHandler(.cancelAuthenticationChallenge, nil)
                    return
                }
                completionHandler(URLSession.AuthChallengeDisposition.useCredential, serverCredential)
            } else {
                completionHandler(URLSession.AuthChallengeDisposition.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
                return
            }
            
             
        } else if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodClientCertificate {
            
            if (MagClientData().getKeyChainFieldValue(fieldName: "magSSLPinningEnabled") == "false") {
                completionHandler(URLSession.AuthChallengeDisposition.useCredential, nil)
                return
            }
            
            NSLog("NSURLAuthenticationMethodClientCertificate Authentication Method")
            
            let getCertQuery: [String: Any] = [
                kSecClass as String : kSecClassCertificate,
                kSecReturnRef as String : kCFBooleanTrue,
                kSecAttrPublicKeyHash as String: privKeyAttrApplicationLabel,
            ]

            var certItem: CFTypeRef?
            let certStatus = SecItemCopyMatching(getCertQuery as CFDictionary, &certItem)

            
            guard certStatus == errSecSuccess else { return }

            let certificate = certItem as! SecCertificate


            let getIdentityQuery: [String: Any] = [kSecClass as String: kSecClassIdentity,
                                                   kSecReturnRef as String : kCFBooleanTrue]
            
         
            var identityItem: CFTypeRef?
            
            let certificateStatus = SecItemCopyMatching(getIdentityQuery as CFDictionary, &identityItem)
           
            if (certificateStatus == errSecSuccess) {
                   
                let identity = identityItem as! SecIdentity
                let magClientCredential = URLCredential(identity: identity, certificates: [certificate], persistence: .forSession)
                    completionHandler(URLSession.AuthChallengeDisposition.useCredential, magClientCredential)
                   
            } else {
                completionHandler(URLSession.AuthChallengeDisposition.useCredential, nil)
            }
        }
    }
    
    
    func urlSession(_ session: URLSession, didBecomeInvalidWithError error: Error?) {
            // We've got an error
            if let err = error {
                print("Error: \(err.localizedDescription)")
            } else {
                print("Error. Giving up")
            }
    }
    
    func getServerUrlCredential(protectionSpace:URLProtectionSpace)->URLCredential?{

        if let serverTrust = protectionSpace.serverTrust {
            
            //
            // Check if the server presented certificate is actually valid
            //
            
            var result = SecTrustResultType.invalid
            let status = SecTrustEvaluate(serverTrust, &result)
            print("SecTrustEvaluate res = \(result.rawValue)")

            if (status == errSecSuccess),
                let serverCertificate = SecTrustGetCertificateAtIndex(serverTrust, 0) {
                    //Get Server Certificate Data
                    let serverCertificateData = SecCertificateCopyData(serverCertificate)
                    //
                    // Get the registered MAG Server certificate and compare them
                    //
                
                    let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                   kSecAttrLabel as String: magServerDeviceCertificateTag,
                                                   kSecReturnData as String: kCFBooleanTrue]
                

                    var item: CFTypeRef?
                    let certificateStatus = SecItemCopyMatching(getquery as CFDictionary, &item)
                    var validCertificateFound:Bool = false
                
                    if (certificateStatus == errSecSuccess) {
                        
                        //
                        // Check if certificates are equal, otherwhise pinning failed and return nil
                        //
                        guard [serverCertificateData as? Data] == [item as? Data] else {
                            print("Pinned Certificate and the one received do not match.")
                            return nil
                        }
                        
                    }
                
                    return URLCredential(trust: serverTrust)
            }
        }

        return nil

    }

}
