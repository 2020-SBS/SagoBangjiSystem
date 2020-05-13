package com.example.samplesbs.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.samplesbs.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "Signup";
    private FirebaseAuth mAuth;
    private Button signUpBtn, gotoLoginBtn;
    private LinearLayout mainLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mainLayout = findViewById(R.id.main_layout);
        signUpBtn = findViewById(R.id.signUpBtn);
        gotoLoginBtn = findViewById(R.id.gotoLoginBtn);
        signUpBtn.setOnClickListener(onClickListener);
        gotoLoginBtn.setOnClickListener(onClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }
    @Override public void onBackPressed(){
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    View.OnClickListener onClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.signUpBtn:
                    signUp();
                    break;
                case R.id.gotoLoginBtn:
                    gotoLogin();
                    break;
            }
        }
    };

    private void signUp() {
        String email = ((EditText) findViewById(R.id.emailEditText)).getText().toString();
        String password = ((EditText) findViewById(R.id.pwEditText)).getText().toString();
        String passwordCheck = ((EditText) findViewById(R.id.pwCheckEditText)).getText().toString();

        if(!signUpRegex(email,password)){
            return ;
        }

        if (email.length() > 0 && password.length() > 0 && passwordCheck.length() > 0) {
            if (password.equals(passwordCheck)) {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    gotoLogin();
                                    //성공시 UI
                                } else {
                                    Toast.makeText(getApplicationContext(), getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                    task.getException().printStackTrace();
                                    //실패시 UI
                                }
                                // ...
                            }
                        });
            } else {
                Toast.makeText(this, getString(R.string.password_not_equal), Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, getString(R.string.signup_null_failed), Toast.LENGTH_SHORT).show();
    }
    private void gotoLogin(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private boolean signUpRegex(final String email, final String password){
        //키보드 내리기
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        String pwPattern = "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-z]).{9,15}$";
        Matcher matcher = Pattern.compile(pwPattern).matcher(password);

        pwPattern = "(.)\\1\\1\\1";
        Matcher matcher2 = Pattern.compile(pwPattern).matcher(password);


        if(!matcher.matches()) {
            Toast.makeText(getApplicationContext(), "영문, 숫자, 특수문자를 조합하여 비밀번호를 생성해야 합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(matcher2.find()) {
            Toast.makeText(getApplicationContext(), "비밀번호에 같은 문자를 네개 이상 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
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