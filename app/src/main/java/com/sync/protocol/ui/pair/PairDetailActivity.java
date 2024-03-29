package com.sync.protocol.ui.pair;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.sync.lib.action.PairListener;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.process.Process;
import com.sync.lib.util.DataUtils;
import com.sync.protocol.R;
import com.sync.protocol.ui.PresentationActivity;
import com.sync.protocol.ui.ToastHelper;

import java.util.Calendar;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class PairDetailActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_detail);
        
        Intent intent = getIntent();
        String Device_name = intent.getStringExtra("device_name");
        String Device_id = intent.getStringExtra("device_id");
        PairDeviceInfo Device_info = new PairDeviceInfo(Device_name, Device_id);

        ImageView icon = findViewById(R.id.icon);
        ImageView batteryIcon = findViewById(R.id.batteryIcon);
        TextView deviceName = findViewById(R.id.deviceName);
        TextView deviceIdInfo = findViewById(R.id.deviceIdInfo);
        TextView batteryDetail = findViewById(R.id.batteryDetail);
        Button forgetButton = findViewById(R.id.forgetButton);
        Button findButton = findViewById(R.id.findButton);

        LinearLayout batterySaveEnabled = findViewById(R.id.batterySaveEnabled);
        LinearLayout batteryLayout = findViewById(R.id.batteryLayout);
        LinearLayout testSpeedLayout = findViewById(R.id.testSpeedLayout);
        LinearLayout remotePresentation = findViewById(R.id.remotePresentation);

        String[] colorLow = getResources().getStringArray(R.array.material_color_low);
        String[] colorHigh = getResources().getStringArray(R.array.material_color_high);
        int randomIndex = new Random(Device_name.hashCode()).nextInt(colorHigh.length);

        icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorHigh[randomIndex])));
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorLow[randomIndex])));
        deviceName.setText(Device_name);
        deviceIdInfo.setText("Device's unique address: " + Device_id);

        forgetButton.setOnClickListener(v -> {
            Process.requestRemovePair(Device_info);
            finish();
        });

        findButton.setOnClickListener(v -> {
            DataUtils.sendFindTaskNotification(Device_info);
            ToastHelper.show(this, "Your request is posted!","OK", ToastHelper.LENGTH_SHORT);
        });

        remotePresentation.setOnClickListener(v -> {
            Intent remotePresentationIntent = new Intent(this, PresentationActivity.class);
            remotePresentationIntent.putExtra("device_name", Device_name);
            remotePresentationIntent.putExtra("device_id", Device_id);
           startActivity(remotePresentationIntent);
        });

        AtomicLong currentTime = new AtomicLong();
        testSpeedLayout.setVisibility(getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE).getBoolean("printDebugLog", false) ? View.VISIBLE : View.GONE);
        testSpeedLayout.setOnClickListener((v) -> {
            currentTime.set(Calendar.getInstance().getTimeInMillis());
            DataUtils.requestData(Device_info, "speed_test");
        });

        PairListener.addOnDataReceivedListener(map -> {
            if(Device_info.equals(map.getDevice())) {
                switch (map.get(Value.REQUEST_DATA)) {
                    case "speed_test":
                        final long receivedTime = Long.parseLong(Objects.requireNonNull(map.get(Value.RECEIVE_DATA)));
                        final long arrivalTime = Calendar.getInstance().getTimeInMillis();

                        PairDetailActivity.this.runOnUiThread(() -> {
                            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(PairDetailActivity.this, R.style.MaterialAlertDialog_Material3));
                            dialog.setTitle("Test result");
                            dialog.setMessage("Send -> Receive: " + (receivedTime - currentTime.get()) + "\nReceive -> Send: " + (arrivalTime - receivedTime) + "\nTotal: " + (arrivalTime - currentTime.get()));
                            dialog.setIcon(R.drawable.ic_info_outline_black_24dp);
                            dialog.setPositiveButton("Close", (d, w) -> {});
                            dialog.show();
                        });
                        break;

                    case "battery_info":
                        String[] data = Objects.requireNonNull(map.get(Value.RECEIVE_DATA)).split("\\|");
                        int batteryInt = data[0].equals("undefined") ? 0 : Math.round(Float.parseFloat(data[0]));
                        int resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_warning_24_regular;

                        if(batteryInt < 10) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_0_24_regular;
                        else if(batteryInt < 20) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_1_24_regular;
                        else if(batteryInt < 30) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_2_24_regular;
                        else if(batteryInt < 40) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_3_24_regular;
                        else if(batteryInt < 50) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_4_24_regular;
                        else if(batteryInt < 60) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_5_24_regular;
                        else if(batteryInt < 70) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_6_24_regular;
                        else if(batteryInt < 80) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_7_24_regular;
                        else if(batteryInt < 90) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_8_24_regular;
                        else if(batteryInt < 100) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_9_24_regular;
                        else if(batteryInt == 100) resId = com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_battery_10_24_regular;

                        int finalResId = resId;
                        PairDetailActivity.this.runOnUiThread(() -> {
                            if(data[2].equals("true")) batterySaveEnabled.setVisibility(View.VISIBLE);
                            batteryLayout.setVisibility(View.VISIBLE);
                            batteryDetail.setText(data[0] + "% remaining" + (data[1].equals("true") ? ", Charging" : ""));
                            batteryIcon.setImageDrawable(AppCompatResources.getDrawable(PairDetailActivity.this, finalResId));
                        });
                        break;
                }
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((v) -> this.finish());
        DataUtils.requestData(Device_info, "battery_info");
    }
}
