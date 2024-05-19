package com.zzowo.swc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BindingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BindingFragment extends Fragment {
    private static final String TAG = "BindingFragment";
    private View rootView;
    private RecyclerView recyclerView;
    private List<String> recyclerViewData = new ArrayList<>();
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private boolean primaryUser;
    private String selfUid;
    private RecyclerView mRecyclerView;
    private TextView userIdentity;
    private TextView userName;
    private ImageView userAvatar;
    private LottieAnimationView lottieAnimationView;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public BindingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NotificationsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BindingFragment newInstance(String param1, String param2) {
        BindingFragment fragment = new BindingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    public void onCreate(LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_binding, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        userIdentity = rootView.findViewById(R.id.user_identity);
        userName = rootView.findViewById(R.id.user_name);
        userAvatar = rootView.findViewById(R.id.user_avatar);
        lottieAnimationView = rootView.findViewById(R.id.animation_avatar);
        recyclerView = rootView.findViewById(R.id.recyclerView);

        db = FirebaseFirestore.getInstance();

        //init
        initPreferences();
        initStatusBarColor();
        initUserIdentity();
        initAccountInfo(user);
        initOperationView(rootView);
        initRecyclerView();

        updateRecyclerViewData();

        return rootView;
    }

    private void initPreferences() {
        sp = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = sp.edit();

        primaryUser = sp.getBoolean("primaryUser", true);
        selfUid = sp.getString("userUid", "");
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
    }

    private void initAccountInfo(FirebaseUser user) {
        // get provider
        String provider = user.getProviderData().get(1).getProviderId();

        if (provider.contains("google.com")) {
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
    }

    private void initUserIdentity() {
        boolean primaryUser = sp.getBoolean("primaryUser", true); // default: true

        if (primaryUser) {
            userIdentity.setText(R.string.wheelchair_user);
        } else {
            userIdentity.setText(R.string.caregiver);
        }
    }

    private void initOperationView(View rootView) {
        View operationViewPrimary = rootView.findViewById(R.id.op_prime_user);
        View operationViewSecondary = rootView.findViewById(R.id.op_secondary_user);

        if (primaryUser) {
            // primaryUser
            operationViewPrimary.setVisibility(View.VISIBLE);
            operationViewSecondary.setVisibility(View.INVISIBLE);

            final Boolean[] isUseClipboard = {true};
            EditText inputUid = rootView.findViewById(R.id.input_uid);
            TextView selectBtn = rootView.findViewById(R.id.select_button);

            inputUid.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        selectBtn.setText("Confirm");
                        isUseClipboard[0] = false;
                    } else {
                        inputUid.setHint(R.string.input_uid);
                    }
                }
            });

            selectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Confirm button clicked!");

                    // 讀取剪貼簿內容
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData == null || !isUseClipboard[0]) {
                        return;
                    }
                    ClipData.Item item = clipData.getItemAt(0);
                    String text = item.getText().toString();
                    inputUid.setText(text);

                    // 確認按鈕
                    String input = inputUid.getText().toString();
                    // 檢查 UID 是否合規
                    Log.d(TAG, "Input length: " + input.length());
                    if (input.length() != 28) {
                        Toast.makeText(getContext(), "Uid invalid", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (input.equals(selfUid)) {
                        Toast.makeText(getContext(), "Uid is the same as you", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 比對輸入之UID是否存在
                    db.collection("users").document(input).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // 判斷該使用者是否為主使用者
                                    Boolean isUidUserPrimary = task.getResult().getBoolean("primaryUser");
                                    if (isUidUserPrimary == null) {
                                        Toast.makeText(getContext(), "UID not found", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Input UID not found!");
                                        return;
                                    }
                                    if (isUidUserPrimary) {
                                        Toast.makeText(getContext(), "Primary user cannot be bound", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Primary user cannot be bound!");
                                        return;
                                    }

                                    Log.d(TAG, "Input UID found!");

                                    // 上傳輸入之UID
                                    storeBoundUidToFirestore(input);

                                } else {
                                    Toast.makeText(getContext(), "UID not found", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Input UID not found!");
                                }
                            });
                }
            });
        } else {
            // SecondaryUser
            operationViewPrimary.setVisibility(View.INVISIBLE);
            operationViewSecondary.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);

            // 處理操作區內容
            TextView primeUid = rootView.findViewById(R.id.prime_uid);
            TextView copyPrimeUid = rootView.findViewById(R.id.copy_prime_uid);

            primeUid.setText("UID: " + selfUid.substring(0, 8) + "...");
            copyPrimeUid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Copy UID button clicked!");
                    // 複製 UID
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("uid", selfUid); // label為系統可見標籤, 非使用者可見標籤
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getContext(), R.string.uid_copied, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void initRecyclerView() {
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new ItemSpacingDecoration(getContext(), 10)); // 2dp 的間距
    }

    private void updateRecyclerViewData() {

        db.collection("users").document(selfUid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // 將取得的資料放入 recyclerViewData()
                        List<String> boundUsers = (List<String>) task.getResult().get("boundUsers");
                        if (boundUsers != null) {
                            for (String boundUser : boundUsers) {
                                recyclerViewData.add(boundUser.substring(0, 15) + "...");
                                ItemAdapter adapter = new ItemAdapter(recyclerViewData);
                                mRecyclerView.setAdapter(adapter);
                            }
                        }
                    }
                });
    }

    private void storeBoundUidToFirestore(String targetUid) {
        Log.d(TAG, "storeBoundUidToFirestore: " + targetUid);

        AtomicInteger successCounter = new AtomicInteger();

        // update "boundUsers" field to self database
        db.collection("users").document(selfUid)
                .update("boundUsers", FieldValue.arrayUnion(targetUid))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Success save selfUid to Firestore");
                        checkUpdateSuccess(successCounter.incrementAndGet(), targetUid);
                    } else {
                        Log.d(TAG, "Failed save selfUid to Firestore");
                    }

                });

        // update "boundUsers" field to target database
        db.collection("users").document(targetUid)
                .update("boundUser", selfUid)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Success save targetUid to Firestore");
                        checkUpdateSuccess(successCounter.incrementAndGet(), targetUid);
                    } else {
                        Log.d(TAG, "Failed save targetUid to Firestore");
                        Toast.makeText(getContext(), "Bound failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUpdateSuccess(int successCounter, String targetUid) {
        Log.d(TAG, "checkUpdateSuccess: " + successCounter);
        if (successCounter == 2) {
            Toast.makeText(getContext(), "Bound successfully", Toast.LENGTH_SHORT).show();

            // TODO: 更新本地資料
//            Set<String> data = sp.getStringSet("boundUsers", new HashSet<>());
//            data.add(targetUid);
//
//            editor.putStringSet("boundUsers", data);
//            editor.commit();

            // TODO: update UI
            EditText inputUid = rootView.findViewById(R.id.input_uid);
            inputUid.setText("");
            recyclerViewData.clear();
            updateRecyclerViewData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}