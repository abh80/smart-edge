package com.abh80.smartedge.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.abh80.smartedge.BuildConfig;
import com.abh80.smartedge.R;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class UpdaterService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + ".START_UPDATE")) {
                if (download_url != null) {
                    new DownloadFileFromURL().execute(download_url);
                } else {
                    sendNotification("Cannot update app, please report the problem to developer.");
                }
            }
        }
    };
    private String download_url;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        RequestQueue queue = Volley.newRequestQueue(this);
        sendNotification("Checking for updates");
        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".START_UPDATE"));
        int VERSION_CODE = BuildConfig.VERSION_CODE;
        String baseUrl = "https://api.github.com/";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, baseUrl + "repos/abh80/smart-edge/releases",
                null, response -> {
            if (response.length() > 0) {
                try {
                    JSONObject object = (JSONObject) response.get(0);
                    try {
                        if (Integer.parseInt(object.getString("tag_name")) > VERSION_CODE) {
                            JSONArray o = object.getJSONArray("assets");
                            download_url = ((JSONObject) o.get(0)).getString("browser_download_url");
                            Intent intent = new Intent(getPackageName() + ".UPDATE_AVAIL");
                            intent.putExtra("version", object.getString("name"));
                            sendBroadcast(new Intent(intent));
                        } else {
                            stopSelf();
                        }
                    } catch (Exception e) {
                        // do nothing lol
                        stopSelf();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    stopSelf();
                }

            } else {
                stopSelf();
            }
        }, error ->

        {
        });
        queue.add(jsonArrayRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        manager.cancel(100);
    }

    NotificationManager manager;


    private void sendNotification(String text) {
        final String NOTIFICATION_CHANNEL_ID = getPackageName() + ".updater_channel";
        String channelName = "Updater Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Smart Edge")
                .setContentText(text)
                .setSmallIcon(R.drawable.launcher_foreground)

                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        manager.notify(100, notification);
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            sendNotification("Starting download");
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(getExternalFilesDir(null).getAbsolutePath() + "/output.apk");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            final String NOTIFICATION_CHANNEL_ID = getPackageName() + ".updater_channel";
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(UpdaterService.this, NOTIFICATION_CHANNEL_ID);
            notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.launcher_foreground)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentTitle("Smart Edge")
                    .setContentText("Downloading update")
                    .setProgress(100, Integer.parseInt(String.valueOf(progress[0])), false);

            Notification notification = notificationBuilder.build();
            manager.notify(100, notification);
        }

        //  Source for below codes : https://medium.com/@vishtech36/installing-apps-programmatically-in-android-10-7e39cfe22b86
        @Override
        protected void onPostExecute(String file_url) {
            String PATH = getExternalFilesDir(null).getAbsolutePath() + "/output.apk";
            File file = new File(PATH);
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uriFromFile(getApplicationContext(), new File(PATH)), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    getApplicationContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Log.e("TAG", "Error in opening the file!");
                }
            } else {
                Toast.makeText(getApplicationContext(), "installing", Toast.LENGTH_LONG).show();
            }


            UpdaterService.this.

                    stopSelf();
        }

        Uri uriFromFile(Context context, File file) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);

        }
    }
}
