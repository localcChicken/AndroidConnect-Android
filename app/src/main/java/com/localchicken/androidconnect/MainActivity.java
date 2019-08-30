package com.localchicken.androidconnect;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.localchicken.androidconnect.Helpers.Security.CertHelper;
import com.localchicken.androidconnect.Plugins.PluginFactory;
import com.localchicken.androidconnect.TCP.MessageCodes;
import com.localchicken.androidconnect.TCP.TCPClient;
import com.localchicken.androidconnect.TCP.ThreadHandler;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(listener -> {
            CertHelper.InitKeypair(this);
            CertHelper.InitCert(this);


            TCPClient client = new TCPClient("Client");
            client.start();
            Looper clientLooper = client.getLooper();
            Handler clientHandler = new ThreadHandler(clientLooper, this);
            Message msg = clientHandler.obtainMessage(MessageCodes.CONNECT);
            Bundle data = new Bundle();
            EditText editText = findViewById(R.id.IP);
            data.putString("host",editText.getText().toString());
            data.putInt("port", 4908);
            msg.setData(data);
            clientHandler.sendMessage(msg);
        });
    }
}
