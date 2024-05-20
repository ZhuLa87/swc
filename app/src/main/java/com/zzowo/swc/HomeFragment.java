package com.zzowo.swc;

import static com.zzowo.swc.ConnectWheelChairActivity.connectedThread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zzowo.swc.BtThread.ConnectedThread;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements MainActivity.OnBeepStatusChangedListener {
    private static final String TAG = "HomeFragment";
    private Toolbar toolbar;
    private AppCompatActivity activity;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private TextView toolBarTextView;
    private View btnBeep, btnLocation, btnAlarm;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        initPreferences();

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private void initPreferences() {
        sp = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // init
        initStatusBarColor();
        initToolBar(rootView);
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 指定對應按鈕
        btnBeep = rootView.findViewById(R.id.func_01);
        btnLocation = rootView.findViewById(R.id.func_02);
        btnAlarm = rootView.findViewById(R.id.func_03);

        intiButtonColor(btnLocation);
        ((MainActivity) getActivity()).setOnBeepStatusChangedListener(this);

        // 按鈕監聽
        btnBeep.setOnClickListener(view -> {
            if (connectedThread != null) {
                connectedThread.btWriteString("beep", "1", "toggle");
            } else {
                Toast.makeText(getContext(), R.string.swc, Toast.LENGTH_SHORT).show();
            }
        });

        btnLocation.setOnClickListener(view -> {
            Boolean allowStoreLocation = sp.getBoolean("allowStoreLocation", true);
            allowStoreLocation = !allowStoreLocation;

            // 更改按鈕顏色
            if (allowStoreLocation) {
                btnLocation.getBackground().setColorFilter(Color.parseColor("#00FF00"), android.graphics.PorterDuff.Mode.MULTIPLY);
            } else {
                btnLocation.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY);
            }

            // 儲存設定
            editor.putBoolean("allowStoreLocation", allowStoreLocation);
            editor.commit();
            Toast.makeText(getContext(), allowStoreLocation ? "Location tracking enabled" : "Location tracking disabled", Toast.LENGTH_SHORT).show();
        });

        btnAlarm.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("警報將於3秒內送出，是否取消")
                    .setCancelable(false)
                    .setPositiveButton("取消", (dialog, which) -> {
                        dialog.cancel();
                    })
                    .setNegativeButton("送出", (dialog, which) -> {
                        sendAlert();
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            Handler alertHandler = new Handler();
            alertHandler.postDelayed(() -> {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    sendAlert();
                }
            }, 3000);
        });

        return rootView;
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
    }

    private void initToolBar(View view) {
        // 讀取資料
        String mySWCNameDefault = getString(R.string.mySWC_name_default);
        String mySWCName = sp.getString("mySWCName", mySWCNameDefault);

        toolbar = view.findViewById(R.id.toolbar);
        activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        toolBarTextView = view.findViewById(R.id.toolbar_text);
        toolBarTextView.setText(mySWCName);
        activity.getSupportActionBar().setTitle("");
    }

    private void intiButtonColor(View btnLocation) {
        Boolean allowStoreLocation = sp.getBoolean("allowStoreLocation", true);

        // 更改按鈕顏色
        if (allowStoreLocation) {
            btnLocation.getBackground().setColorFilter(Color.parseColor("#00FF00"), android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            btnLocation.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Boolean primaryUser = sp.getBoolean("primaryUser", false);
        if (!primaryUser) {
            Toast.makeText(getContext(), "You are not a prime user", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (id == R.id.toolbar_rename) {
            toolbarRename();
        } else if (id == R.id.toolbar_add) {
            toolbarAdd();
        } else if (id == R.id.toolbar_disconnect) {
            toolbarDisconnect();
        }
        return true;
    }

    // 重新命名按鍵
    private void toolbarRename() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View edit_text_dialog = inflater.inflate(R.layout.edit_text_dialog, null);
        builder.setView(edit_text_dialog);
        builder.setIcon(R.drawable.baseline_edit_24);
        builder.setTitle(R.string.toolbar_menu_rename);
        builder.setPositiveButton("Save", (dialog, i) -> {
            EditText editText = edit_text_dialog.findViewById(R.id.edit_text);
            if (editText.length() > 0) {
                String newTitle = editText.getText().toString().trim();
                // 儲存新名稱
                editor.putString("mySWCName", newTitle);
                editor.commit();
                // 更新介面
                toolBarTextView.setText(newTitle);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // this method is called when user click on negative button.
            dialog.cancel();
        });

        builder.show();
    }

    private void toolbarAdd() {
        // switch to new activity
        Intent intent = new Intent(getContext(), ConnectWheelChairActivity.class);
        startActivity(intent);
    }

    private void sendAlert() {
        Log.d(TAG, "sendAlert: " + user.getUid());

        // 藍芽警報
        if (connectedThread != null) {
            connectedThread.btWriteString("alarm", "99", "Alert");
        }

        // 通知警報
        String provider = user.getProviderData().get(1).getProviderId();
        String userDisplayName = "";
        if (provider.contains("google.com")) {
            userDisplayName = user.getDisplayName();
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("from", user.getUid());
        notification.put("title", "緊急通知!");
        notification.put("body", userDisplayName.isEmpty()?"輪椅使用者":userDisplayName + "發出警報訊號，請立即前往查看。");
        notification.put("tag", "alert");
        notification.put("timestamp", new Timestamp(new Date()));

        db.collection("Notifications")
                .add(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Alert sent to server", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send alert", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toolbarDisconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    @Override
    public void onBeepStatusChanged(Boolean isOn) {
        if (isOn) {
            btnBeep.getBackground().setColorFilter(Color.parseColor("#00FF00"), android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            btnBeep.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
    }
}