package com.zzowo.swc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginPage extends AppCompatActivity {
    private static final String TAG = "LOGIN";
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private ImageView buttonGoogleLogin;
    private ProgressBar progressBar;
    private TextView forgotPassword;
    private TextView signUpNow;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    @Override
    public void onStart() {
        super.onStart();
        // 初始化 Firebase 應用程式的方法, 該方法用於建立與 Firebase 相關的基礎設定, 並設置應用程式與 Firebase 之間的連接
        FirebaseApp.initializeApp(this);
        // 初始化應用檢查
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase FireStore
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.login_btn);
        progressBar = findViewById(R.id.progressBar);
        signUpNow = findViewById(R.id.signUpNow);
        buttonGoogleLogin = findViewById(R.id.login_google);
        forgotPassword = findViewById(R.id.forgotPassword);

//        init
        initStatusBarColor();
        initPreferences();
        initGoogleSignUp();

        signUpNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(getApplicationContext(), SignUpPage.class);
                startActivity(intent);
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
                } else if (!email.matches(emailPattern)) {
                    progressBar.setVisibility(View.GONE);
                    editTextPassword.setError(getString(R.string.email_pattern_wrong));
                    Toast.makeText(LoginPage.this, R.string.email_pattern_wrong, Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
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
                                    Toast.makeText(LoginPage.this, R.string.toast_authentication_failed,
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

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(getApplicationContext(), ResetPasswordPage.class);
                startActivity(intent);
            }
        });
    }

    private void initStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorBackground));
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

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 從Google登入取得結果, "task" 為非同步的結果, 當錯誤發生時會拋出 ApiException
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginPage.this, getText(R.string.login_cancel), Toast.LENGTH_SHORT).show();
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
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            saveUserInfoPreferences(firebaseUser);
                            storeUserInfoInFirestoreAndRedirect(firebaseUser);
                        } else {
                            Toast.makeText(LoginPage.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Google auth error: " + e);
                        try {
                            mAuth.signOut();
                            mGoogleSignInClient.signOut();
                        } catch (Exception ex) {
                            Log.d(TAG, "Google auth logout error: " + ex);
                        }
                    }
                });
    }

    private void storeUserInfoInFirestoreAndRedirect(FirebaseUser firebaseUser) {
        UsersData usersData = new UsersData();
        usersData.setEmail(firebaseUser.getEmail());
        usersData.setFullName(firebaseUser.getDisplayName());

        // 取得 Firestore 數據庫的引用, 指定集合為 "users", 文件 ID 為使用者的 UID
        db.collection("users").document(firebaseUser.getUid())
                .set(usersData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressBar.setVisibility(View.GONE);
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        welcomeToast();

                        Intent intent = new Intent(LoginPage.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: 添加資料庫錯誤時的處裡邏輯
                        Log.w(TAG, "An error occurred while writing to the database", e);
                        Toast.makeText(getApplicationContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                        logout();
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

    private void logout() {
        try {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
        } catch (Exception e) {
            Log.d(TAG, "Logout error: " + e);
        }
        progressBar.setVisibility(View.GONE);
    }
}