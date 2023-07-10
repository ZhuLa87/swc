package com.zzowo.swc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

public class LoginPage extends AppCompatActivity {
    private static final String TAG = "LOGIN";
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
//    FirebaseDatabase database;
    GoogleSignInClient mGoogleSignInClient;
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    ImageView buttonGoogleLogin;
    ProgressBar progressBar;
    TextView signUpNow;

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
        setContentView(R.layout.activity_login_page);

        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.login_btn);
        progressBar = findViewById(R.id.progressBar);
        signUpNow = findViewById(R.id.signUpNow);
        buttonGoogleLogin = findViewById(R.id.login_google);
//        database = FirebaseDatabase.getInstance();

        signUpNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignUpPage.class);
                startActivity(intent);
                finish();
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = editTextEmail.getText().toString();
                password = editTextPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    progressBar.setVisibility(View.GONE);
                    editTextEmail.setError(getString(R.string.input_empty_error));
                    Toast.makeText(LoginPage.this, R.string.toast_enter_email, Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(password)) {
                    progressBar.setVisibility(View.GONE);
                    editTextPassword.setError(getString(R.string.input_empty_error));
                    Toast.makeText(LoginPage.this, R.string.toast_enter_password, Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    String[] userName = mAuth.getCurrentUser().getEmail().split("@");
                                    Toast.makeText(LoginPage.this, getString(R.string.toast_welcome_back) + userName[0],
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginPage.this, R.string.toast_authentication_failed,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        buttonGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);
            }
        });
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
                            welcomeToast();

//                             TODO: Bug here, can't update to database
//                            UsersData usersData = new UsersData();
//                            usersData.setUserId(user.getUid());
//                            usersData.setName(user.getDisplayName());
//                            usersData.setProfile(user.getPhotoUrl().toString());
//
//                            database.getReference().child("Users").child(user.getUid()).setValue(users);

                            Intent intent = new Intent(LoginPage.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginPage.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void welcomeToast() {
        FirebaseUser user = mAuth.getCurrentUser();
        String provider = user.getProviderData().get(1).getProviderId();
        String userDisplayName = user.getDisplayName();
        String welcomeMsg = getString(R.string.toast_account_created);
        if (provider.contains("password")) {
            String[] userName = user.getEmail().split("@");
            welcomeMsg += userName[0];
        } else if (provider.contains("google.com")) {
            welcomeMsg += userDisplayName;
        }
        Log.d(TAG, "welcomeMsg: " + welcomeMsg);
        Toast.makeText(this, welcomeMsg, Toast.LENGTH_SHORT).show();
    }
}