package com.localchicken.androidconnect.Plugins.NotificationPlugin;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.localchicken.androidconnect.Plugins.PluginBase;
import com.localchicken.androidconnect.TCP.MessageCodes;
import com.localchicken.androidconnect.TCP.TCPClient;
import com.localchicken.androidconnect.Plugins.PluginFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import AndroidConnect.NotificationOuterClass;


@PluginFactory.Plugin
public class NotificationPlugin extends PluginBase implements NotificationListener.Listener {



    private boolean serviceReady = false;
    private int pKey = 0;
    private Map<String, RepliableNotification> RepliableList = new ConcurrentHashMap<>();
    private Map<String, Notification.Action> NonRepliableActions = new ConcurrentHashMap<>();
    //https://stackoverflow.com/questions/57591412/android-onnotificationposted-event-fires-twice-but-only-when-replying-to-notifi
    private boolean sendNext = true;
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
        Log.e("nPosted", "triggered");
        Notification recv_notification = sbn.getNotification();
        Log.e("nposted", Integer.toString(recv_notification.flags));
        if ((recv_notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0
                || (recv_notification.flags & Notification.FLAG_ONGOING_EVENT) != 0
                || (recv_notification.flags & Notification.FLAG_LOCAL_ONLY) != 0
                || (recv_notification.flags & NotificationCompat.FLAG_GROUP_SUMMARY) != 0) return;
        if(!sendNext){
            sendNext = true;
            return;
        }
        NotificationOuterClass.Notification.Builder builder = NotificationOuterClass.Notification.newBuilder();


        builder.setUndertext("AndroidConnect Beta Build!");

        Notification.Action[] actions = recv_notification.actions;
        if(actions != null) {
            for (Notification.Action action : actions) {
                String title = action.title.toString();
                NotificationOuterClass.NotificationActions.Builder actionsBuilder = NotificationOuterClass.NotificationActions.newBuilder();

                actionsBuilder.setContent(title);
                actionsBuilder.setAType("Background");
                actionsBuilder.setArgs("action=" + title.replace(' ', '_'));

                RepliableNotification repliableNotification = CreateReplNotification(action, sbn.getPackageName());
                if (repliableNotification != null) {
                    actionsBuilder.setIsRepliable(1);
                    RepliableList.put(title.replace(' ', '_'), repliableNotification);
                } else {
                    actionsBuilder.setIsRepliable(0);
                    NonRepliableActions.put(title.replace(' ', '_'), action);
                }
                builder.addNActions(actionsBuilder);
            }
            Pair<String, String> conv = getConv(recv_notification);
            builder.setTitle(conv.first == null ? getExtraString(recv_notification.extras, NotificationCompat.EXTRA_TITLE) : conv.first);
            builder.setText(getConvText(recv_notification, conv));
            Log.e("Text: ", getConvText(recv_notification, conv));
        }else {
            Bundle extras = recv_notification.extras;
            String bigTEXT = getExtraString(extras, Notification.EXTRA_BIG_TEXT), bigTITLE = getExtraString(extras, Notification.EXTRA_TITLE_BIG);

            builder.setText(bigTEXT == null ? getExtraString(extras, Notification.EXTRA_TEXT) : bigTEXT);
            builder.setTitle(bigTITLE == null ? getExtraString(extras, Notification.EXTRA_TITLE) : bigTITLE);
        }
        //Adding everything before this line
        NotificationOuterClass.Notification notification = builder.build();


        //Calculating and sending
        byte[] notificationArr = notification.toByteArray();
        Log.e("Len ", Integer.toString(notificationArr.length));
        byte[] arrSize = { (byte)((notificationArr.length >> 8) & 0x000000FF), (byte)(notificationArr.length & 0x000000FF) };

        byte[] pType = { (byte)((getpKey() >> 8) & 0x000000FF), (byte)(getpKey() & 0x000000FF)};
        String testStr = Integer.toString(((pType)[0] << 8) | pType[1]);
        Log.e("Ss", testStr);
        Log.e("ArrSize", Integer.toString(notificationArr.length));
        Bundle bundle = new Bundle();
        bundle.putByteArray(TCPClient.DataNames.SIZE, arrSize);
        bundle.putByteArray(TCPClient.DataNames.TYPE, pType);
        bundle.putByteArray(TCPClient.DataNames.DATA,  notificationArr);
        Message msg = getHandler().obtainMessage(MessageCodes.WRITE);
        msg.setData(bundle);
        getHandler().sendMessage(msg);
        Log.e("I", "send!");
    }

    private String getConvText(Notification notification, Pair<String, String> conv){
        if(conv.second != null) return conv.second;
        Bundle extras = notification.extras;
        if(extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) return getExtraString(extras, NotificationCompat.EXTRA_BIG_TEXT);
        return getExtraString(extras, NotificationCompat.EXTRA_TEXT);
    }

    private Pair<String, String> getConv(Notification notification){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return new Pair<>(null, null);
        if(!notification.extras.containsKey(Notification.EXTRA_MESSAGES)) return new Pair<>(null, null);

        Parcelable[] parcelables = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if(parcelables == null) return new Pair<>(null, null);
        boolean isGroup = notification.extras.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION);
        String title = notification.extras.getString(Notification.EXTRA_CONVERSATION_TITLE);
        StringBuilder sb = new StringBuilder();
        Bundle mBundle = (Bundle)parcelables[parcelables.length - 1];
        Object sender = getExtraString(mBundle, "sender");

        if(isGroup) {
            if (sender != null) {
                sb.append(getExtraString(mBundle, "text"));
            }
        }else{
            sb.append(getExtraString(mBundle, "text"));
        }
        return new Pair<>(title, sb.toString());
    }

    private static String getExtraString(Bundle b, String key){
        Object extra = b.get(key);
        if(key == null) {
            return null;
        }else if(extra instanceof String){
            return (String)extra;
        }else if(extra instanceof SpannableString){
            return extra.toString();
        }
        return null;

    }

    private RepliableNotification CreateReplNotification(Notification.Action action, String packageName){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        if(action != null && action.getRemoteInputs() != null){
                RepliableNotification rNot = new RepliableNotification();
                rNot.appName = packageName;
                rNot.pendingIntent = action.actionIntent;
                rNot.remoteInputs.addAll(Arrays.asList(action.getRemoteInputs()));
                return rNot;
            }

        return null;
    }

    private void ReplyToNotification(String key, String value){

        if(RepliableList.isEmpty() || !RepliableList.containsKey(key)) {
            Log.e("AC/Reply", "No such notification!");
            return;
        }
        RepliableNotification repliableNotification = RepliableList.get(key);
        if(repliableNotification == null) {
            Log.e("AC/Reply", "No such notification!");
            return;
        }
        RemoteInput[] inputs = new RemoteInput[repliableNotification.remoteInputs.size()];

        Bundle message = new Bundle();
        int index = 0;
        sendNext = false;
        for(RemoteInput input : repliableNotification.remoteInputs){
            inputs[index] = input;
            message.putCharSequence(inputs[index++].getResultKey(), value);
        }
        Intent replyIntent = new Intent();
        replyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        RemoteInput.addResultsToIntent(inputs, replyIntent, message);
        try {
            repliableNotification.pendingIntent.send(this.getCtx(), 0, replyIntent);
        }catch(Exception e){
            e.printStackTrace();
        }

        RepliableList.remove(key);

    }

    @Override
    public void Received(int dataSize, byte[] data) {

        try {
            NotificationOuterClass.Notification notification = NotificationOuterClass.Notification.parseFrom(data);
            if(notification.hasReply()){
                NotificationOuterClass.NotificationReply reply = notification.getReply();
                ReplyToNotification(notification.getText()  , reply.getReplyvalue());
            }else{
                Objects.requireNonNull(NonRepliableActions.get(notification.getText())).actionIntent.send();
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        //Handle
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
