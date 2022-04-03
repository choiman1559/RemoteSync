package com.sync.protocol.ui;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kieronquinn.monetcompat.core.MonetCompat;
import com.sync.protocol.R;
import com.sync.protocol.utils.DataUtils;

public class PairPreference extends PreferenceFragmentCompat  {

    Activity mContext;
    MonetCompat monet;
    SharedPreferences prefs;

    Preference UseDataEncryption;
    Preference UseDataEncryptionPassword;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MonetCompat.setup(requireContext());
        monet = MonetCompat.getInstance();
        monet.updateMonetColors();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        monet = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) mContext = (Activity) context;
        else throw new RuntimeException("Can't get Activity instanceof Context!");
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pair_preferences, rootKey);
        prefs = mContext.getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE);

        UseDataEncryption = findPreference("UseDataEncryption");
        UseDataEncryptionPassword = findPreference("UseDataEncryptionPassword");

        boolean usesDataEncryption = prefs.getBoolean("UseDataEncryption", false);
        UseDataEncryptionPassword.setVisible(usesDataEncryption);
        UseDataEncryption.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean foo = (boolean) newValue;
            UseDataEncryptionPassword.setVisible(foo);
            return true;
        });
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        MaterialAlertDialogBuilder dialog;
        EditText editText;
        LinearLayout parentLayout;
        LinearLayout.LayoutParams layoutParams;

        switch(preference.getKey()) {
            case "findPhone":
                if(!prefs.getString("UID","").isEmpty()) {
                    DataUtils.sendFindTaskNotification(mContext);
                    ToastHelper.show(mContext, "Your request is posted!","OK", ToastHelper.LENGTH_SHORT);
                } else ToastHelper.show(mContext, "Please check your account status and try again!","Dismiss", ToastHelper.LENGTH_SHORT);
                break;

            case "PairingKey":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setIcon(com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_edit_24_regular);
                dialog.setCancelable(false);
                dialog.setTitle("Input Pairing key");
                dialog.setMessage("Enter the password to be used for pairing.\nPairing key is limited to a maximum of 30 characters.");

                editText = new EditText(mContext);
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                editText.setHint("Input Pairing key");
                editText.setGravity(Gravity.START);
                editText.setText(prefs.getString("UID", ""));

                parentLayout = new LinearLayout(mContext);
                layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(30, 16, 30, 16);
                editText.setLayoutParams(layoutParams);
                parentLayout.addView(editText);
                dialog.setView(parentLayout);

                dialog.setPositiveButton("Apply", (d, w) -> {
                    String value = editText.getText().toString().trim();
                    if (value.equals("")) {
                        ToastHelper.show(mContext, "Please Input key","DISMISS", ToastHelper.LENGTH_SHORT);
                    } else if(value.length() > 31) {
                        ToastHelper.show(mContext, "Pairing key too long! maximum 30 chars.", "DISMISS",ToastHelper.LENGTH_SHORT);
                    } else {
                        prefs.edit().putString("UID", value).apply();
                        FirebaseMessaging.getInstance().subscribeToTopic(value);
                    }
                });
                dialog.setNegativeButton("Cancel", (d, w) -> { });
                dialog.show();
                break;

            case "UseDataEncryptionPassword":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setIcon(com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_edit_24_regular);
                dialog.setCancelable(false);
                dialog.setTitle("Input password");
                dialog.setMessage("Enter the password to be used for encryption.\nPassword is limited to a maximum of 20 characters.");

                editText = new EditText(mContext);
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                editText.setHint("Input password");
                editText.setGravity(Gravity.START);
                editText.setText(prefs.getString("EncryptionPassword", ""));

                parentLayout = new LinearLayout(mContext);
                layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(30, 16, 30, 16);
                editText.setLayoutParams(layoutParams);
                parentLayout.addView(editText);
                dialog.setView(parentLayout);

                dialog.setPositiveButton("Apply", (d, w) -> {
                    String value = editText.getText().toString();
                    if (value.equals("")) {
                        ToastHelper.show(mContext, "Please Input password","DISMISS", ToastHelper.LENGTH_SHORT);
                    } else if(value.length() > 20) {
                        ToastHelper.show(mContext, "Password too long! maximum 20 chars.", "DISMISS",ToastHelper.LENGTH_SHORT);
                    } else {
                        prefs.edit().putString("EncryptionPassword", value).apply();
                    }
                });
                dialog.setNeutralButton("Reset Default", (d, w) -> prefs.edit().remove("EncryptionPassword").apply());
                dialog.setNegativeButton("Cancel", (d, w) -> { });
                dialog.show();
                break;

            case "TaskerCompatibleInfo":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setTitle("Tasker compatible apps");
                dialog.setMessage(getString(R.string.Dialog_Tasker_compatible));
                dialog.setIcon(R.drawable.ic_info_outline_black_24dp);
                dialog.setPositiveButton("Close", (d, w) -> {
                });
                dialog.show();
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
