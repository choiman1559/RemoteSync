package com.sync.protocol.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.sync.lib.Protocol;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.process.Process;
import com.sync.protocol.R;
import com.sync.protocol.ui.ExitActivity;

import java.util.HashSet;
import java.util.Set;

public class PairAcceptActivity extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_accept);
        Intent intent = getIntent();
        MaterialButton AcceptButton = findViewById(R.id.ok);
        MaterialButton CancelButton = findViewById(R.id.cancel);

        String Device_name = intent.getStringExtra("device_name");
        String Device_id = intent.getStringExtra("device_id");

        TextView info = findViewById(R.id.notiDetail);
        info.setText(Html.fromHtml("Are you sure you want to grant the pairing request?<br><b>Requested Device:</b> " + Device_name));

        AcceptButton.setOnClickListener(v -> {
            sendAcceptedMessage(Device_name, Device_id, true, this);
            SharedPreferences prefs = getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
            boolean isNotRegistered = true;
            String dataToSave = Device_name + "|" + Device_id;

            Set<String> list = new HashSet<>(prefs.getStringSet("paired_list", new HashSet<>()));
            for(String str : list) {
                if(str.equals(dataToSave)) {
                    isNotRegistered = false;
                    break;
                }
            }

            if(isNotRegistered) {
                list.add(dataToSave);
                prefs.edit().putStringSet("paired_list", list).apply();
            }
        });
        CancelButton.setOnClickListener(v -> sendAcceptedMessage(Device_name, Device_id, false, this));
    }

    public static void sendAcceptedMessage(String Device_name, String Device_id, boolean isAccepted, Context context) {
        Process.responsePairAcceptation(new PairDeviceInfo(Device_name, Device_id), isAccepted, context);
        ExitActivity.exitApplication(context);

        if(isAccepted) {
            for(PairDeviceInfo info : Protocol.pairingProcessList) {
                if(info.getDevice_name().equals(Device_name) && info.getDevice_id().equals(Device_id)) {
                    Protocol.isListeningToPair = false;
                    Protocol.pairingProcessList.remove(info);
                    break;
                }
            }
        }
    }
}
