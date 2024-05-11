package com.zzowo.swc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BindingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BindingFragment extends Fragment {
    private static final String TAG = "BindingFragment";
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private FirebaseFirestore db;
    private boolean primaryUser;
    private String selfUid;

    private RecyclerView mRecyclerView;
    private List<String> mData;
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
        View rootView = inflater.inflate(R.layout.fragment_binding, container, false);


        db = FirebaseFirestore.getInstance();

        //init
        initPreferences();
        initStatusBarColor();
        initOperationView(rootView);

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        // recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<String> data = new ArrayList<>();
        recyclerView.addItemDecoration(new ItemSpacingDecoration(getContext(), 10)); // 2dp 的間距
        data.add("user1");
        data.add("user2");
        data.add("user3");
        data.add("user4");
        // 添加更多項目...

        ItemAdapter adapter = new ItemAdapter(data);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    private void initPreferences() {
        sp = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = sp.edit();

        primaryUser = sp.getBoolean("primaryUser", true);
        selfUid = sp.getString("userUid", "");
    }

    private void initOperationView(View rootView) {
        View operationViewPrimary = rootView.findViewById(R.id.op_prime_user);
        View operationViewSecondary = rootView.findViewById(R.id.op_secondary_user);

        if (primaryUser) {
            // primaryUser
            operationViewPrimary.setVisibility(View.VISIBLE);
            operationViewSecondary.setVisibility(View.INVISIBLE);

            // TODO: 處理操作區內容
            EditText inputUid = rootView.findViewById(R.id.input_uid);
            TextView selectBtn = rootView.findViewById(R.id.select_button);

            inputUid.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        selectBtn.setText("Confirm");
                    } else {
                        inputUid.setHint(R.string.input_uid);
                    }
                }
            });

            selectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Confirm button clicked!");
                    // 確認按鈕
                    String input = inputUid.getText().toString();
                    // 檢查 UID 是否合法
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
                                    if (isUidUserPrimary) {
                                        Toast.makeText(getContext(), "Primary user cannot be bound", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Primary user cannot be bound!");
                                        return;
                                    }

                                    Log.d(TAG, "Input UID found!" + task.getResult().getData());

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
                    Toast.makeText(getContext(), R.string.uid_copied, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
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
            // 以targetUid取得使用者email
            db.collection("users").document(targetUid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String targetEmail = task.getResult().getString("email");
                            editor.putString("boundUserEmail", targetEmail); // TODO: save array list
                            editor.apply();
                        }
                    });

            // TODO: update UI
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}