package com.localchicken.androidconnect.Plugins;

import android.content.Context;
import android.os.Handler;

import org.atteo.classindex.ClassIndex;
import org.atteo.classindex.IndexAnnotated;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PluginFactory {
   @IndexAnnotated
   public @interface Plugin{}

   public static class PluginInfo {
       PluginInfo(Context ctx, String name, Handler handler, Class<? extends PluginBase> iClass){
           this.ctx = ctx;
           this.name = name;
           this.handler = handler;
           this.iClass = iClass;
       }

       public Context getCtx() {
           return this.ctx;
       }

       public String getName() {
           return this.name;
       }

       public Handler getHandler() {
           return handler;
       }

       Class<? extends PluginBase> getIClass() {
           return iClass;
       }

       private final String name;
       private final Context ctx;
       private final Handler handler;
       private final Class<? extends PluginBase> iClass;
   }

   private static final Map<Integer, PluginInfo> pInfo = new ConcurrentHashMap<>();

   public static PluginInfo getPluginInfo(int pKey){
       return pInfo.get(pKey);
   }

    public static void initPluginInfo(Context ctx, Handler handler){
       try{
           for(Class<?> pluginClass : ClassIndex.getAnnotated(Plugin.class)){
               PluginBase p = ((PluginBase) pluginClass.newInstance());
               p.setCtx(ctx);
               p.setHandler(handler);
               PluginInfo info = new PluginInfo(p.getCtx(), p.getPluginName(), p.getHandler(), p.getClass());
               pInfo.put(p.getpKey(), info);
           }
       }catch(Exception e){
           throw new RuntimeException(e);
       }
    }

    public static Set<Integer> getAvaidlablePlugins() {
       return pInfo.keySet();
    }

    public static PluginBase instantinatePlugin(Context ctx, Handler handler, int pKey){
       PluginInfo info = pInfo.get(pKey);
       try{
           assert info != null;
           PluginBase plugin = info.getIClass().newInstance();
           plugin.setCtx(ctx);
           plugin.setHandler(handler);
           return plugin;
       }catch(Exception e){
           return null;
       }
    }


}
