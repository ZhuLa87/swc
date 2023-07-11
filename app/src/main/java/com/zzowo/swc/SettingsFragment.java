package com.zzowo.swc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

//在 Firebase 中管理用户
//> https://firebase.google.com/docs/auth/android/manage-users?hl=zh-cn

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private TextView userName; // userName 未製作
    private TextView userEmail;
    private TextView userUid;
    private LottieAnimationView lottieAnimationView;
    private ImageView userAvatar;

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

        mAuth = FirebaseAuth.getInstance(); // 第一次寫少了這行, Debug1個半小時才找到, 所以我決定給他個註解; 附上錯誤訊息"java.lang.NullPointerException: Attempt to invoke virtual method 'com.google.firebase.auth.FirebaseUser com.google.firebase.auth.FirebaseAuth.getCurrentUser()' on a null object reference"
        user = mAuth.getCurrentUser();

//        init
        initPreferences();
        initAccountInfo(user);


//        設定介面未完成, 登出按鈕僅供測試
//        profileUpdates();
        btn_logout(rootView);

        return rootView;
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
                            Log.d("SETTINGS", "User profile updated.");
                        }
                    }
                });
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
        userUid.setText("UID: " + user.getUid());
//            userEmailVerified.setText(user.isEmailVerified()?"True":"False");
    }

    private void btn_logout(View view) {
        Button logout = view.findViewById(R.id.logout);
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

        Intent intent = new Intent(getContext(), LoginPage.class);
        startActivity(intent);
        getActivity().finish();
    }
}