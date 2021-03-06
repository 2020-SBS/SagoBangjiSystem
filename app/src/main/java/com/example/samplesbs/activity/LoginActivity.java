package com.example.samplesbs.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samplesbs.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Button loginBtn, gotoSuBtn, google_sign_in_btn,facebook_sign_in_btn;
    private TextView resetTextView;
    private String token=null;
    private String uid = null;

    private static final String TAG = "Login";
    private static final int GOOGLE_LOGIN_CODE = 1001;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInOptions gso;
    private CallbackManager callbackManager;
    private ContentLoadingProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        //FOR GOOGLE AUTH
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        googleSignInClient =  GoogleSignIn.getClient(this,gso);

        //FOR FACEBOOK AUTH
        callbackManager = CallbackManager.Factory.create();

        google_sign_in_btn = findViewById(R.id.google_sign_in_btn);
        facebook_sign_in_btn = findViewById(R.id.facebook_sign_in_btn);
        loginBtn = findViewById(R.id.loginBtn);
        gotoSuBtn = findViewById(R.id.gotoSuBtn);
        resetTextView = findViewById(R.id.password_reset_text);
        progressBar = findViewById(R.id.progress_bar);

        google_sign_in_btn.setOnClickListener(onClickListener);
        facebook_sign_in_btn.setOnClickListener(onClickListener);
        loginBtn.setOnClickListener(onClickListener);
        gotoSuBtn.setOnClickListener(onClickListener);
        resetTextView.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.loginBtn:
                    progressBar.show();
                    login();
                    break;
                case R.id.gotoSuBtn:
                    startActivity(SignUpActivity.class);
                    break;
                case R.id.password_reset_text:
                    startActivity(PasswordResetActivity.class);
                    break;
                case R.id.google_sign_in_btn:
                    progressBar.show();
                    googleLogin();
                    break;
                case R.id.facebook_sign_in_btn:
                    progressBar.show();
                    facebookLogin();
                    break;
            }
        }
    };

    private void login() {
        String email = ((EditText) findViewById(R.id.emailEditText)).getText().toString();
        String password = ((EditText) findViewById(R.id.pwEditText)).getText().toString();
        if (email.length() > 0 && password.length() > 0) {

            //검증을 위해 아래 코드를 실행함
            if(!loginValCheck(email,password)){
                return ;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                uid=task.getResult().getUser().getUid();
                                registerPushTokenAndStartActivity();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT).show();
                                Log.e("login failed",task.getResult().toString());
                            }

                            // ...
                        }
                    });
        }else{
            Toast.makeText(this, getString(R.string.login_failed),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data); //facebook
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_LOGIN_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            uid=mAuth.getCurrentUser().getUid();
                            registerPushTokenAndStartActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
    private void googleLogin(){
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent,GOOGLE_LOGIN_CODE);
    }

    private void facebookLogin(){
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });
    }

    // [START auth_with_facebook]
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        // [START_EXCLUDE silent]
        // [END_EXCLUDE]

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            uid=mAuth.getCurrentUser().getUid();
                            registerPushTokenAndStartActivity();
                        } else {

                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void startActivity(Class c){
        Intent intent = new Intent(this, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void registerPushTokenAndStartActivity() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        token = task.getResult().getToken();
                        Map<String, Object> map = new HashMap<>();
                        map.put("token", token);
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("uid",uid);
                        intent.putExtra("token",token);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                });
    }


    private boolean loginValCheck(final String email, final String password){
        //키보드 내리기
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        /*for preventing SQL injection, some special characters are restricted.*/
        String pwRestricted = "(?=.*[@#$%*=,.;])";
        Matcher Rmatcher = Pattern.compile(pwRestricted).matcher(password);

        if(Rmatcher.find()) {
            Toast.makeText(getApplicationContext(), "비밀번호에 다음과 같은 특수문자를 사용할 수 없습니다. \"@#$%*=,.;\"", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(password.contains(email)) {
            Toast.makeText(getApplicationContext(), "비밀번호가 이메일 중 일부를 포함할 수 없습니다.",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(password.contains(" ")) {
            Toast.makeText(getApplicationContext(), "비밀번호는 공백을 포함할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
