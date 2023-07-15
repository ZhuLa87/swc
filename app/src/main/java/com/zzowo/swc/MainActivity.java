package com.zzowo.swc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zzowo.swc.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Night mode is enable by default
        super.onCreate(savedInstanceState);

//        init
        initAuth();
        bottomNavSetup();
    }

    private void initAuth() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginPage.class);
            startActivity(intent);
            finish();
        }

        // 'FirebaseAuth.getInstance().getCurrentUser().getProviderId()' always return 'firebase'
        // Solution: https://www.qiniu.com/qfans/qnso-49484003#comments
        String providerId = user.getProviderData().get(1).getProviderId();
        Log.d("zhu", "ProviderId: " + providerId);
        Log.d("zhu", "UserName: " + user.getDisplayName());
        Log.d("zhu", "UserEmail: " + user.getEmail());
        Log.d("zhu", "UserUID: " + user.getUid());
    }

    private void bottomNavSetup() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

//        底部導覽列
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.bottom_map) {
                Toast.makeText(this, "地圖頁面存在Bug, 待重製", Toast.LENGTH_SHORT).show();
                replaceFragment(new MapFragment());
            } else if (itemId == R.id.bottom_binding) {
                replaceFragment(new BindingFragment());
            } else if (itemId == R.id.bottom_settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

}