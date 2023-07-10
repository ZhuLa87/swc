package com.zzowo.swc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignUpPage extends AppCompatActivity {
    private static final String TAG = "SIGNUP";
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonSignUp;
    private ImageView buttonGoogleLogin;
    private ProgressBar progressBar;
    private TextView loginNow;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            welcomeToast();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);

        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonSignUp = findViewById(R.id.sign_up_btn);
        progressBar = findViewById(R.id.progressBar);
        loginNow = findViewById(R.id.loginNow);
        buttonGoogleLogin = findViewById(R.id.login_google);

//        init
        initPreferences();
        initGoogleSignUp();

        loginNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginPage.class);
                startActivity(intent);
                finish();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = editTextEmail.getText().toString();
                password = editTextPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    progressBar.setVisibility(View.GONE);
                    editTextEmail.setError(getString(R.string.input_empty_error));
                    Toast.makeText(SignUpPage.this, R.string.toast_enter_email, Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(password)) {
                    progressBar.setVisibility(View.GONE);
                    editTextPassword.setError(getString(R.string.input_empty_error));
                    Toast.makeText(SignUpPage.this, R.string.toast_enter_password, Toast.LENGTH_SHORT).show();
                    return;
                }


                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    saveUserInfoPreferences(mAuth.getCurrentUser());
                                    welcomeToast();
                                    // 切換到主頁面
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(SignUpPage.this, R.string.toast_registration_failed,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        buttonGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);
            }
        });
    }

    private void initPreferences() {
        sp = this.getSharedPreferences("data", MODE_PRIVATE);
        editor = sp.edit();
    }

    private void initGoogleSignUp() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        progressBar.setVisibility(View.GONE);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            saveUserInfoPreferences(mAuth.getCurrentUser());
                            welcomeToast();

//                             TODO: Bug here, can't update to database
//                            UsersData usersData = new UsersData();
//                            usersData.setUserId(user.getUid());
//                            usersData.setName(user.getDisplayName());
//                            usersData.setProfile(user.getPhotoUrl().toString());
//
//                            database.getReference().child("Users").child(user.getUid()).setValue(users);

                            Intent intent = new Intent(SignUpPage.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignUpPage.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserInfoPreferences(FirebaseUser user) {
        String userUid = user.getUid();
        editor.putString("userUid", userUid);
        editor.commit();
        Log.d(TAG, "saveUserUid: " + userUid);
    }

    private void welcomeToast() {
        FirebaseUser user = mAuth.getCurrentUser();
        String provider = user.getProviderData().get(1).getProviderId();
        String welcomeMsg = getString(R.string.toast_account_created);
        if (provider.contains("password")) {
            String[] userName = user.getEmail().split("@");
            welcomeMsg += userName[0];
        } else if (provider.contains("google.com")) {
            welcomeMsg += user.getDisplayName();
        }
        Log.d(TAG, "Hi: " + welcomeMsg);
        Toast.makeText(this, welcomeMsg, Toast.LENGTH_SHORT).show();
    }
}