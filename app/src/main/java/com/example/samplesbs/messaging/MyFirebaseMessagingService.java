package com.example.samplesbs.messaging;


import android.util.Log;
import com.example.samplesbs.activity.MainActivity;
import com.example.samplesbs.data_model.LocationData;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import net.daum.mf.map.api.MapPoint;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

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
            final double latitude = Double.parseDouble((remoteMessage.getData().get("latitude")));
            final double longitude = Double.parseDouble((remoteMessage.getData().get("longitude")));
            final String[] latitude_array;
            final String[] longitude_array;
            final String[] angle_array;
            try {
                JSONArray temp = new JSONArray(((remoteMessage.getData().get("latitude_array"))));
                latitude_array = temp.join(",").split(",");
                temp = new JSONArray(((remoteMessage.getData().get("longitude_array"))));
                longitude_array = temp.join(",").split(",");
                temp = new JSONArray(((remoteMessage.getData().get("angle_array"))));
                angle_array = temp.join(",").split(",");
                ArrayList<LocationData> items = new ArrayList<>();
                try {
                    for (int i = 0; i < angle_array.length; i++) {
                        double lat = Double.parseDouble(latitude_array[i].substring(latitude_array[i].indexOf('"') + 1, latitude_array[i].lastIndexOf('"')));
                        double lon = Double.parseDouble(longitude_array[i].substring(longitude_array[i].indexOf('"') + 1, longitude_array[i].lastIndexOf('"')));
                        double ang = Double.parseDouble(angle_array[i].substring(angle_array[i].indexOf('"') + 1, angle_array[i].lastIndexOf('"')));
                        Log.d("lat", lat + "");
                        Log.d("lon", lon + "");
                        Log.d("ang", ang + "");
                        items.add(new LocationData(lat, lon, ang));
                        ((MainActivity)MainActivity.context).showMarker(MapPoint.mapPointWithGeoCoord(latitude,longitude), items);
                    }
                }catch (StringIndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }catch (JSONException e){e.printStackTrace();}
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }
}