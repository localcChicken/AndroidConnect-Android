package com.localchicken.androidconnect.Plugins.NotificationPlugin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationListener extends NotificationListenerService {



    public interface Listener {
        void onNotificationPosted(StatusBarNotification sbn);

        void onListenerConnected(NotificationListener service);

        void onListenerDisconnected(NotificationListener service);
    }
    private final ArrayList<Listener> listeners = new ArrayList<>();





    public void addListener(Listener listener){
        listeners.add(listener);
    }

    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    @Override
    public void onListenerConnected() {
        for(Listener listener : listeners){
            listener.onListenerConnected(this);
        }

    }

    @Override
    public void onListenerDisconnected() {
        for(Listener listener : listeners){
            listener.onListenerDisconnected(this);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        for(Listener listener : listeners){
            listener.onNotificationPosted(sbn);
        }
    }

    public interface ICallback {
        void onStart(NotificationListener service);
    }
    private final static ArrayList<ICallback> callbacks = new ArrayList<>();
    private final static Lock mutex = new ReentrantLock(true);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mutex.lock();
        try {
            for (ICallback c : callbacks) {
                c.onStart(this);
            }
            callbacks.clear();
        }finally {
            mutex.unlock();
        }
        return Service.START_STICKY;
    }

    public static void Run(Context c, final ICallback callback){
        if(callback != null){
            mutex.lock();
            try{
                callbacks.add(callback);
            }finally {
                mutex.unlock();
            }
        }
        Intent serviceIntent = new Intent(c, NotificationListener.class);
        c.startService(serviceIntent);
    }
}
