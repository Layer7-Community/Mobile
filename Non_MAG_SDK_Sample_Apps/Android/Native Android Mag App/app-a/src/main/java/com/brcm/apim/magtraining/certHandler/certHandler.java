package com.brcm.apim.magtraining.certHandler;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.brcm.apim.magtraining.MainActivity;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.ContentVerifierProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.pkcs.PKCSException;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;

import static android.security.keystore.KeyProperties.BLOCK_MODE_CBC;
import static android.security.keystore.KeyProperties.BLOCK_MODE_CTR;
import static android.security.keystore.KeyProperties.BLOCK_MODE_ECB;
import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.DIGEST_NONE;
import static android.security.keystore.KeyProperties.DIGEST_SHA1;
import static android.security.keystore.KeyProperties.DIGEST_SHA256;
import static android.security.keystore.KeyProperties.DIGEST_SHA384;
import static android.security.keystore.KeyProperties.DIGEST_SHA512;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
import static android.security.keystore.KeyProperties.SIGNATURE_PADDING_RSA_PKCS1;
import static android.security.keystore.KeyProperties.SIGNATURE_PADDING_RSA_PSS;

public class certHandler {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int MAX_CHAIN = 9;

    private static final String MSSO_DN = "cn=msso";
    private static final String magDeviceIdentifierAlias = "com.brcm.device.identifier";
    private static String magClientKeyStore = "AndroidKeyStore";
    private static PKCS10CertificationRequest csr = null;

    //
    // Generate a random Mag Client Id which is needed during the registeration process
    //
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String generateMagClientDeviceIdentifier() {

        String magDeviceIdentifier = null;

        generateKeyPair(magClientKeyStore, magDeviceIdentifierAlias);
        PublicKey deviceIdPublicKey = getPublicKey(magClientKeyStore,magDeviceIdentifierAlias);
        byte[] encoded = deviceIdPublicKey.getEncoded();

        //
        // Don't need the public key anymore - delete it
        //

        deleteKey(magClientKeyStore, magDeviceIdentifierAlias);

        //
        //Encode to SHA-256 and then convert to a hex string
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(encoded);
            byte[] mdBytes = md.digest();
            magDeviceIdentifier = hexDump(mdBytes,0, mdBytes.length, false);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        return magDeviceIdentifier;
    }

    private static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] hexadecimal_upper = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static String hexDump(byte[] binaryData, int off, int len, boolean upperCase) {
        final char[] hex = upperCase ? hexadecimal_upper : hexadecimal;
        if (binaryData == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > binaryData.length) throw new IllegalArgumentException();
        char[] buffer = new char[len * 2];
        for (int i = 0; i < len; i++) {
            int low = (binaryData[off + i] & 0x0f);
            int high = ((binaryData[off + i] & 0xf0) >> 4);
            buffer[i*2] = hex[high];
            buffer[i*2 + 1] = hex[low];
        }
        return new String(buffer);
    }

    //
    // Retrieve a private Key from the key store using the supplied keyStore name and Alias
    //
    public static PrivateKey getPrivateKey(String keyStoreName,String keyAlias) {
        // Need to search the Android Key Store to determine if indeed the key has been previously generated
        try {
            KeyStore keyStore = getKeyStore(keyStoreName);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, null);
            return privateKey;
        } catch (Exception e) {
            return null;
        }

    }

    //
    // Retrieve a Public Key from the key store using the supplied keyStore name and Alias
    //
    public static PublicKey getPublicKey(String keyStoreName,String keyAlias) {

        // Need to search the Android Key Store to determine if indeed the key has been previously generated
        try {
            KeyStore keyStore = getKeyStore(keyStoreName);

            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            return publicKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //
    // Generate the MAG Client RSA Key Pair which is needed for mutual SSL
    //

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void generateKeyPair(String keyStoreName, String keyAlias)
    {
        KeyPair rsaKeyPair = null;
        KeyPairGenerator keyGen = null;

        try {
            keyGen = KeyPairGenerator.getInstance("RSA",keyStoreName);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        try {

            Calendar cal = Calendar.getInstance();
            Date now = cal.getTime();
            cal.add(Calendar.YEAR, 1);
            Date end = cal.getTime();

            Log.d( TAG, "RSA Key Pair Alias is: [" + keyAlias + ']' );

            keyGen.initialize(new KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT + KeyProperties.PURPOSE_DECRYPT
                            + KeyProperties.PURPOSE_SIGN + KeyProperties.PURPOSE_VERIFY)
                    .setSignaturePaddings(SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .setCertificateNotBefore(now).setCertificateNotAfter(end)
                    //.setCertificateSubject(new X500Principal(MSSO_DN))
                    .setCertificateSerialNumber(BigInteger.valueOf(1))
                    .setRandomizedEncryptionRequired(false)
                    .setBlockModes(BLOCK_MODE_CBC, BLOCK_MODE_CTR, BLOCK_MODE_ECB, BLOCK_MODE_GCM)
                    .setDigests(DIGEST_NONE, DIGEST_SHA1, DIGEST_SHA256, DIGEST_SHA384, DIGEST_SHA512)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_NONE,ENCRYPTION_PADDING_PKCS7, ENCRYPTION_PADDING_RSA_OAEP, ENCRYPTION_PADDING_RSA_PKCS1)
                    .setSignaturePaddings(SIGNATURE_PADDING_RSA_PSS, SIGNATURE_PADDING_RSA_PKCS1)
                    .build()); // defaults to RSA 2048
            keyGen.generateKeyPair();

        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        Log.d( TAG, "RSA Key Pair has been generated!" );
    }

    //
    // Create the PKCS10 certificate signing request (CSR) from private and public keys
    // We check the signature on the CSR prior to returning it
    //
    public static PKCS10CertificationRequest generateCSR(String keyStoreName, String keyPairAlias, X500Principal csrDN) {


        try {
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(getPrivateKey(keyStoreName, keyPairAlias));

            PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(
                    csrDN, getPublicKey(keyStoreName, keyPairAlias));
            ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
            extensionsGenerator.addExtension(Extension.basicConstraints, true, new BasicConstraints(
                    true));
            csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                    extensionsGenerator.generate());
            csr = csrBuilder.build(signer);

            JcaContentVerifierProviderBuilder contentVerifierProviderBuilder = new JcaContentVerifierProviderBuilder();
            ContentVerifierProvider contentVerifierProvider = contentVerifierProviderBuilder.build(getPublicKey(keyStoreName,keyPairAlias));
            Log.d(TAG, "isSignatureValid? " + csr.isSignatureValid(contentVerifierProvider));
            Log.d(TAG, "Subject Name: " + csr.getSubject());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (PKCSException e) {
            e.printStackTrace();
        }
        return csr;
    }

    //
    // Convert the CSR into a PEM structured string
    //
    public static String stringEncodedCSR() {

        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(csr);
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static KeyStore getKeyStore(String keyStoreName) throws java.security.KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreName);
        keyStore.load(null);
        return keyStore;
    }

    //
    // Save a certificate chain into the internal Key Store
    //
    public void saveCertificateChain(String keyStoreName, String alias, X509Certificate[] chain) throws KeyStoreException {
        try {
            KeyStore keyStore = getKeyStore(keyStoreName);
            // delete any existing ones
            for (int i = 1; i <= MAX_CHAIN; i++) {
                keyStore.deleteEntry(alias + i);
            }
            // now add the new ones
            for (int i = 0; i < chain.length; i++) {
                Log.d(TAG, "Certificate Store Alias is: [" + alias + (i + 1) + "]");
                keyStore.setCertificateEntry(alias + (i + 1), chain[i]);
            }
        } catch (Exception e) {
            throw new KeyStoreException(e);
        }
    }

    //
    // Retrieve a certificate chain from the internal Key Store
    //
    public static X509Certificate[] getCertificateChain(String keyStoreName, String alias) throws KeyStoreException {
        try {
            X509Certificate[] returnChain = null;

            KeyStore keyStore = getKeyStore(keyStoreName);
            // Discover how many certificates are in the chain
            int numInChain = 0;
            for (Enumeration e = keyStore.aliases(); e.hasMoreElements(); ) {
                String aliasFound = (String) e.nextElement();
                if (aliasFound.startsWith(alias)) {
                    numInChain = 1;

                    break;
                }
            }
            if (numInChain > 0) {
                returnChain = new X509Certificate[numInChain];
                for (int i = 0; i < numInChain; i++) {
                    X509Certificate extractedCert = (X509Certificate) keyStore.getCertificate(alias + (i + 1));
                    if (extractedCert != null)
                        returnChain[i] = extractedCert;
                    else
                        return null;
                }
            }

            return returnChain;
        } catch (Exception e) {
            return null;
        }
    }

    //
    // Delete a certificate chain from the internal Key Store
    //
    public void deleteCertificateChain(String keyStoreName, String alias) {
        try {
            KeyStore keyStore = getKeyStore(keyStoreName);
            for (int i = 1; i <= MAX_CHAIN; i++) {
                keyStore.deleteEntry(alias + i);
            }
        } catch (Exception ignore) {
            //ignore
        }

    }

    //
    // Delete a specific key e.g. private or public from the internal Key Store
    //
    public static void deleteKey(String keyStoreName, String alias) {

        try {
            KeyStore lKeyStore = getKeyStore(keyStoreName);
            for (Enumeration e = lKeyStore.aliases(); e.hasMoreElements(); ) {
                String aliasFound = (String) e.nextElement();
                if (aliasFound.startsWith(alias)) {
                    lKeyStore.deleteEntry(alias);

                }
            }

        } catch (Exception ignore) {
            //ignore
        }
    }

}
