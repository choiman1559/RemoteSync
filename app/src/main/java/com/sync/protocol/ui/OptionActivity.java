package com.sync.protocol.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.sync.protocol.R;

public class OptionActivity extends AppCompatActivity {

    private static String title = "Default Message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        Fragment fragment;

        fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if(savedInstanceState == null || fragment == null) {
            fragment = new PairPreference();
            title = "Connection\npreferences";
        }

        Bundle bundle = new Bundle(0);
        fragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener((v) -> this.finish());
    }
}
