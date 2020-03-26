package com.example.samplesbs.messaging;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.samplesbs.R;
import com.example.samplesbs.activity.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.muddzdev.styleabletoast.StyleableToast;

import net.daum.mf.map.api.MapPoint;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            //final String message = remoteMessage.getData().get("message");
            final double latitude = Double.parseDouble((remoteMessage.getData().get("latitude")));
            final double longitude = Double.parseDouble((remoteMessage.getData().get("longitude")));
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    StyleableToast.makeText(getApplicationContext(), "근방에 급감속 발생", Toast.LENGTH_LONG, R.style.mytoast).show();
                    //Toast.makeText(getApplicationContext(),"주의. 전방에 급감속 발생",Toast.LENGTH_LONG).show();
                }
            }, 0);
            ((MainActivity)MainActivity.context).showMarker(MapPoint.mapPointWithGeoCoord(latitude,longitude));
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
}