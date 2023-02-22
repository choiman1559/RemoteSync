package com.sync.protocol.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.util.DataUtils;
import com.sync.protocol.R;

import java.util.Random;

public class PresentationActivity extends AppCompatActivity {
    public static final String ACTION_NAME = "PRESENTATION_KEY_PRESSED";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        Intent intent = getIntent();
        String Device_name = intent.getStringExtra("device_name");
        String Device_id = intent.getStringExtra("device_id");
        PairDeviceInfo Device_info = new PairDeviceInfo(Device_name, Device_id);

        Button finishButton = findViewById(R.id.finishButton);
        Button startButton = findViewById(R.id.startButton);
        Button previousButton = findViewById(R.id.previousButton);
        Button nextButton = findViewById(R.id.nextButton);

        ImageView deviceIcon = findViewById(R.id.icon);
        TextView deviceName = findViewById(R.id.deviceName);

        finishButton.setOnClickListener(v -> DataUtils.requestAction(Device_info, ACTION_NAME, "escape"));
        startButton.setOnClickListener(v -> DataUtils.requestAction(Device_info, ACTION_NAME, "f5"));
        previousButton.setOnClickListener(v -> DataUtils.requestAction(Device_info, ACTION_NAME, "left"));
        nextButton.setOnClickListener(v -> DataUtils.requestAction(Device_info, ACTION_NAME, "right"));

        String[] colorLow = getResources().getStringArray(R.array.material_color_low);
        String[] colorHigh = getResources().getStringArray(R.array.material_color_high);
        int randomIndex = new Random(Device_name.hashCode()).nextInt(colorHigh.length);

        deviceIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorHigh[randomIndex])));
        deviceIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorLow[randomIndex])));
        deviceName.setText(Device_name);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((v) -> this.finish());
    }
}
