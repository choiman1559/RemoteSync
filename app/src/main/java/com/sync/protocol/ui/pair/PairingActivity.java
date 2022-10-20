package com.sync.protocol.ui.pair;

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
import com.sync.lib.data.Value;
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
            PairDeviceInfo device = map.getDevice();
            if(device.getDevice_name() != null && device.getDevice_id() != null) {
                boolean isDeviceNotQueried = true;
                for(PairDeviceInfo info : infoList) {
                    if(info.equals(device)) {
                        isDeviceNotQueried = false;
                        break;
                    }
                }

                if(isDeviceNotQueried) {
                    infoList.add(device.setDevice_status(PairDeviceStatus.Device_Process_Pairing));
                    notifyDataSetChanged(device);
                }
            }
        }));

        PairListener.setOnDevicePairResultListener(map -> {
            for(int i = 0;i < infoList.size(); i++) {
                PairDeviceInfo info = infoList.get(i);
                PairDeviceInfo device = map.getDevice();

                if(info.equals(device)) {
                    int finalI = i;
                    PairingActivity.this.runOnUiThread(() -> {
                        RelativeLayout view = (RelativeLayout) deviceListLayout.getChildAt(finalI);
                        Holder holder = new Holder(view);
                        if("false".equals(map.get(Value.PAIR_ACCEPT))) {
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

    public void notifyDataSetChanged(PairDeviceInfo device) {
        RelativeLayout layout = (RelativeLayout) View.inflate(PairingActivity.this, R.layout.cardview_pair_device, null);
        Holder holder = new Holder(layout);

        String[] colorLow = PairingActivity.this.getResources().getStringArray(R.array.material_color_low);
        String[] colorHigh = PairingActivity.this.getResources().getStringArray(R.array.material_color_high);
        int randomIndex = new Random(device.getDevice_name().hashCode()).nextInt(colorHigh.length);

        holder.deviceName.setText(device.getDevice_name());
        holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorHigh[randomIndex])));
        holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorLow[randomIndex])));

        layout.setOnClickListener(v -> {
            Process.requestPair(device, PairingActivity.this);
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
