package com.sync.protocol.ui.activity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.PairDeviceStatus;
import com.sync.lib.action.PairListener;
import com.sync.lib.process.Process;
import com.sync.protocol.R;
import com.sync.protocol.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressLint("SetTextI18n")
public class PairingActivity extends AppCompatActivity {

    ProgressBar progress;
    LinearLayout deviceListLayout;
    final List<PairDeviceInfo> infoList  = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_find);

        TextView deviceName = findViewById(R.id.deviceNameInfo);
        TextView deviceId = findViewById(R.id.deviceIdInfo);

        progress = findViewById(R.id.progress);
        deviceListLayout = findViewById(R.id.deviceListLayout);

        PairListener.setOnDeviceFoundListener(map -> PairingActivity.this.runOnUiThread(() -> {
            String[] data = {map.get("device_name"), map.get("device_id")};
            if(data[0] != null && data[1] != null) {
                boolean isDeviceNotQueried = true;
                for(PairDeviceInfo info : infoList) {
                    if(info.getDevice_name().equals(data[0]) && info.getDevice_id().equals(data[1])) {
                        isDeviceNotQueried = false;
                        break;
                    }
                }

                if(isDeviceNotQueried) {
                    infoList.add(new PairDeviceInfo(data[0], data[1], PairDeviceStatus.Device_Process_Pairing));
                    notifyDataSetChanged(data[0], data[1]);
                }
            }
        }));

        PairListener.setOnDevicePairResultListener(map -> {
            for(int i = 0;i < infoList.size(); i++) {
                PairDeviceInfo info = infoList.get(i);
                if(info.getDevice_name().equals(map.get("device_name")) && info.getDevice_id().equals(map.get("device_id"))) {
                    int finalI = i;
                    PairingActivity.this.runOnUiThread(() -> {
                        RelativeLayout view = (RelativeLayout) deviceListLayout.getChildAt(finalI);
                        Holder holder = new Holder(view);
                        if("false".equals(map.get("pair_accept"))) {
                            holder.pairStatus.setText("Failed");
                        } else PairingActivity.this.finish();
                    });
                }
            }
        });

        Process.requestDeviceListWidely(this);
        deviceName.setText(Build.MODEL);
        deviceId.setText("Phone's Unique address: " + DataUtils.getUniqueID(this));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((v) -> this.finish());
    }

    public void notifyDataSetChanged(String Device_name, String Device_id) {
        RelativeLayout layout = (RelativeLayout) View.inflate(PairingActivity.this, R.layout.cardview_pair_device, null);
        Holder holder = new Holder(layout);

        String[] colorLow = PairingActivity.this.getResources().getStringArray(R.array.material_color_low);
        String[] colorHigh = PairingActivity.this.getResources().getStringArray(R.array.material_color_high);
        int randomIndex = new Random(Device_name.hashCode()).nextInt(colorHigh.length);

        holder.deviceName.setText(Device_name);
        holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorHigh[randomIndex])));
        holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorLow[randomIndex])));

        layout.setOnClickListener(v -> {
            Process.requestPair(new PairDeviceInfo(Device_name, Device_id), PairingActivity.this);
            progress.setVisibility(View.GONE);

            holder.pairStatus.setText("Connecting...");
            holder.pairStatus.setVisibility(View.VISIBLE);
        });

        deviceListLayout.addView(layout);
    }

    static class Holder {
        TextView deviceName;
        TextView pairStatus;
        RelativeLayout baseLayout;
        ImageView icon;

        Holder(View view) {
            deviceName = view.findViewById(R.id.deviceName);
            pairStatus = view.findViewById(R.id.deviceStatus);
            baseLayout = view.findViewById(R.id.baseLayout);
            icon = view.findViewById(R.id.icon);
        }
    }
}
