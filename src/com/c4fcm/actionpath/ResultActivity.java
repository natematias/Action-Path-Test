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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
// LOCATION THINGS
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;
import android.content.IntentSender;
import android.location.Location;
import com.google.android.gms.location.LocationClient;
// file writing things
import android.os.Environment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;

import android.util.Log;



/**
 * This Activity is displayed when users click the notification itself. It provides
 * UI for snoozing and dismissing the notification.
 */
public class ResultActivity extends FragmentActivity implements 
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener{
	
	private final static int
    CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	Location mCurrentLocation;
	LocationClient mLocationClient;
	
	Stack<ArrayList<String>> queuedLogEntries;
    
	// this code is loaded when the activity is started
	// and when receiving an intent
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(this, this, this);
        queuedLogEntries = new Stack<ArrayList<String>>();
        Intent intent = getIntent();
        String action = intent.getAction();
/*        if(action==GeofenceUtils.ACTION_LOG_TRANSITION){
        	logCurrentLocation(action,intent.getStringExtra("transitionType"));
        }else{*/
          
	        setContentView(R.layout.activity_result);
	        String message = intent.getStringExtra(GeofenceUtils.EXTRA_MESSAGE);
	        TextView text = (TextView) findViewById(R.id.result_message);
	        text.setText(message);
        //}
    }
	
	public void onResume(){
		super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
		if(action==GeofenceUtils.ACTION_LOG_TRANSITION){
	        String transitionType = intent.getStringExtra("transitionType");
	        String id = intent.getStringExtra("id");
	        Log.i("queueing transition", transitionType + " - "+ id);
        	queueLocation(id,transitionType);
	        if(mLocationClient.isConnected()){
	        	logQueuedLocations();
	        }
		}
	}

    public void onSnoozeClick(View v) {
        FragmentManager fm = getSupportFragmentManager();
       // logCurrentLocation("SNOOZE");
        this.mCurrentLocation = this.mLocationClient.getLastLocation();
        
        SampleAlertDialogFragment sampleAlertDialog = new SampleAlertDialogFragment();
        sampleAlertDialog.setLocation(this.mCurrentLocation.toString());
        sampleAlertDialog.show(fm, "fragment_edit_name");

        /*Intent intent = new Intent(getApplicationContext(), NotificationService.class);
        intent.setAction(GeofenceUtils.ACTION_SNOOZE);
        startService(intent);*/
    }

    public void onDismissClick(View v) {
       // logCurrentLocation("DISMISS");
    	Intent myIntent=new Intent(v.getContext(),MainActivity.class);
        startActivity(myIntent);
        finish();
    }
    
    public void queueLocation(String action, String transitionType){
    	Timestamp now = new Timestamp(System.currentTimeMillis());
    	ArrayList<String> a = new ArrayList<String>();
    	a.add(0,action);
    	a.add(1,transitionType);
    	a.add(2, now.toString());
    	queuedLogEntries.push(a);
    }
    
    public void logQueuedLocations(){
      this.mCurrentLocation = this.mLocationClient.getLastLocation();
      String longitude = String.valueOf(this.mCurrentLocation.getLongitude());
      String latitude = String.valueOf(this.mCurrentLocation.getLatitude());
      Iterator<ArrayList<String>> it = queuedLogEntries.iterator();
      while(it.hasNext())
      {
          ArrayList<String> locations = it.next();
          logCurrentLocation(locations.get(0), locations.get(1),latitude, longitude, locations.get(2));
      }
      queuedLogEntries.clear(); // TODO: could be a garbage collection issue
    }
    
    /// LOG CURRENT LOCATION TO A FILE
    public void logCurrentLocation(String transition, String geofence, String latitude, String longitude, String timestamp){
    	try{
    		String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
    		File dir = new File(root + "/Android/data/action_path");    
			Log.i("LogResultActivity",root);
    		if(dir.mkdirs() || dir.isDirectory()){
    			
    			FileWriter write = new FileWriter(root + "/Android/data/action_path" + File.separator + "geodata.txt", true);
    			String line = geofence + "," + transition + "," + timestamp + 
    					"," + latitude + "," + longitude+"\n";
    			Log.i("LogResultActivity",line);
    			write.append(line);
    			write.flush();
    			write.close();
    		}
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    
    ////LOCATION RELATED CODE BELOW
    // connect the location client when the activity is started
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }
    
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }
    
    
    
    @Override
    public void onConnected(Bundle dataBundle) {
        // when connected, log queued locations
    	logQueuedLocations();
    }
    
    @Override
    public void onDisconnected() {
        // Display the connection status
        /*Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();*/
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, perhaps consider showing an error
             */
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    
}

