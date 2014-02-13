/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.c4fcm.actionpath;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

/**
 * This Activity is displayed when users click the notification itself. It provides
 * UI for snoozing and dismissing the notification.
 */
public class ResultActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        String message = getIntent().getStringExtra(GeofenceUtils.EXTRA_MESSAGE);
        TextView text = (TextView) findViewById(R.id.result_message);
        text.setText(message);
    }

    public void onSnoozeClick(View v) {
        FragmentManager fm = getSupportFragmentManager();
        SampleAlertDialogFragment sampleAlertDialog = new SampleAlertDialogFragment();
        sampleAlertDialog.show(fm, "fragment_edit_name");

        /*Intent intent = new Intent(getApplicationContext(), NotificationService.class);
        intent.setAction(GeofenceUtils.ACTION_SNOOZE);
        startService(intent);*/
    }

    public void onDismissClick(View v) {
    	Intent myIntent=new Intent(v.getContext(),MainActivity.class);
        startActivity(myIntent);
        finish();
    }
}

