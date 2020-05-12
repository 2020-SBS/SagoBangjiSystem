package com.example.samplesbs.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.example.samplesbs.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
    private String userID = null;
    private String token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        user = FirebaseAuth.getInstance().getCurrentUser();

        Handler handler = new Handler();
        handler.postDelayed(new splashHandler(), 0);
    }

    private class splashHandler implements Runnable {
        @Override
        public void run() {
            if (user == null) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            } else {
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    return;
                                }
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                String token = task.getResult().getToken();
                                Map<String, Object> map = new HashMap<>();
                                map.put("token", token);
                                String uid = user.getUid();
                                FirebaseFirestore.getInstance().collection("tokens").document(uid).set(map);
                                intent.putExtra("uid", uid);
                                intent.putExtra("token", token);
                                startActivity(intent);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        FirebaseAuth.getInstance().signOut();
                        startActivity(intent);

                    }
                });
            }
            SplashActivity.this.finish();
        }
    }

    /*
    private class splashHandler implements Runnable{
        @Override
        public void run() {
            if(user==null) {
                Log.e("null","user");
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                SplashActivity.this.finish();
            }else{
                final Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                userID=user.getUid();
                Log.e("exist","user");
                FirebaseFirestore.getInstance().collection("tokens").document(userID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try {
                            token = documentSnapshot.get("token").toString();
                        }catch (NullPointerException e){
                            e.printStackTrace();
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }
                        intent.putExtra("token",token);
                        intent.putExtra("uid",userID);
                        startActivity(intent);
                        SplashActivity.this.finish();
                    }
                });
            }
        }
    }*/

    @Override
    public void onBackPressed() {
        //스플래시 화면에서 뒤로가기 불가
    }

}
