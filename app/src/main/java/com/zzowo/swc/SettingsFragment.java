package com.zzowo.swc;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

//在 Firebase 中管理用户
//> https://firebase.google.com/docs/auth/android/manage-users?hl=zh-cn

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SETTINGS";
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private TextView userName; // userName 未製作
    private TextView userEmail;
    private TextView userUid;
    private TextView copyUid;
    private LottieAnimationView lottieAnimationView;
    private ImageView userAvatar;
    private SwitchCompat switchNightMode;
    private SwitchCompat switchNotification;
    private TextView textVersion;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
        lottieAnimationView = rootView.findViewById(R.id.animation_avatar);
        userAvatar = rootView.findViewById(R.id.user_avatar);
        userName = rootView.findViewById(R.id.user_name);
        userEmail = rootView.findViewById(R.id.user_email);
        userUid = rootView.findViewById(R.id.user_uid);
        copyUid = rootView.findViewById(R.id.copy_uid);
        switchNightMode = rootView.findViewById(R.id.switch_night_mode);
        switchNotification = rootView.findViewById(R.id.switch_notification);
        textVersion = rootView.findViewById(R.id.text_version);

        mAuth = FirebaseAuth.getInstance(); // 第一次寫少了這行, Debug1個半小時才找到, 所以我決定給他個註解; 附上錯誤訊息"java.lang.NullPointerException: Attempt to invoke virtual method 'com.google.firebase.auth.FirebaseUser com.google.firebase.auth.FirebaseAuth.getCurrentUser()' on a null object reference"
        user = mAuth.getCurrentUser();

//        init
        initStatusBarColor();
        initPreferences();
        initAccountInfo(user);
        initSwitchCompat();
        initOtherInfo();

        // Account info button
        copyUid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("uid", user.getUid()); // label為系統可見標籤, 非使用者可見標籤
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getContext(), "UID 已複製", Toast.LENGTH_SHORT).show();
            }
        });

        // Settings Options
        View optionNightMode = rootView.findViewById(R.id.option_night_mode);
        View optionNotification = rootView.findViewById(R.id.option_notification);

        optionNightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchNightMode.toggle();
                Log.d(TAG, "Set switchNightMode to " + switchNightMode.isChecked());

                Boolean stateNightMode = switchNightMode.isChecked();
                if (stateNightMode) {
                    // 如果切換後開關狀態為 "開 / On"
                    editor.putBoolean("switch_state_night_mode", true).commit();
                } else {
                    // 如果切換後開關狀態為 "關/ Off"
                    editor.putBoolean("switch_state_night_mode", false).commit();
                }
            }
        });

        optionNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchNotification.toggle();
                Log.d(TAG, "Set switchNotification to " + switchNotification.isChecked());

                Boolean stateNotification = switchNotification.isChecked();
                if (stateNotification) {
                    // 如果切換後開關狀態為 "開 / On"
                    Log.d(TAG, "onClick: Switch Notification set to: " + switchNotification.isChecked());

                    // Grant notification permission
                    Dexter.withContext(getActivity().getApplicationContext()).withPermission(Manifest.permission.POST_NOTIFICATIONS)
                            .withListener(new PermissionListener() {
                                @Override
                                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                    editor.putBoolean("switch_state_notification", true).commit();
                                    Log.d(TAG, "Saved switch_state_notification: true");
                                }

                                @Override
                                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                    switchNotification.setChecked(false);
                                }

                                @Override
                                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                    permissionToken.continuePermissionRequest();
                                }
                            }).check();

                } else {
                    // 如果切換後開關狀態為 "關/ Off"
                    editor.putBoolean("switch_state_notification", false).commit();
                }
            }
        });

//        profileUpdates();
        btn_logout(rootView);

        return rootView;
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorBackground));
    }

    private void initPreferences() {
        sp = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    private void initAccountInfo(FirebaseUser user) {
        // get provider
        String provider = user.getProviderData().get(1).getProviderId();

        if (provider.contains("google.com")) { // Can't use provider == "google.com", but idk why
            // 顯示使用者頭像
            Uri userAvatarUrl = user.getPhotoUrl();
            Picasso.get().load(userAvatarUrl).into(userAvatar);
            lottieAnimationView.setVisibility(View.INVISIBLE);
            userAvatar.setVisibility(View.VISIBLE);

            // 顯示使用者名稱
            String userDisplayName = user.getDisplayName();
            userName.setText(userDisplayName);
            userName.setVisibility(View.VISIBLE);
        } else if (provider.contains("password")) {
            // sign in with password
        }

        userEmail.setText(user.getEmail());
        userUid.setText("UID: " + user.getUid().substring(0, 6) + "...");
//            userEmailVerified.setText(user.isEmailVerified()?"True":"False");
    }

    private void initSwitchCompat() {
        // sp
        boolean switchStateNightMode = sp.getBoolean("switch_state_night_mode", false); // default: false
        boolean switchStateNotification = sp.getBoolean("switch_state_notification", false); // default: false

        Log.d(TAG, "Get switchNightMode: " + switchStateNightMode);
        Log.d(TAG, "Get switchNotification: " + switchStateNotification);

        if (switchStateNightMode) {
            // Do something for switch (Night Mode) is selected on
            switchNightMode.setChecked(true);
            Log.d(TAG, "Loaded switchNightMode: " + true);
        } else {
            // switch off
            Log.d(TAG, "Loaded switchNightMode: " + false);
        }

        if (switchStateNotification) {
            // Check permission first
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                switchNotification.setChecked(true);
                Log.d(TAG, "Loaded switchNotification: " + true);
            } else {
                switchNotification.setChecked(false);
            }
        } else {
            // switch off
            Log.d(TAG, "Loaded switchNotification: " + false);
        }
    }

    private void initOtherInfo() {
        PackageManager manager = getActivity().getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(getActivity().getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getPackageInfo: ", e);
        }

        String versionName = info.versionName;

        textVersion.setText(versionName);
    }

    private void profileUpdates() {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName("")
                .setPhotoUri(Uri.parse(""))
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                        }
                    }
                });
    }

    private void btn_logout(View view) {
        View logout = view.findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                mGoogleSignInClient.signOut();
                logout();
            }
        });
    }

    private void logout() {
//        Delete Saved preferences.
        editor.remove("userUid")
                .remove("mySWC_name")
                .apply();
        Toast.makeText(getContext(), R.string.cya, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), LoginPage.class);
        startActivity(intent);
        getActivity().finish();
    }
}