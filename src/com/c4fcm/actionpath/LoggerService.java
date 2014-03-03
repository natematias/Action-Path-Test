package com.c4fcm.actionpath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class LoggerService extends IntentService implements 
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener{

	Location mCurrentLocation;
	LocationClient mLocationClient;
	
	Stack<ArrayList<String>> queuedLogEntries;

	public LoggerService(){
		super("LoggerService");
	}

	@Override
	public void onCreate(){
		super.onCreate();
        mLocationClient = new LocationClient(this, this, this);
        queuedLogEntries = new Stack<ArrayList<String>>();
	}
	
	
	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		Bundle extras = intent.getExtras();
		String transitionType = intent.getStringExtra("transitionType");
		String[] geofenceIds = extras.getStringArray("ids");
		/*Log.i("TRANSITION INTENT HANDLED", "OH YEAH");
		Log.i("TRANSITION INTENT HANDLED", transitionType);
		Log.i("TRANSITION INTENT HANDLED", TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,geofenceIds));*/
		for(String id: geofenceIds){
			queueLocation(transitionType, id);
		}
		mLocationClient.connect();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 */
		//TODO: determine if we need to do this or not

		if (result.hasResolution()) {
			// if there's a way to resolve the result
		} else {
			// otherwise consider showing an error
		}	
	}


	@Override
	public void onConnected(Bundle connectionHint) {
		// when connected, log queued locations
		logQueuedLocations();
	}

	@Override
	public void onDisconnected() {	
	}

    public void queueLocation(String action, String id){
    	Timestamp now = new Timestamp(System.currentTimeMillis());
    	ArrayList<String> a = new ArrayList<String>();
    	a.add(0,action);
    	a.add(1,id);
    	a.add(2, now.toString());
		Log.i("QUEUEING LOCATION", action);
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


}
