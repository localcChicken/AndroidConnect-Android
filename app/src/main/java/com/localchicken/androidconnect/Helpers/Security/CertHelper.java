package com.localchicken.androidconnect.Helpers.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

public class CertHelper {
    private static class Names {
        private static String privateKeyName = "privKey";
        private static String publicKeyName = "pubKey";
        private static String certificateName = "certificate";
        private static String serverCertificateName = "servCert";
    }


    static X509Certificate certificate;
    static final BouncyCastleProvider BC = new BouncyCastleProvider();

    public static boolean InitCert(Context ctx){
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            byte[] certBytes = DecodeFromString(settings.getString(Names.certificateName, ""));
            X509CertificateHolder holder = new X509CertificateHolder(certBytes);
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(holder);
            certificate = cert;
            return true;

        }catch (Exception e){
            Log.e("AndroidC", "Certificate not found, generating a new one!");
        }

        try {
            GenerateCertificate(ctx);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean InitKeypair(Context ctx) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        if(!settings.contains(Names.publicKeyName) | !settings.contains(Names.privateKeyName)){
            return GenerateKeypair(ctx);
        }
        return true;
    }


    private static void GenerateCertificate(Context ctx) throws Exception{
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, "HuemiSosiPizdu"); // devID
        nameBuilder.addRDN(BCStyle.O, "localChicken"); // Name
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date notBefore = calendar.getTime();
        calendar.add(Calendar.YEAR, 1);
        Date notAfter = calendar.getTime();
        PublicKey publicKey = GetPublicKey(ctx);
        PrivateKey privateKey = GetPrivateKey(ctx);


        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                nameBuilder.build(),
                BigInteger.ONE,
                notBefore,
                notAfter,
                nameBuilder.build(),
                publicKey
        );

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(privateKey);

        certificate = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certificateBuilder.build(contentSigner));

        SharedPreferences.Editor edit = settings.edit();
        edit.putString(Names.certificateName, Base64.encodeToString(certificate.getEncoded(), 0));
        edit.apply();

    }


    public static boolean GenerateKeypair(Context ctx) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);

        if(!settings.contains(Names.publicKeyName) || !settings.contains(Names.privateKeyName)){
            KeyPair kp;
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                kp = keyPairGenerator.generateKeyPair();

                byte[] publicKey = kp.getPublic().getEncoded();
                byte[] privateKey = kp.getPrivate().getEncoded();

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Names.publicKeyName, EncodeToString(publicKey));
                editor.putString(Names.privateKeyName, EncodeToString(privateKey));
                editor.apply();
            }catch(Exception e){
                return false;
            }
        }
        return true;
    }



    public static X509Certificate GetCertificate(Context ctx) throws Exception {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        byte[] certBytes = DecodeFromString(settings.getString(Names.certificateName, ""));
        X509CertificateHolder holder = new X509CertificateHolder(certBytes);
        return new JcaX509CertificateConverter().setProvider(BC).getCertificate(holder);
    }

    public static X509Certificate GetServerCertificate(Context ctx) throws Exception {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        byte[] certBytes = DecodeFromString(settings.getString(Names.serverCertificateName, ""));
        X509CertificateHolder holder = new X509CertificateHolder(certBytes);
        return new JcaX509CertificateConverter().setProvider(BC).getCertificate(holder);
    }



    public static PrivateKey GetPrivateKey(Context ctx) throws java.security.NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException{
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        byte[] privKeyBytes = DecodeFromString(settings.getString(Names.privateKeyName, ""));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privKeyBytes));
    }

    public static PublicKey GetPublicKey(Context ctx) throws java.security.NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        byte[] pubKeyBytes = DecodeFromString(settings.getString(Names.publicKeyName, ""));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKeyBytes));
    }

    private static String EncodeToString(byte[] data){
        return Base64.encodeToString(data, 0);
    }

    private static byte[] DecodeFromString(String str){
        byte[] retVal = Base64.decode(str, 0);
        return retVal;
    }
}
