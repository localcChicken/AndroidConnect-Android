package com.localchicken.androidconnect.Plugins.NotificationPlugin;

import android.os.Bundle;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.localchicken.androidconnect.Plugins.PluginBase;
import com.localchicken.androidconnect.TCP.MessageCodes;
import com.localchicken.androidconnect.TCP.TCPClient;
import com.localchicken.androidconnect.Plugins.PluginFactory;

import AndroidConnect.NotificationOuterClass;


@PluginFactory.Plugin
public class NotificationPlugin extends PluginBase implements NotificationListener.Listener {



    private boolean serviceReady = false;
    private int pKey = 0;


    @Override
    public boolean onCreate() {

        NotificationListener.Run(this.getCtx(), service -> {
            service.addListener(NotificationPlugin.this);

        });
        return true;
    }

    @Override
    public void onDestroy() {
        NotificationListener.Run(this.getCtx(), service -> service.removeListener(NotificationPlugin.this));
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        NotificationOuterClass.Notification.Builder builder = NotificationOuterClass.Notification.newBuilder();
        builder.setText(sbn.getNotification().extras.getString("android.text"));
        builder.setTitle(sbn.getNotification().extras.getString("android.title"));
        NotificationOuterClass.Notification notification = builder.build();
        byte[] notificationArr = notification.toByteArray();
        Log.e("Len ", Integer.toString(notificationArr.length));
        byte[] arrSize = { (byte)((notificationArr.length >> 8) & 0x000000FF), (byte)(notificationArr.length & 0x000000FF) };
        byte[] pType = { (byte)((getpKey() >> 8) & 0x000000FF), (byte)(getpKey() & 0x000000FF)};
        Bundle bundle = new Bundle();
        bundle.putByteArray(TCPClient.DataNames.SIZE, arrSize);
        bundle.putByteArray(TCPClient.DataNames.TYPE, pType);
        bundle.putByteArray(TCPClient.DataNames.DATA,  notificationArr);
        Message msg = getHandler().obtainMessage(MessageCodes.WRITE);
        msg.setData(bundle);
        getHandler().sendMessage(msg);
    }

    @Override
    public void onListenerConnected(NotificationListener service) {
        serviceReady = true;
    }

    @Override
    public void onListenerDisconnected(NotificationListener service) {
        serviceReady = false;
    }


}
