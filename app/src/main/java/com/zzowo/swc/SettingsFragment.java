package com.zzowo.swc;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

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
    private FirebaseFirestore db;
    private SharedPreferences sp1;
    private SharedPreferences.Editor editor1;
    private SharedPreferences sp2;
    private SharedPreferences.Editor editor2;
    private TextView userIdentity;
    private TextView userName;
    private TextView userEmail;
    private TextView userUid;
    private TextView copyUid;
    private LottieAnimationView lottieAnimationView;
    private ImageView userAvatar;
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
        userIdentity = rootView.findViewById(R.id.user_identity);
        userName = rootView.findViewById(R.id.user_name);
        userEmail = rootView.findViewById(R.id.user_email);
        userUid = rootView.findViewById(R.id.user_uid);
        copyUid = rootView.findViewById(R.id.copy_prime_uid);
        switchNotification = rootView.findViewById(R.id.switch_notification);
        textVersion = rootView.findViewById(R.id.text_version);

        mAuth = FirebaseAuth.getInstance(); // 第一次寫少了這行, Debug1個半小時才找到, 所以我決定給他個註解; 附上錯誤訊息"java.lang.NullPointerException: Attempt to invoke virtual method 'com.google.firebase.auth.FirebaseUser com.google.firebase.auth.FirebaseAuth.getCurrentUser()' on a null object reference"
        user = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();

//        init
        initStatusBarColor();
        initPreferences();
        initAccountInfo(user);
        initUserIdentity();
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
        View optionNotification = rootView.findViewById(R.id.option_notification);

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
                                    editor1.putBoolean("switch_state_notification", true).commit();
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
                    editor1.putBoolean("switch_state_notification", false).commit();
                }
            }
        });

//        profileUpdates();
        selectUpdatePassword(rootView);
        selectIdentity(rootView);
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
        sp1 = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor1 = sp1.edit();

        sp2 = getActivity().getSharedPreferences("wheelchair", Context.MODE_PRIVATE);
        editor2 = sp2.edit();
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
    }

    private void initUserIdentity() {
        boolean primaryUser = sp1.getBoolean("primaryUser", true); // default: true

        if (primaryUser) {
            userIdentity.setText(R.string.wheelchair_user);
        } else {
            userIdentity.setText(R.string.caregiver);
        }
    }

    private void initSwitchCompat() {
        // sp
        boolean switchStateNotification = sp1.getBoolean("switch_state_notification", false); // default: false

        Log.d(TAG, "Get switchNotification: " + switchStateNotification);

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

    private void selectUpdatePassword(View view) {
        View selectUpdatePassword = view.findViewById(R.id.option_update_pwd);
        selectUpdatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 若使用Google登入, 則無法更改密碼
                String provider = user.getProviderData().get(1).getProviderId();
                if (provider.contains("google.com")) {
                    Toast.makeText(getContext(), R.string.google_account_cannot_change_password, Toast.LENGTH_LONG).show();
                    return;
                }

                showChangePasswordDialog();
            }
        });
    }

    private void showChangePasswordDialog() {
        // 使用自訂對話框
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // 取得自訂的彈出介面布局
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        dialog = builder.create();

        // 取得介面元件
        final EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
        final EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // 設定EditText的輸入類型
        editTextOldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editTextNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 取得輸入的密碼
                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();

                // 檢查輸入之密碼格式
                if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(getContext(), R.string.password_empty_error, Toast.LENGTH_SHORT).show();
                    return;
                } else if (newPassword.length() < 6) {
                    Toast.makeText(getContext(), R.string.password_length_error, Toast.LENGTH_SHORT).show();
                    return;
                } else if (oldPassword.equals(newPassword)) {
                    Toast.makeText(getContext(), R.string.password_same_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                updatePassword(oldPassword, newPassword);

                // 關閉對話視窗
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 關閉對話視窗
                dialog.dismiss();
            }
        });

        // 顯示對話視窗

        dialog.show();
    }

    private void updatePassword(String oldPassword, String newPassword) {

        // re-authenticate
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String accountEmail = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(accountEmail, oldPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "User re-authenticated.");

                        user.updatePassword(newPassword)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User password updated.");
                                            Toast.makeText(getContext(), R.string.operation_successful, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.w(TAG, "Error updating password", task.getException());
                                            Toast.makeText(getContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error updating password", e);
                                        Toast.makeText(getContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error re-authenticating user", e);
                        Toast.makeText(getContext(), "Old password wrong!", Toast.LENGTH_SHORT).show();
                    }});
    }

    private void selectIdentity(View view) {
        View selectIdentity = view.findViewById(R.id.option_select_identity);
        selectIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showIdentitySelectionDialog();
            }
        });
    }

    private void showIdentitySelectionDialog() {
        // 建立一個AlertDialog.Builder物件
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.select_identity);
        builder.setIcon(R.drawable.baseline_person_24);

        final CharSequence[] options = {getString(R.string.option_wheelchair_user), getString(R.string.option_caregiver)};

        builder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedOption = options[which].toString();

                handleUserSelection(selectedOption);

                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void handleUserSelection(String selectedOption) {
        Boolean primaryUser = null;

        Log.d(TAG, "Selected: " + selectedOption);
        if (selectedOption.equals(getString(R.string.option_wheelchair_user))) {
            editor1.putBoolean("primaryUser", true).commit();
            primaryUser = true;
        } else if (selectedOption.equals(getString(R.string.option_caregiver))) {
            editor1.putBoolean("primaryUser", false).commit();
            primaryUser = false;
        }
        storeUserIdentityInFirestore(primaryUser);
    }

    private void storeUserIdentityInFirestore(Boolean primaryUser) {
        Map<String, Object> data = new HashMap<>();
        data.put("primaryUser", primaryUser);
        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "primaryUser added with ID: " + user.getUid());
                            updateUserIdentityUI(primaryUser);
                            Toast.makeText(getContext(), R.string.operation_successful, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Error adding document to Firestore", task.getException());

                            Toast.makeText(getContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                        }
                    }})
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserIdentityUI(Boolean primaryUser) {
        if (primaryUser) {
            userIdentity.setText(R.string.wheelchair_user);
        } else {
            userIdentity.setText(R.string.caregiver);
        }
    }

    private void btn_logout(View view) {
        View logout = view.findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
    }

    public void logout() {
        try {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
        } catch (Exception e) { }

//        Delete Saved preferences.
        editor1.clear().apply();
        editor2.clear().apply();

        Toast.makeText(getContext(), R.string.cya, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), LoginPage.class);
        startActivity(intent);
        getActivity().finish();
    }
}