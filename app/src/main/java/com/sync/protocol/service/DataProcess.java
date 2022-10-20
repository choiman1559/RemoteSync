package com.sync.protocol.service;

import static android.content.Context.MODE_PRIVATE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.application.isradeleon.notify.Notify;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.util.DataUtils;
import com.sync.protocol.R;
import com.sync.protocol.ui.pair.PairAcceptActivity;
import com.sync.protocol.utils.AsyncTask;
import com.sync.protocol.utils.PowerUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

public class DataProcess {
    public static void onDataRequested(Data map, Context context) {
        String dataType = map.get(Value.REQUEST_DATA);
        String dataToSend = "";
        if (dataType != null) {
            switch (dataType) {
                case "battery_info":
                    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = context.registerReceiver(null, filter);
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                    int batteryPct = (int) (level * 100 / (float) scale);
                    boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                    boolean isBatterySaver = powerManager.isPowerSaveMode();

                    dataToSend = batteryPct + "|" + isCharging + "|" + isBatterySaver;
                    break;

                case "speed_test":
                    dataToSend = Long.toString(Calendar.getInstance().getTimeInMillis());
                    break;
            }
        }

        if (!dataToSend.isEmpty()) {
            DataUtils.responseDataRequest(map.getDevice(), dataType, dataToSend, context);
        }
    }

    public static void onActionRequested(Data map, Context context) {
        PowerUtils.getInstance(context).acquire();
        PairDeviceInfo device = map.getDevice();
        String actionType = map.get(Value.REQUEST_ACTION);
        String actionArg = map.get(Value.ACTION_ARGS);
        String[] actionArgs = {};

        if (actionArg != null) {
            actionArgs = actionArg.split("\\|");
        }

        if (actionType != null) {
            switch (actionType) {
                case "Show notification with text":
                    Notify.build(context)
                            .setTitle(actionArgs[0])
                            .setContent(actionArgs[1])
                            .setLargeIcon(R.mipmap.ic_launcher)
                            .largeCircularIcon()
                            .setSmallIcon(R.drawable.ic_broken_image)
                            .setChannelName("")
                            .setChannelId("Notification Test")
                            .enableVibration(true)
                            .setAutoCancel(true)
                            .show();
                    break;

                case "Copy text to clipboard":
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Shared from " + device.getDevice_name(), actionArgs[0]);
                    clipboard.setPrimaryClip(clip);
                    break;

                case "Open link in Browser":
                    String url = actionArgs[0];
                    if (!url.startsWith("http://") && !url.startsWith("https://"))
                        url = "http://" + url;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;

                case "Trigger tasker event":
                    TaskerPairEventKt.callTaskerEvent(device.getDevice_name(), device.getDevice_id(), context);
                    break;

                case "Run application":
                    new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(context, "Remote run by SyncProtocol\nfrom " + device.getDevice_name(), Toast.LENGTH_SHORT).show(), 0);
                    String Package = actionArgs[0].trim();
                    try {
                        PackageManager pm = context.getPackageManager();
                        pm.getPackageInfo(Package, PackageManager.GET_ACTIVITIES);
                        Intent intent = pm.getLaunchIntentForPackage(Package);
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Package));
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;

                case "Run command":
                    final String[] finalActionArgs = actionArgs;
                    new Thread(() -> {
                        try {
                            if (finalActionArgs.length > 0)
                                Runtime.getRuntime().exec(finalActionArgs);
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;

                case "Share file":
                    int notificationId = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
                    String notificationChannel = "DownloadFile";
                    NotificationManager mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(notificationChannel, "Download File Notification", NotificationManager.IMPORTANCE_DEFAULT);
                        mNotifyManager.createNotificationChannel(channel);
                    }

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, notificationChannel)
                            .setContentTitle("File Download")
                            .setContentText("File name: " + actionArg)
                            .setSmallIcon(com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_arrow_download_24_regular)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setOnlyAlertOnce(true)
                            .setGroupSummary(false)
                            .setOngoing(true)
                            .setAutoCancel(false);
                    mBuilder.setProgress(0, 0, true);
                    mNotifyManager.notify(notificationId, mBuilder.build());

                    new Thread(() -> {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference();
                        StorageReference fileRef = storageRef.child(context.getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE).getString("UID", "") + "/" + actionArg);

                        try {
                            if (Build.VERSION.SDK_INT < 29) {
                                File targetFile = new File(Environment.getExternalStorageDirectory(), "Download/NotiSender/" + actionArg);
                                targetFile.mkdirs();
                                if (targetFile.exists()) targetFile.delete();
                                targetFile.createNewFile();
                                FileDownloadTask task = fileRef.getFile(targetFile);

                                task.addOnSuccessListener(taskSnapshot -> {
                                    mBuilder.setContentText(actionArg + " download completed.\nCheck download folder!")
                                            .setProgress(0, 0, false)
                                            .setOngoing(false);
                                    mNotifyManager.notify(notificationId, mBuilder.build());
                                });

                                task.addOnFailureListener(exception -> {
                                    mBuilder.setContentText(actionArg + " download failed")
                                            .setProgress(0, 0, false)
                                            .setOngoing(false);
                                    mNotifyManager.notify(notificationId, mBuilder.build());
                                });

                                task.addOnProgressListener(snapshot -> {
                                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                    mBuilder.setProgress(100, (int) progress, false);
                                    mNotifyManager.notify(notificationId, mBuilder.build());
                                });
                            } else {
                                fileRef.getMetadata().addOnSuccessListener(storageMetadata -> {
                                    ContentResolver resolver = context.getContentResolver();
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(MediaStore.Downloads.DISPLAY_NAME, actionArg);
                                    contentValues.put(MediaStore.Downloads.RELATIVE_PATH, "Download/" + "SyncProtocol");
                                    contentValues.put(MediaStore.Downloads.MIME_TYPE, storageMetadata.getContentType());
                                    contentValues.put(MediaStore.Downloads.IS_PENDING, true);
                                    Uri uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                                    Uri itemUri = resolver.insert(uri, contentValues);

                                    fileRef.getStream().addOnSuccessListener(stream -> {
                                        if (itemUri != null) {
                                            AsyncTask<Void, Void, Void> downloadTask = new AsyncTask<>() {
                                                @Override
                                                protected Void doInBackground(Void... voids) {
                                                    try {
                                                        InputStream inputStream = stream.getStream();
                                                        OutputStream outputStream = resolver.openOutputStream(itemUri);
                                                        byte[] buffer = new byte[102400];
                                                        int len;
                                                        while ((len = inputStream.read(buffer)) > 0) {
                                                            outputStream.write(buffer, 0, len);
                                                        }
                                                        outputStream.close();

                                                        contentValues.put(MediaStore.Images.Media.IS_PENDING, false);
                                                        resolver.update(itemUri, contentValues, null, null);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    return null;
                                                }

                                                @Override
                                                protected void onPostExecute(Void unused) {
                                                    super.onPostExecute(unused);
                                                    mBuilder.setContentText(actionArg + " download completed.\nCheck download folder!")
                                                            .setProgress(0, 0, false)
                                                            .setOngoing(false);
                                                    mNotifyManager.notify(notificationId, mBuilder.build());
                                                }
                                            };
                                            downloadTask.execute();
                                        }
                                    }).addOnFailureListener(e -> {
                                        e.printStackTrace();
                                        mBuilder.setContentText(actionArg + " download failed")
                                                .setProgress(0, 0, false)
                                                .setOngoing(false);
                                        mNotifyManager.notify(notificationId, mBuilder.build());
                                    });
                                }).addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    mBuilder.setContentText(actionArg + " download failed")
                                            .setProgress(0, 0, false)
                                            .setOngoing(false);
                                    mNotifyManager.notify(notificationId, mBuilder.build());
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            mNotifyManager.cancel(notificationId);
                        }
                    }).start();
                    break;
            }
        }
    }

    public static void showPairChoiceAction(Data map, Context context) {
        int uniqueCode = (int) (Calendar.getInstance().getTime().getTime() / 1000L % Integer.MAX_VALUE);
        PairDeviceInfo device = map.getDevice();

        Intent notificationIntent = new Intent(context, PairAcceptActivity.class);
        notificationIntent.putExtra("device_name", device.getDevice_name());
        notificationIntent.putExtra("device_id", device.getDevice_id());

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueCode, notificationIntent, Build.VERSION.SDK_INT > 30 ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.notify_channel_id))
                .setContentTitle("New pair request incoming!")
                .setContentText("click here to pair device")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_arrow_sync_checkmark_24_regular))
                .setGroup(context.getPackageName() + ".NOTIFICATION")
                .setGroupSummary(true)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setSmallIcon(R.drawable.ic_notification);
            CharSequence channelName = context.getString(R.string.notify_channel_name);
            String description = context.getString(R.string.notify_channel_description);
            NotificationChannel channel = new NotificationChannel(context.getString(R.string.notify_channel_id), channelName, NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(description);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        } else builder.setSmallIcon(R.mipmap.ic_notification);

        assert notificationManager != null;
        notificationManager.notify((int)((new Date().getTime() / 1000L) % Integer.MAX_VALUE), builder.build());
    }
}
