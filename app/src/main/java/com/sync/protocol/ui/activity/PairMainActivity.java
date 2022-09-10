package com.sync.protocol.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.kieronquinn.monetcompat.app.MonetCompatActivity;
import com.kieronquinn.monetcompat.view.MonetSwitch;
import com.sync.lib.Protocol;
import com.sync.lib.action.PairListener;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.protocol.R;
import com.sync.protocol.ui.OptionActivity;
import com.sync.protocol.ui.RequestActionActivity;

import java.util.ArrayList;
import java.util.Random;

@SuppressLint("SetTextI18n")
public class PairMainActivity extends MonetCompatActivity {

    SharedPreferences prefs;
    LinearLayout deviceListLayout;
    PairListener.onDeviceListChangedListener onChange = (list -> PairMainActivity.this.runOnUiThread(this::loadDeviceList));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_main);

        LinearLayout addNewDevice = findViewById(R.id.addNewDevice);
        LinearLayout connectionPreference = findViewById(R.id.connectionPreference);
        TextView deviceNameInfo = findViewById(R.id.deviceNameInfo);
        MonetSwitch PairingSwitch = findViewById(R.id.PairingSwitch);
        deviceListLayout = findViewById(R.id.deviceListLayout);
        prefs = getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE);

        deviceNameInfo.setText("Visible as \"" + Build.MODEL + "\" to other devices");
        addNewDevice.setOnClickListener(v -> startActivity(new Intent(this, PairingActivity.class)));
        connectionPreference.setOnClickListener(v -> startActivity(new Intent(this, OptionActivity.class).putExtra("Type", "Pair")));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((v) -> this.finish());

        PairingSwitch.setChecked(prefs.getBoolean("ServiceToggle", false));
        PairingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("ServiceToggle", isChecked).apply());

        loadDeviceList();
        PairListener.setOnDeviceListChangedListener(onChange);
    }

    void loadDeviceList() {
        ArrayList<PairDeviceInfo> list = Protocol.getPairedDeviceList();
        if(list.size() == deviceListLayout.getChildCount()) return;
        deviceListLayout.removeViews(0, deviceListLayout.getChildCount());

        for(PairDeviceInfo device : list) {
            RelativeLayout layout = (RelativeLayout) View.inflate(PairMainActivity.this, R.layout.cardview_pair_device_setting, null);
            Holder holder = new Holder(layout);

            String[] colorLow = PairMainActivity.this.getResources().getStringArray(R.array.material_color_low);
            String[] colorHigh = PairMainActivity.this.getResources().getStringArray(R.array.material_color_high);
            int randomIndex = new Random(device.getDevice_name().hashCode()).nextInt(colorHigh.length);

            holder.deviceName.setText(device.getDevice_name());
            holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorHigh[randomIndex])));
            holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorLow[randomIndex])));
            holder.setting.setOnClickListener(v -> {
                Intent intent = new Intent(this, PairDetailActivity.class);
                intent.putExtra("device_name", device.getDevice_name());
                intent.putExtra("device_id", device.getDevice_id());
                startActivity(intent);
            });
            holder.baseLayout.setOnClickListener(v -> {
                Intent intent = new Intent(this, RequestActionActivity.class);
                intent.putExtra("device_name", device.getDevice_name());
                intent.putExtra("device_id", device.getDevice_id());
                startActivity(intent);
            });

            deviceListLayout.addView(layout);
        }
    }

    static class Holder {
        TextView deviceName;
        TextView pairStatus;
        RelativeLayout baseLayout;
        ImageView icon;
        ImageView setting;

        Holder(View view) {
            deviceName = view.findViewById(R.id.deviceName);
            pairStatus = view.findViewById(R.id.deviceStatus);
            baseLayout = view.findViewById(R.id.baseLayout);
            icon = view.findViewById(R.id.icon);
            setting = view.findViewById(R.id.deviceDetail);
        }
    }
}
