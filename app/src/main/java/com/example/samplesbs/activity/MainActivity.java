package com.example.samplesbs.activity;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samplesbs.R;
import com.example.samplesbs.data_model.LocationData;
import com.example.samplesbs.php.InsertLocationData;
import com.example.samplesbs.php.NotifyAccident;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import com.muddzdev.styleabletoast.StyleableToast;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener, SensorEventListener {
    public static Context context;
    private Button accidentBtn;
    private TextView bearingTextView;
    private TextView locationServiceStatus;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private ImageView settingBtn;
    private TextView speedTextView;
    public static final int PERMISSION_CODE = 1000;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private MapView mapView;
    private Boolean isFirst = true;
    private AudioManager audioManager;
    private static String EXTERNAL_IP_ADDRESS = "133.186.212.78";

    private String userID = null;
    private String token = null;

    private Queue<LocationData> queue = new LinkedList<LocationData>();
    private Queue<LocationData> testQueue = new LinkedList<LocationData>();
    private Location prevLocation, latestLocation;
    private float currentBearing;
    private double accidentLatitude = 0.0;
    private double accidentLongitude = 0.0;

    //acc sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] gravity= new float[3];

    private long start, end;
    float distance;
    long time;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //daummap 2번 로딩시 에러 나기 때문
        context = this;
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
                case R.id.settingBtn:
                    if (!drawer.isDrawerOpen(Gravity.LEFT)) {
                        drawer.openDrawer(Gravity.LEFT) ;
                    }else{
                        drawer.closeDrawer(Gravity.LEFT); ;
                    }
                    break;
                case R.id.accidentBtn:
                    NotifyAccident notifyAccident = new NotifyAccident(getApplicationContext());
                    notifyAccident.setQueue(testQueue);
                    notifyAccident.execute("http://" + EXTERNAL_IP_ADDRESS + "/accident.php", String.valueOf(37.320469), String.valueOf(127.115286), "accident", token);
            }
        }
    };

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
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
            speedTextView.setText("속도:" + (distance / time) + "(m/s)");

            prevLocation.setLatitude(latestLocation.getLatitude());
            prevLocation.setLongitude(latestLocation.getLongitude());

            latestLocation.setLatitude(mapPointGeo.latitude);
            latestLocation.setLongitude(mapPointGeo.longitude);

            currentBearing = prevLocation.bearingTo(latestLocation);
            if(currentBearing<0){
                currentBearing +=360;
            }

            if(bearingTextView!=null)
                bearingTextView.setText(currentBearing+"");



            if (token != null && (prevLocation.getLongitude() != latestLocation.getLongitude() || prevLocation.getLatitude() != latestLocation.getLatitude())) //위도 경도가 하나라도 달라야됨
                insertLocationData.execute("http://" + EXTERNAL_IP_ADDRESS + "/insert.php", String.valueOf(latestLocation.getLatitude()), String.valueOf(latestLocation.getLongitude()), token);

            if(queue.size()<10)
                queue.add(new LocationData(mapPointGeo.latitude,mapPointGeo.longitude,currentBearing));
            else{
                queue.poll();
                queue.add(new LocationData(mapPointGeo.latitude,mapPointGeo.longitude,currentBearing));
            }
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


        if(total > 1.0 * 9.8 && token!=null && (accidentLongitude!=latestLocation.getLongitude()||accidentLatitude!=latestLocation.getLatitude())) { //사고 위치가 바뀌고 중력가속도 기준치 이상
            Log.d("accident","occur");
            accidentLatitude = latestLocation.getLatitude();
            accidentLongitude = latestLocation.getLongitude();
            NotifyAccident notifyAccident = new NotifyAccident(getApplicationContext());

            notifyAccident.setQueue(queue);
            notifyAccident.execute("http://" + EXTERNAL_IP_ADDRESS + "/accident.php", String.valueOf(accidentLatitude), String.valueOf(accidentLongitude), "accident", token);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void showMarker(MapPoint mapPoint,ArrayList<LocationData> items) {
        double minBearing=360.0, maxBearing=0.0;

        // 같은 장소에서 테스트 시 아래 max,min 구하는 코드를 적용시 문제 있을 수 있음: 같은 장소 테스트시에는 가급적 주석처리.
        for(int i=0; i<items.size(); i++){
            if(minBearing > items.get(i).getAngle() && items.get(i).getAngle()!=0.0)
                minBearing=items.get(i).getAngle();
            if(maxBearing < items.get(i).getAngle() && items.get(i).getAngle()!=0.0)
                maxBearing=items.get(i).getAngle();
        }
        // End Of Max, Min Algorithm

        for(int i=0; i<items.size(); i++){
            Location subCircle = new Location(String.valueOf(i));
            subCircle.setLongitude(items.get(i).getLongitude());
            subCircle.setLatitude(items.get(i).getLatitude());
            double distance = latestLocation.distanceTo(subCircle);
            if(distance<135.0 &&(currentBearing>=minBearing && currentBearing<=maxBearing)){
                MapPOIItem customMarker = new MapPOIItem();
                customMarker.setItemName("급감속 발생 위치");
                customMarker.setTag(1);
                customMarker.setMapPoint(mapPoint);
                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                customMarker.setCustomImageResourceId(R.drawable.accident_spot); // 마커 이미지.
                customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                customMarker.setCustomImageAnchor(0.0f, 0.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StyleableToast.makeText(getApplicationContext(), "근방에 급감속 발생", Toast.LENGTH_LONG, R.style.mytoast).show();
                    }
                }, 0);
                mapView.addPOIItem(customMarker);
                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.alert);
                audioManager.setStreamVolume(audioManager.STREAM_MUSIC,
                        (int)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_PLAY_SOUND);
                mediaPlayer.start();
                break;
            }else{
                Log.e("latestLocation",  latestLocation.getLatitude()+","+latestLocation.getLongitude()+"");
                Log.e("subcircle",  subCircle.getLatitude()+","+subCircle.getLongitude()+"");
                Log.e("distance",  latestLocation.distanceTo(subCircle)+"");
                continue;
            }
        }
    }




    private void init() {
        prevLocation = new Location("prevLocation");
        latestLocation = new Location("latestLocation");

        mapView = findViewById(R.id.map_view);
        settingBtn = findViewById(R.id.settingBtn);
        accidentBtn = findViewById(R.id.accidentBtn);
        speedTextView = findViewById(R.id.speedTextView);
        bearingTextView = findViewById(R.id.bearingTextView);
        locationServiceStatus = findViewById(R.id.locationServiceStatus);



        navigationView = findViewById(R.id.navigationView);
        drawer = findViewById(R.id.drawerLayout);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        setting();
        mapView.setZoomLevel(2, true);
        mapView.zoomIn(true);
        mapView.zoomOut(true);
        mapView.setHDMapTileEnabled(false); //고해상도 지도 사용 안함
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setCurrentLocationEventListener(this);
        navigationView.setNavigationItemSelectedListener(itemSelectedListener);
        settingBtn.setOnClickListener(onClickListener);
        accidentBtn.setOnClickListener(onClickListener);
    }


    private void startActivity(Class c) {
        Intent intent = new Intent(this, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    NavigationView.OnNavigationItemSelectedListener itemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            menuItem.setChecked(true);
            drawer.closeDrawer(Gravity.LEFT);

            switch (menuItem.getItemId()){
                case R.id.sound_alert_item:
                    Toast.makeText(getApplicationContext(),"sound",Toast.LENGTH_LONG).show();
                    break;
                case R.id.vibration_alert_item:
                    Toast.makeText(getApplicationContext(),"vibration",Toast.LENGTH_LONG).show();
                    break;
                case R.id.logout_item:
                    FirebaseAuth.getInstance().signOut();
                    startActivity(LoginActivity.class);
                    break;
            }

            return true;
        }
    };

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
        //onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        //onFinishReverseGeoCoding("Fail");
    }

    private void setting(){
        LocationData t1= new LocationData(37.325582,127.109091);
        LocationData t2= new LocationData(37.324179,127.109043);
        LocationData t3= new LocationData(37.322769,127.108670);
        LocationData t4= new LocationData(37.321975,127.108594);
        LocationData t5= new LocationData(37.321059,127.108552);//보정동 좌회전해서 돌아가는쪽
        LocationData t6= new LocationData(37.320473,127.108858);
        LocationData t7= new LocationData(37.320458,127.110349);
        LocationData t8= new LocationData(37.320481,127.112590);
        LocationData t9= new LocationData(37.320485,127.113904);
        LocationData t10= new LocationData(37.320469,127.115286);
        t1.setAngle(0.0); //default value
        t2.setAngle(181.56506);
        t3.setAngle(191.92928);
        t4.setAngle(184.37137);
        t5.setAngle(182.09717);
        t6.setAngle(157.36186);
        t7.setAngle(90.72125);
        t8.setAngle(89.26306);
        t9.setAngle(89.78121);
        t10.setAngle(90.8301);


        testQueue.add(t1);testQueue.add(t2);testQueue.add(t3);testQueue.add(t4);testQueue.add(t5);
        testQueue.add(t6);testQueue.add(t7);testQueue.add(t8);testQueue.add(t9);testQueue.add(t10);




        Location test1 = new Location("test1");
        Location test2 = new Location("test2");
        Location test3 = new Location("test3");
        Location test4 = new Location("test4");
        Location test5 = new Location("test5");
        Location test6 = new Location("test6");
        Location test7 = new Location("test7");
        Location test8 = new Location("test8");
        Location test9 = new Location("test9");
        Location test10 = new Location("test10");
        test1.setLatitude(37.325582);test1.setLongitude(127.109091);
        test2.setLatitude(37.324179);test2.setLongitude(127.109043);
        test3.setLatitude(37.322769);test3.setLongitude(127.108670);
        test4.setLatitude(37.321975);test4.setLongitude(127.108594);
        test5.setLatitude(37.321059);test5.setLongitude(127.108552); //회전
        test6.setLatitude(37.320473);test6.setLongitude(127.108858);

        test7.setLatitude(37.320458);test7.setLongitude(127.110349);
        test8.setLatitude(37.320481);test8.setLongitude(127.112590);
        test9.setLatitude(37.320485);test9.setLongitude(127.113904);
        test10.setLatitude(37.320469);test10.setLongitude(127.115286);




        float bearing1= test1.bearingTo(test2);
        if(bearing1<0){
            bearing1 +=360;
        }
        float bearing2= test2.bearingTo(test3);
        if(bearing2<0){
            bearing2 +=360;
        }
        float bearing3= test3.bearingTo(test4);
        if(bearing3<0){
            bearing3 +=360;
        }
        float bearing4= test4.bearingTo(test5);
        if(bearing4<0){
            bearing4 +=360;
        }
        float bearing5= test5.bearingTo(test6);
        if(bearing5<0){
            bearing5 +=360;
        }
        float bearing6= test6.bearingTo(test7);
        if(bearing6<0){
            bearing6 +=360;
        }
        float bearing7= test7.bearingTo(test8);
        if(bearing7<0){
            bearing7 +=360;
        }
        float bearing8= test8.bearingTo(test9);
        if(bearing8<0){
            bearing8 +=360;
        }
        float bearing9= test9.bearingTo(test10);
        if(bearing9<0){
            bearing9 +=360;
        }

        Log.d("test1", bearing1+"");
        Log.d("test2", bearing2+"");
        Log.d("test3", bearing3+"");
        Log.d("test4", bearing4+"");
        Log.d("test5", bearing5+"");
        Log.d("test6", bearing6+"");
        Log.d("test7", bearing7+"");
        Log.d("test8", bearing8+"");
        Log.d("test9", bearing9+"");

    }
}
