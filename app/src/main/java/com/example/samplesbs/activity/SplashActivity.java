package com.example.samplesbs.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.samplesbs.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private FirebaseUser user;
    private String userID =null;
    private String token = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        user= FirebaseAuth.getInstance().getCurrentUser();

        Handler handler = new Handler();
        handler.postDelayed(new splashHandler(),0);
    }

    private class splashHandler implements Runnable{
        @Override
        public void run() {
            if(user==null) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                SplashActivity.this.finish();
            }else{
                final Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                userID=user.getUid();
                FirebaseFirestore.getInstance().collection("tokens").document(userID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        token=documentSnapshot.get("token").toString();
                        intent.putExtra("token",token);
                        intent.putExtra("uid",userID);
                        startActivity(intent);
                        SplashActivity.this.finish();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed(){
        //스플래시 화면에서 뒤로가기 불가
    }

}
