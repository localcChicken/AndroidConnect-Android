package com.localchicken.androidconnect.TCP;

import android.os.HandlerThread;

public class TCPClient extends HandlerThread {
    public static class DataNames {
        public static String SIZE = "dataSize";
        public static String TYPE = "dataType";
        public static String DATA = "data";
    }


    public TCPClient(String name){
        super(name);
    }

    @Override
    public void run() {
        super.run();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
    }
}