package com.example.samplesbs.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samplesbs.R;
import com.example.samplesbs.data_model.LocationData;
import com.example.samplesbs.data_model.UserData;
import com.example.samplesbs.data_model.UserSituationData;
import com.example.samplesbs.php.InsertLocationData;
import com.example.samplesbs.php.NotifyAccident;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener, SensorEventListener {
    public static Context context;
    private FirebaseFirestore firestore;
    private TextView addressTextView;
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView speedTextView;
    private TextView locationServiceStatus;
    private UserSituationData userSituationData = new UserSituationData();
    private Button logoutBtn;
    public static final int PERMISSION_CODE = 1000;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private MapView mapView;
    private Boolean isFirst = true;
    private long start, end;

    private static String IP_ADDRESS = "192.168.0.112"; //wifi
    private static String EXTERNAL_IP_ADDRESS = "117.123.48.170";

    //192.168.0.1
    //192.168.137.1
    //117.123.48.170 외부아이피
    //192.168.0.114 dmz 호스트 주소

    private String userID = null;
    private String token = null;

    private Queue<LocationData> queue = new LinkedList<LocationData>();
    private Location prevLocation, latestLocation;
    private double accidentLatitude = 0.0;
    private double accidentLongitde = 0.0;

    //acc sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] gravity= new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //daummap 2번 로딩시 에러 나기 때문
        context = this;
        firestore = FirebaseFirestore.getInstance();
        userID = getIntent().getStringExtra("uid");
        token = getIntent().getStringExtra("token");


        if (!hasPermissions(this, permissions))
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
        else {
            init();
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            assert sensorManager != null;
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.logoutBtn:
                    FirebaseAuth.getInstance().signOut();
                    startActivity(LoginActivity.class);
                    break;
            }
        }
    };

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        float distance;
        long time;
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        InsertLocationData insertLocationData = new InsertLocationData(this);
        if (isFirst) { //첫 업데이트
            start = System.currentTimeMillis();
            latestLocation.setLatitude(mapPointGeo.latitude);
            latestLocation.setLongitude(mapPointGeo.longitude);
            queue.add(new LocationData(mapPointGeo.latitude,mapPointGeo.longitude));
            if (token != null)
                insertLocationData.execute("http://" + EXTERNAL_IP_ADDRESS + "/insert.php", String.valueOf(latestLocation.getLatitude()), String.valueOf(latestLocation.getLongitude()), token);
            isFirst = false;
        } else { //이후의 갱신
            end = System.currentTimeMillis();
            time = (end - start) / 1000;
            start = end;
            prevLocation.setLatitude(latestLocation.getLatitude());
            prevLocation.setLongitude(latestLocation.getLongitude());

            latestLocation.setLatitude(mapPointGeo.latitude);
            latestLocation.setLongitude(mapPointGeo.longitude);

            distance = latestLocation.distanceTo(prevLocation);
            distanceTextView.setText("거리:" + distance + "(m)");
            timeTextView.setText("시간:" + time + "(s)");
            speedTextView.setText("속도:" + (distance / time) + "(m/s)");
            if (token != null && (prevLocation.getLongitude() != latestLocation.getLongitude() || prevLocation.getLatitude() != latestLocation.getLatitude())) //위도 경도가 하나라도 달라야됨
                insertLocationData.execute("http://" + EXTERNAL_IP_ADDRESS + "/insert.php", String.valueOf(latestLocation.getLatitude()), String.valueOf(latestLocation.getLongitude()), token);

            if(queue.size()<10)
                queue.add(new LocationData(mapPointGeo.latitude,mapPointGeo.longitude));
            else{
                queue.poll();
                queue.add(new LocationData(mapPointGeo.latitude,mapPointGeo.longitude));
            }

            userSituationData.setDistance(distance);
            userSituationData.setTime(time);
            userSituationData.setSpeed(distance / time);
            firestore.collection("situations").document(userID).set(userSituationData);
        }


        MapReverseGeoCoder mapReverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_app_key), mapPoint, this, MainActivity.this);
        mapReverseGeoCoder.startFindingAddress();

        mapView.setMapCenterPoint(mapPoint, true);
        mapView.setCurrentLocationRadius(250); //m단위  250이란 값이 실제 지도에서 1km정도에 해당됨
        mapView.setCurrentLocationRadiusFillColor(android.graphics.Color.argb(10, 255, 0, 0));
        mapView.setCurrentLocationRadiusStrokeColor(android.graphics.Color.argb(100, 255, 0, 0));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];  //중력가속도 계산

        float accX = event.values[0] - gravity[0];
        float accY = event.values[1] - gravity[1];
        float accZ = event.values[2] - gravity[2]; // 중력을 뺀 가속도.

        double total = Math.sqrt(Math.pow(accX, 2) + Math.pow(accY, 2) + Math.pow(accZ, 2));


        if(total > 6.0 * 9.8 && token!=null && (accidentLongitde!=latestLocation.getLongitude()||accidentLatitude!=latestLocation.getLatitude())) { //사고 위치가 바뀌고 중력가속도 기준치 이상
            Log.d("accident","occur");
            accidentLatitude = latestLocation.getLatitude();
            accidentLongitde = latestLocation.getLongitude();
            NotifyAccident notifyAccident = new NotifyAccident(getApplicationContext());
            notifyAccident.execute("http://" + EXTERNAL_IP_ADDRESS + "/accident.php", String.valueOf(accidentLatitude), String.valueOf(accidentLongitde), "accident", token);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void showMarker(MapPoint mapPoint) {
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("급감속 발생 위치");
        customMarker.setTag(1);
        customMarker.setMapPoint(mapPoint);
        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageResourceId(R.drawable.marker); // 마커 이미지.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

        mapView.addPOIItem(customMarker);
    }

    private void onFinishReverseGeoCoding(String result) {
        addressTextView.setText(result);
    }


    private void init() {
        prevLocation = new Location("prevLocation");
        latestLocation = new Location("latestLocation");

        mapView = findViewById(R.id.map_view);
        addressTextView = findViewById(R.id.address);
        distanceTextView = findViewById(R.id.distance);
        timeTextView = findViewById(R.id.time);
        speedTextView = findViewById(R.id.speed);
        logoutBtn = findViewById(R.id.logoutBtn);
        locationServiceStatus = findViewById(R.id.locationServiceStatus);

        mapView.setZoomLevel(2, true);
        mapView.zoomIn(true);
        mapView.zoomOut(true);
        mapView.setHDMapTileEnabled(false); //고해상도 지도 사용 안함
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
        mapView.setCurrentLocationEventListener(this);
        logoutBtn.setOnClickListener(onClickListener);
    }


    private void startActivity(Class c) {
        Intent intent = new Intent(this, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void setLocationServiceStatus(String text) {
        locationServiceStatus.setText(text);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[i])) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                init();
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
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {}

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {}

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {}

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }
}
