package com.localchicken.androidconnect.Plugins;

import android.content.Context;
import android.os.Handler;


public class PluginBase {
    private Context ctx;
    private String pluginName;
    private int pKey;
    private Handler socketHandler;

    public void setCtx(Context ctx){
        this.ctx = ctx;
    }

    public Context getCtx(){
        return this.ctx;
    }

    public void setPluginName(String name){
        pluginName = name;
    }

    public String getPluginName() {
        return pluginName;
    }

    public int getpKey(){
        return this.pKey;
    }

    public boolean onCreate() {
        return true;
    }

    public void Received(int dataSize, byte[] data) { }

    public void setHandler(Handler handler) {
        this.socketHandler = handler;
    }

    public Handler getHandler() {
        return socketHandler;
    }

    public void onDestroy() {

    }

    private void reqPermDialog() {

    }



}
