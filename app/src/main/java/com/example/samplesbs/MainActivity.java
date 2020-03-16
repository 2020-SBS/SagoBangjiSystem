package com.example.samplesbs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.nio.channels.InterruptedByTimeoutException;
import java.security.Security;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {
    private double lastSpeed, currentSpeed;
    private Location prevLocation, latestLocation;
    private Context context;
    private TextView addressTextView;
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView speedTextView;
    public static final int PERMISSION_CODE = 1000;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private MapView mapView;
    private Boolean isFirst = true;
    private long start,end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        if (!hasPermissions(this, permissions))
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);

        prevLocation = new Location("prevLocation");
        latestLocation = new Location("latestLocation");

        mapView = findViewById(R.id.map_view);
        addressTextView = findViewById(R.id.address);
        distanceTextView = findViewById(R.id.distance);
        timeTextView = findViewById(R.id.time);
        speedTextView=findViewById(R.id.speed);

        mapView.setZoomLevel(3, true);
        mapView.zoomIn(true);
        mapView.zoomOut(true);
        mapView.setHDMapTileEnabled(false); //고해상도 지도 사용 안함
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);

        mapView.setCurrentLocationEventListener(this);
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        float distance;
        long time;
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i("TAG", String.format("MapView onCurrentLocationUpdate (%f,%f))", mapPointGeo.latitude, mapPointGeo.longitude)); //위도, 경도
        if(isFirst) {
            start = System.currentTimeMillis();
            latestLocation.setLatitude(mapPointGeo.latitude);
            latestLocation.setLongitude(mapPointGeo.longitude);
            isFirst=false;
        }else{
            end=System.currentTimeMillis();
            time=(end-start)/1000;
            start=end;
            prevLocation.setLatitude(latestLocation.getLatitude());
            prevLocation.setLongitude(latestLocation.getLongitude());

            latestLocation.setLatitude(mapPointGeo.latitude);
            latestLocation.setLongitude(mapPointGeo.longitude);

            distance=latestLocation.distanceTo(prevLocation);
            distanceTextView.setText("거리:"+distance+"(m)");
            timeTextView.setText("시간:"+time+"(s)");
            speedTextView.setText("속도:"+(distance/time)+"(m/s)");

        }
        mapView.setMapCenterPoint(mapPoint, true);

        MapReverseGeoCoder mapReverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_app_key),mapPoint,this,MainActivity.this);
        mapReverseGeoCoder.startFindingAddress();

        mapView.setCurrentLocationRadius(250); //m단위  250이란 값이 실제 지도에서 1km정도에 해당됨
        mapView.setCurrentLocationRadiusFillColor(android.graphics.Color.argb(20, 255, 0, 0));
        mapView.setCurrentLocationRadiusStrokeColor(android.graphics.Color.argb(100, 255, 0, 0));

        //카카오맵을 사용해야됨
        /*
        String url ="daummaps://search?q=맛집&p=37.537229,127.005515";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        finish();*/

        /*
        Intent intent = new Intent(MainActivity.this, NaviActivity.class);
        intent.putExtra("x",mapPointGeo.latitude);
        intent.putExtra("y",mapPointGeo.longitude);
        startActivity(intent);
        finish();*/
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[i])) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Log.e("권한", "허용");
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.permission_failed), Toast.LENGTH_SHORT).show();
                                ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
                                Log.e("권한", "비허용");
                            }
                        }

                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.permission_failed), Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
                    Log.e("권한", "비허용");
                    finish();
                }
                return;
            }
        }

    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }

    private void onFinishReverseGeoCoding(String result) {
        addressTextView.setText(result);
    }
}
