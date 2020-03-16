package com.example.samplesbs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.kakao.kakaonavi.KakaoNaviParams;
import com.kakao.kakaonavi.KakaoNaviService;
import com.kakao.kakaonavi.Location;
import com.kakao.kakaonavi.NaviOptions;
import com.kakao.kakaonavi.options.CoordType;
import com.kakao.kakaonavi.options.RpOption;
import com.kakao.kakaonavi.options.VehicleType;

public class NaviActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
        Intent intent = getIntent();
        float x = intent.getExtras().getFloat("x");
        float y = intent.getExtras().getFloat("y");

        // Location.Builder를 사용하여 Location 객체를 만든다.
        Location destination = Location.newBuilder("카카오 판교 오피스", 127.10821222694533, 37.40205604363057).build();

        NaviOptions options = NaviOptions.newBuilder().setCoordType(CoordType.WGS84)
                .setVehicleType(VehicleType.FIRST).setRpOption(RpOption.SHORTEST).setStartX(x).setStartY(y).build();
        // 경유지를 포함하지 않는 KakaoNaviParams.Builder 객체
        KakaoNaviParams.Builder builder = KakaoNaviParams.newBuilder(destination).setNaviOptions(options);

        KakaoNaviService.getInstance().navigate(NaviActivity.this, builder.build());
        //웹으로 지원해줌
    }
}
