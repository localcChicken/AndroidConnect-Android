package com.localchicken.androidconnect.Helpers.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLHelper {
    static X509Certificate certificate;

    public static SSLContext CreateSSlContext(Context ctx, boolean devTrusted){
        try {
            certificate = CertHelper.GetCertificate(ctx);

            PrivateKey privateKey = CertHelper.GetPrivateKey(ctx);


            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);
            keyStore.setKeyEntry("key", privateKey, "".toCharArray(), new Certificate[]{certificate});

            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmFactory.init(keyStore, "".toCharArray());


            SecureRandom secureRandom = new SecureRandom();


            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

            KeyManager[] keyManagers = kmFactory.getKeyManagers();

            if (devTrusted) {
                TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmFactory.init(keyStore);

                sslContext.init(keyManagers, tmFactory.getTrustManagers(), secureRandom);
            } else {
                TrustManager[] allTrust = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                sslContext.init(keyManagers, allTrust, secureRandom);

            }
            return sslContext;


        }catch(Exception e){
            Log.e("AConnect", "Failed to initialize ssl context");

        }
        return null;
    }

    public static void ConfigureSSlSocket(SSLSocket sslSocket, boolean isDevTrusted) throws java.net.SocketException{
        ArrayList<String> supportedCiphers = new ArrayList<>();
        supportedCiphers.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        sslSocket.setEnabledCipherSuites(supportedCiphers.toArray(new String[0]));
        sslSocket.setSoTimeout(5000);

        if(isDevTrusted){
            sslSocket.setWantClientAuth(true);
        }else{
            sslSocket.setNeedClientAuth(true);
        }
    }



}
