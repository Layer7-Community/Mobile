//
//  keyChainHelper.swift
//  MAG Native App
//
//  Created by Sean O'Connell on 20/10/2021.
//

import Foundation



final class KeychainHelper {
    
    static let standard = KeychainHelper()
    private init() {}
    
    // Class implementation here...
    
    func save<T>(_ item: T, appIssuer: String, userOfKeyChain: String) where T : Codable {
        
        do {
            // Encode as JSON data and save in keychain
            let data = try JSONEncoder().encode(item)
            save(data, appIssuer: appIssuer, userOfKeyChain: userOfKeyChain)
            
        } catch {
            assertionFailure("Fail to encode item for keychain: \(error)")
        }
    }
    
    func read<T>(appIssuer: String, userOfKeyChain: String, type: T.Type) -> T? where T : Codable {
        
        // Read item data from keychain
        guard let data = read(appIssuer: appIssuer, userOfKeyChain: userOfKeyChain) else {
            return nil
        }
        
        // Decode JSON data to object
        do {
            let item = try JSONDecoder().decode(type, from: data)
            return item
        } catch {
            assertionFailure("Fail to decode item for keychain: \(error)")
            return nil
        }
    }
    
    func save(_ data: Data, appIssuer: String, userOfKeyChain: String) {
        
        // Create query
        let query = [
            kSecValueData: data,
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: appIssuer,
            kSecAttrAccount: userOfKeyChain,
        ] as CFDictionary
        
        // Add data in query to keychain
       var status = SecItemAdd(query, nil)
        
        if status == errSecDuplicateItem {
                // Item already exist, thus update it.
                let query = [
                    kSecAttrService: appIssuer,
                    kSecAttrAccount: userOfKeyChain,
                    kSecClass: kSecClassGenericPassword,
                ] as CFDictionary

                let attributesToUpdate = [kSecValueData: data] as CFDictionary

                // Update existing item
                status = SecItemUpdate(query, attributesToUpdate)
            }
        
        if status != errSecSuccess {
            // Print out the error
            NSLog("Error: \(status)")
        }
    }

    func read(appIssuer: String, userOfKeyChain: String) -> Data? {
        
        let query = [
            kSecAttrService: appIssuer,
            kSecAttrAccount: userOfKeyChain,
            kSecClass: kSecClassGenericPassword,
            kSecReturnData: true
        ] as CFDictionary
        
        var result: AnyObject?
        SecItemCopyMatching(query, &result)
        
        return (result as? Data)
    }
    
    func delete(appIssuer: String, userOfKeyChain: String) {
        
        let query = [
            kSecAttrService: appIssuer,
            kSecAttrAccount: userOfKeyChain,
            kSecClass: kSecClassGenericPassword,
            ] as CFDictionary
        
        // Delete item from keychain
        SecItemDelete(query)
    }
    
}

