package com.localchicken.androidconnect.TCP;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import androidx.annotation.NonNull;

import com.localchicken.androidconnect.Helpers.Security.CertHelper;
import com.localchicken.androidconnect.Helpers.Security.SSLHelper;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ThreadHandler extends Handler {
    private SSLSocket sslSocket;
    private boolean connected = false;
    //TODO: callbacks
    private InputStream is;
    private OutputStream os;
    private Context ctx;
    public ThreadHandler(Looper looper, Context ctx){
        super(looper);
        this.ctx = ctx;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Bundle data = msg.getData();
        if(msg.what == MessageCodes.CONNECT){
            try {
                Connect(InetAddress.getByName(data.getString("host")), data.getInt("port"));
            }catch(Exception e){
                e.printStackTrace();
            }
        }else if(msg.what == MessageCodes.DISCONNECT){
            Disconnect();
        }else if(msg.what == MessageCodes.WRITE){
            if(connected){
                try {
                    os.write(Objects.requireNonNull(msg.getData().getByteArray(TCPClient.DataNames.SIZE)));
                    os.write(Objects.requireNonNull(msg.getData().getByteArray(TCPClient.DataNames.TYPE)));
                    os.write(Objects.requireNonNull(msg.getData().getByteArray(TCPClient.DataNames.DATA)));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        super.handleMessage(msg);

    }

    private void Connect(InetAddress ip, int port) {
        try {
            CertHelper.InitCert(ctx);
            CertHelper.InitKeypair(ctx);
            SSLContext sslContext = SSLHelper.CreateSSlContext(ctx, false);
            assert sslContext != null;
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            sslSocket = (SSLSocket)sslSocketFactory.createSocket();
            sslSocket.addHandshakeCompletedListener(completed -> {
                try {
                    os = completed.getSocket().getOutputStream();
                    is = completed.getSocket().getInputStream();
                    connected = true;
                }catch(Exception e){
                    e.printStackTrace();
                }
            });
            SSLHelper.ConfigureSSlSocket(sslSocket, false);
            sslSocket.connect(new InetSocketAddress(ip, port));
            sslSocket.startHandshake();
            /*Certificate[] certificates = sslSocket.getSession().getPeerCertificates();
            for(Certificate certificate : certificates){
                Log.e("Cert:", certificate.toString());
            }
*/

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void Disconnect(){
        is = null;
        os = null;
        try {
            sslSocket.close();
            connected = false;
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
