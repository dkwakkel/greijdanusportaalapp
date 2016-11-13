package com.example.dkwakkel.greijdanusapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ScheduleActivity extends ContextActivity {

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new DownloadFilesTask(this).execute();
    }

    private class DownloadFilesTask extends AsyncTask<String, Void, String> {
        private final ContextActivity contextActivity;

        public DownloadFilesTask(ContextActivity contextActivity) {
            this.contextActivity = contextActivity;
        }

        protected String doInBackground(String... urls) {
            try {
                return new Downloader(contextActivity).getData();
            } catch (Exception e) {
                Log.e("Download", "Error during download", e);
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ScrollView sv = new ScrollView(ScheduleActivity.this);
            LinearLayout layout = new LinearLayout(ScheduleActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            sv.addView(layout);

            // This is where and how the view is used
            TextView tv = new TextView(ScheduleActivity.this);
            tv.setText(result);
            layout.addView(tv);

            setContentView(sv);
        }
    }

}