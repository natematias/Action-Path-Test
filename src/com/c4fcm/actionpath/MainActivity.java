/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.c4fcm.actionpath;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.c4fcm.actionpath.GeofenceUtils.REMOVE_TYPE;
import com.c4fcm.actionpath.GeofenceUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI handler for the Location Services Geofence sample app.
 * Allow input of latitude, longitude, and radius for two geofences.
 * When registering geofences, check input and then send the geofences to Location Services.
 * Also allow removing either one of or both of the geofences.
 * The menu allows you to clear the screen or delete the geofences stored in persistent memory.
 */
public class MainActivity extends FragmentActivity {
    /*
     * Use to set an expiration time for a geofence. After this amount
     * of time Location Services will stop tracking the geofence.
     * Remember to unregister a geofence when you're finished with it.
     * Otherwise, your app will use up battery. To continue monitoring
     * a geofence indefinitely, set the expiration time to
     * Geofence#NEVER_EXPIRE.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;

    // Store the current request
    private REQUEST_TYPE mRequestType;

    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;

    // Persistent storage for geofences
    private SimpleGeofenceStore mPrefs;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    // Add geofences handler
    private GeofenceRequester mGeofenceRequester;
    // Remove geofences handler
    private GeofenceRemover mGeofenceRemover;
 
    /*
     * Internal lightweight geofence objects for geofence 1 and 2
     */
    private SimpleGeofence mUIGeofence1;
    private SimpleGeofence mUIGeofence2;
    private SimpleGeofence mUIGeofence3;
    private SimpleGeofence mUIGeofence4;
    private SimpleGeofence mUIGeofence5;
    private SimpleGeofence mUIGeofence6;
    private SimpleGeofence mUIGeofence7;
    private SimpleGeofence mUIGeofence8;

    // decimal formats for latitude, longitude, and radius
    private DecimalFormat mLatLngFormat;
    private DecimalFormat mRadiusFormat;

    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    private GeofenceSampleReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter mIntentFilter;

    // Store the list of geofences to remove
    private List<String> mGeofenceIdsToRemove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the pattern for the latitude and longitude format
        String latLngPattern = getString(R.string.lat_lng_pattern);

        // Set the format for latitude and longitude
        mLatLngFormat = new DecimalFormat(latLngPattern);

        // Localize the format
        mLatLngFormat.applyLocalizedPattern(mLatLngFormat.toLocalizedPattern());

        // Set the pattern for the radius format
        String radiusPattern = getString(R.string.radius_pattern);

        // Set the format for the radius
        mRadiusFormat = new DecimalFormat(radiusPattern);

        // Localize the pattern
        mRadiusFormat.applyLocalizedPattern(mRadiusFormat.toLocalizedPattern());

        // Create a new broadcast receiver to receive updates from the listeners and service
        mBroadcastReceiver = new GeofenceSampleReceiver();

        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // Instantiate a new geofence storage area
        mPrefs = new SimpleGeofenceStore(this);

        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);
        addGeoFences();
        // Attach to the main UI
        setContentView(R.layout.activity_main);

    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * GeofenceRemover and GeofenceRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     * calls
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to add geofences
                        if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Toggle the request flag and send a new request
                            mGeofenceRequester.setInProgressFlag(false);

                            // Restart the process of adding the current geofences
                            mGeofenceRequester.addGeofences(mCurrentGeofences);

                        // If the request was to remove geofences
                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                            // Toggle the removal flag and send a new removal request
                            mGeofenceRemover.setInProgressFlag(false);

                            // If the removal was by Intent
                            if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

                                // Restart the removal of all geofences for the PendingIntent
                                mGeofenceRemover.removeGeofencesByIntent(
                                    mGeofenceRequester.getRequestPendingIntent());

                            // If the removal was by a List of geofence IDs
                            } else {

                                // Restart the removal of the geofence list
                                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
                            }
                        }
                    break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }

    /*
     * Whenever the Activity resumes, reconnect the client to Location
     * Services and reload the last geofences that were set
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver to receive status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
        /*
         * Get existing geofences from the latitude, longitude, and
         * radius values stored in SharedPreferences. If no values
         * exist, null is returned.
         */
        mUIGeofence1 = mPrefs.getGeofence("1");
        mUIGeofence2 = mPrefs.getGeofence("2");
        mUIGeofence3 = mPrefs.getGeofence("3");
        mUIGeofence4 = mPrefs.getGeofence("4");
        mUIGeofence5 = mPrefs.getGeofence("5");
        mUIGeofence6 = mPrefs.getGeofence("6");
        mUIGeofence7 = mPrefs.getGeofence("7");
        mUIGeofence8 = mPrefs.getGeofence("8");
    }

    /*
     * Inflate the app menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }
    /*
     * Respond to menu item selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
    	/*        switch (item.getItemId()) {

            // Request to clear the geofence1 settings in the UI
            case R.id.menu_item_clear_geofence1:
                mLatitude1.setText(GeofenceUtils.EMPTY_STRING);
                mLongitude1.setText(GeofenceUtils.EMPTY_STRING);
                mRadius1.setText(GeofenceUtils.EMPTY_STRING);
                return true;

            // Pass through any other request
            default:
                return super.onOptionsItemSelected(item);
        }*/
    	return super.onOptionsItemSelected(item);
    }

    /*
     * Save the current geofence settings in SharedPreferences.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.setGeofence("1", mUIGeofence1);
        mPrefs.setGeofence("2", mUIGeofence2);
        mPrefs.setGeofence("3", mUIGeofence3);
        mPrefs.setGeofence("4", mUIGeofence4);
        mPrefs.setGeofence("5", mUIGeofence5);
        mPrefs.setGeofence("6", mUIGeofence6);
        mPrefs.setGeofence("7", mUIGeofence7);
        mPrefs.setGeofence("8", mUIGeofence8);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

        // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }

    /**
     * Called when the user clicks the "Remove geofences" button
     *
     * @param view The view that triggered this callback
     */
    public void onUnregisterByPendingIntentClicked(View view) {
        /*
         * Remove all geofences set by this app. To do this, get the
         * PendingIntent that was added when the geofences were added
         * and use it as an argument to removeGeofences(). The removal
         * happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done
         */

        /*
         * Record the removal as remove by Intent. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        // Record the type of removal
        mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        // Try to make a removal request
        try {
        /*
         * Remove the geofences represented by the currently-active PendingIntent. If the
         * PendingIntent was removed for some reason, re-create it; since it's always
         * created with FLAG_UPDATE_CURRENT, an identical PendingIntent is always created.
         */
        mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester.getRequestPendingIntent());

        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Called when the user clicks the "Remove geofence 1" button
     * @param view The view that triggered this callback
     */
    public void onUnregisterGeofence1Clicked(View view) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of geofence 1 ("1").
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

        // Create a List of 1 Geofence with the ID "1" and store it in the global list
        mGeofenceIdsToRemove = Collections.singletonList("1");

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        mRemoveType = GeofenceUtils.REMOVE_TYPE.LIST;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        // Try to remove the geofence
        try {
            mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);

        // Catch errors with the provided geofence IDs
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }
    }



    public void addGeoFences() {

        /*
         * Record the request as an ADD. If a connection error occurs,
         * the app can automatically restart the add request if Google Play services
         * can fix the error
         */
        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }
        
        /*
         * Create a version of geofence 1 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #1 --> Last lamppost before you get to medical building pass through
         */
        mUIGeofence1 = new SimpleGeofence(
            "1",
            Double.valueOf(42.361228),
            Double.valueOf(-71.087067),
            Float.valueOf("1.0"),
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Only detect entry transitions
            Geofence.GEOFENCE_TRANSITION_ENTER);

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("1", mUIGeofence1);

        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #2 --> Tree in front of Old Media Lab entrance on medical building side
         */
        mUIGeofence2 = new SimpleGeofence(
            "2",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.361228"),
            Double.valueOf("-71.087067"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("2", mUIGeofence2);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #3 --> Corner post of old media lab overhang on medical building side
         */
        mUIGeofence3 = new SimpleGeofence(
            "3",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.361228"),
            Double.valueOf("-71.087067"),
            Float.valueOf("10.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("3", mUIGeofence3);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #4 --> Tree near overhang on Ames Street side of old building
         */
        mUIGeofence4 = new SimpleGeofence(
            "4",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.361016"),
            Double.valueOf("-71.08715"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("4", mUIGeofence4);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #5 --> Front door on New media lab building on medical building side
         */
        mUIGeofence5 = new SimpleGeofence(
            "5",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.360963"),
            Double.valueOf("-71.087117"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("5", mUIGeofence5);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #6 --> First tree near bike racks medical building side heading toward food trucks
         */
        mUIGeofence6 = new SimpleGeofence(
            "6",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.36091"),
            Double.valueOf("-71.087087"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("6", mUIGeofence6);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #7 --> Walkway from E15 -> E14 on civic side
         */
        mUIGeofence7 = new SimpleGeofence(
            "7",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.360853"),
            Double.valueOf("-71.087052"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("7", mUIGeofence7);
        
        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         * 
         * #8 --> In front of elevators in E14
         */
        mUIGeofence8 = new SimpleGeofence(
            "8",
            // Get latitude, longitude, and radius from the UI
            Double.valueOf("42.360935"),
            Double.valueOf("-71.087012"),
            Float.valueOf("5.0"),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Detect both entry and exit transitions
            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence("8", mUIGeofence8);

        /*
         * Add Geofence objects to a List. toGeofence()
         * creates a Location Services Geofence object from a
         * flat object
         */
        mCurrentGeofences.add(mUIGeofence1.toGeofence());
        mCurrentGeofences.add(mUIGeofence2.toGeofence());
        mCurrentGeofences.add(mUIGeofence3.toGeofence());
        mCurrentGeofences.add(mUIGeofence4.toGeofence());
        mCurrentGeofences.add(mUIGeofence5.toGeofence());
        mCurrentGeofences.add(mUIGeofence6.toGeofence());
        mCurrentGeofences.add(mUIGeofence7.toGeofence());
        mCurrentGeofences.add(mUIGeofence8.toGeofence());
        

        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            mGeofenceRequester.addGeofences(mCurrentGeofences);
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceSampleReceiver extends BroadcastReceiver {
        /*
         * Define the required method for broadcast receivers
         * This method is invoked when a broadcast Intent triggers the receiver
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check the action code and determine what to do
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

                handleGeofenceError(context, intent);

            // Intent contains information about successful addition or removal of geofences
            } else if (
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    ||
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

                handleGeofenceStatus(context, intent);

            // Intent contains information about a geofence transition
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

                handleGeofenceTransition(context, intent);

            // The Intent contained an invalid action
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * If you want to display a UI message about adding or removing geofences, put it here.
         *
         * @param context A Context for this component
         * @param intent The received broadcast Intent
         */
        private void handleGeofenceStatus(Context context, Intent intent) {

        }

        /**
         * Report geofence transitions to the UI
         *
         * @param context A Context for this component
         * @param intent The Intent containing the transition
         */
        private void handleGeofenceTransition(Context context, Intent intent) {
            /*
             * If you want to change the UI when a transition occurs, put the code
             * here. The current design of the app uses a notification to inform the
             * user that a transition has occurred.
             */
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         *
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
