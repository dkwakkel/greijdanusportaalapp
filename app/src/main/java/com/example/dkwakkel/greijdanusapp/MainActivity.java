package com.example.dkwakkel.greijdanusapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;

public class MainActivity extends ContextActivity {

    private static final int SETTINGS_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openSettings(View view) {
        Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(i, SETTINGS_RESULT);
    }

    public void openSchedule(View view) throws IOException {
        Intent i = new Intent(getApplicationContext(), ScheduleActivity.class);
        startActivityForResult(i, SETTINGS_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SETTINGS_RESULT) {
            System.err.println("Ready");
         }
    }
}
