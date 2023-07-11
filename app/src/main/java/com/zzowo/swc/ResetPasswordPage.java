package com.zzowo.swc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordPage extends AppCompatActivity {
    private static final String TAG = "RESET_PASSWORD";
    private FirebaseAuth mAuth;
    private TextInputEditText editTextEmail;
    private TextView backToLogin;
    private Button buttonNext;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password_page);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);
        editTextEmail = findViewById(R.id.email);
        buttonNext = findViewById(R.id.reset_password_btn);
        backToLogin = findViewById(R.id.back_to_login);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email;
                email = editTextEmail.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    progressBar.setVisibility(View.GONE);
                    editTextEmail.setError(getString(R.string.input_empty_error));
                    Toast.makeText(ResetPasswordPage.this, R.string.toast_enter_email, Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Password reset email sent.");
                                    TextView resetText = findViewById(R.id.success_email);
                                    resetText.setText(email);
                                    findViewById(R.id.reset_input_view).setVisibility(View.GONE);
                                    findViewById(R.id.success_layout).setVisibility(View.VISIBLE);
                                } else
                                    Toast.makeText(ResetPasswordPage.this, R.string.account_not_found, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(getApplicationContext(), LoginPage.class);
                startActivity(intent);
            }
        });
    }
}