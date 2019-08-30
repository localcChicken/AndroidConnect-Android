package com.localchicken.androidconnect.Plugins.NotificationPlugin;

import android.app.PendingIntent;
import android.app.RemoteInput;
import java.util.List;
import java.util.ArrayList;
public class RepliableNotification {
    public PendingIntent pendingIntent;
    public List<RemoteInput> remoteInputs = new ArrayList<>();
    String appName;
}
