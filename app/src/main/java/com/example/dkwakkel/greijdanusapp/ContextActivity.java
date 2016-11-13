package com.example.dkwakkel.greijdanusapp;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;

public class ContextActivity extends AppCompatActivity {

    public String getUsername() {
        return getSetting(SettingsActivity.KEY_USERNAME, "");
    }

    public String getPassword() {
        return getSetting(SettingsActivity.KEY_PASSWORD, "");
    }

    private String getSetting(String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, defaultValue);
    }
}
