package com.c4fcm.actionpath;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.Notification.Builder;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * This class receives geofence transition events from Location Services, in the
 * form of an Intent containing the transition type and geofence id(s) that triggered
 * the event.
 */
public class ReceiveTransitionsIntentService extends IntentService {
	
    /**
     * Sets an identifier for this class' background thread
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
    }

    /**
     * Handles incoming intents
     * @param intent The Intent sent by Location Services. This Intent is provided
     * to Location Services (inside a PendingIntent) when you call addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // Create a local broadcast Intent
        Intent broadcastIntent = new Intent();

        // Give it the category for all intents sent by the Intent Service
        broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // First check for errors
        if (LocationClient.hasError(intent)) {

            // Get the error code
            int errorCode = LocationClient.getErrorCode(intent);

            // Get the error message
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);

            // Log the error
            Log.e(GeofenceUtils.APPTAG,
                    getString(R.string.geofence_transition_error_detail, errorMessage)
            );

            // Set the action and error message for the broadcast intent
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            // Broadcast the error *locally* to other components in this app
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        // If there's no error, get the transition type and create a notification
        } else {

            // Get the type of transition (entry or exit)
            int transition = LocationClient.getGeofenceTransition(intent);

            // Test that a valid transition was reported
            if (
                    (transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                    ||
                    (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
               ) {

                // Post a notification
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size() ; index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                // log the first transition

                String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,geofenceIds);
                
                //sends an intent to ResultActivity, which is not suitable for logging
                //logTransition(getTransitionString(transition), ids);
                
                String transitionType = getTransitionString(transition);

                //activate the logging system
            	Intent loggerServiceIntent = new Intent(this,LoggerService.class);
                loggerServiceIntent.putExtra("transitionType", transitionType);
                loggerServiceIntent.putExtra("ids", geofenceIds);
            	startService(loggerServiceIntent);
                                
            	//create the notification
                sendNotification(transitionType, geofenceIds);

            	try {
            		this.playSound(getBaseContext());
            	} catch (Exception e) {
            	    e.printStackTrace();
            	}
            	
            	
                // Log the transition type and a message to adb debug
                Log.d(GeofenceUtils.APPTAG,
                        getString(
                                R.string.geofence_transition_notification_title,
                                transitionType,
                                ids));
                Log.d(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_notification_text));

            // An invalid transition was reported
            } else {
                // Always log as an error
                Log.e(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_invalid_type, transition));
            }
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     * @param transitionType The type of transition that occurred.
     *
     */
    private void sendNotification(String transitionType, String[] ids) {
    	
    	/*Random r=new Random();
    	int mId = r.nextInt(1000);*/
    	Intent resultIntent = new Intent(this, MainActivity.class);
    	PendingIntent resultPendingIntent = PendingIntent.getActivity(getBaseContext(),
    	        105, resultIntent,
    	        PendingIntent.FLAG_CANCEL_CURRENT);
    	
    	//NOTIFICATION FOR OLDER ANDROID DEVICES
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(this)
    	        .setSmallIcon(R.drawable.ic_stat_notification)
    	        .setContentTitle("Geofence Transition")
    	        .setContentIntent(resultPendingIntent)
    	        .setContentText(TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,ids));
    	// Creates an explicit intent for an Activity in your app

   
    	    	NotificationManager mNotificationManager =
    	    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	// mId allows you to update the notification later on.
    	mNotificationManager.notify(100, mBuilder.build());
    	
    	try {
    		this.playSound(getBaseContext());
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    	
    	//NOTIFICATIONS FOR MORE RECENT ANDROIDS (16 and up)
    	/*PendingIntent pi = getPendingIntent();
    	Builder notificationBuilder = new Notification.Builder(this);
    	notificationBuilder.setContentTitle("ActionPath " + transitionType + " " + TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,ids))
    	// Notification title
    	.setContentText("You have " + transitionType + " " + ids.length + "ActionPaths")
    	// you can put subject line.
    	.setSmallIcon(R.drawable.ic_launcher)
    	// Set your notification icon here.
    	.addAction(R.drawable.ic_launcher, "Respond",pi)
    	.addAction(
    			R.drawable.ic_action_search,
    			"Act Now", pi);
    	// Now create the Big picture notification.
    	Notification notification = new Notification.BigPictureStyle(notificationBuilder)
    	.bigPicture(
    			BitmapFactory.decodeResource(getResources(),
    					R.drawable.ic_notification_placeholder)).build();
    	// Put the auto cancel notification flag
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	NotificationManager notificationManager = getNotificationManager();
    	notificationManager.notify(0, notification);*/
    	
    	// TODO: Create a way to clear the notification once it has been clicked

    }
    
    public void playSound(Context context) throws IllegalArgumentException, 
    SecurityException, 
    IllegalStateException,
    IOException {

    	Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	MediaPlayer mMediaPlayer = new MediaPlayer();
    	mMediaPlayer.setDataSource(context, soundUri);
    	final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    	if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
    		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    		mMediaPlayer.setLooping(true);
    		mMediaPlayer.prepare();
    		mMediaPlayer.start();
    	}
    }
    
    //creates a PendingIntent for bigPicture notifications
    public PendingIntent getPendingIntent() {
    	return PendingIntent.getActivity(this, 0, new Intent(this,
    			MainActivity.class), 0);
    }

    public NotificationManager getNotificationManager() {
    	return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


    /**

     * Maps geofence transition types to their human-readable equivalents.
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);

            default:
                return getString(R.string.geofence_transition_unknown);
        }
    }
    
  
}
