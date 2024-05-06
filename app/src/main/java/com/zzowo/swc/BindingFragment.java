package com.zzowo.swc;

import android.Manifest;
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
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BindingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BindingFragment extends Fragment {
    private static final String TAG = "BindingFragment";
    private SharedPreferences sp;
    private boolean primaryUser;
    private String userUid;

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

        //init
        initPreferences();
        initStatusBarColor();
        initOperationView(rootView);

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        // recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<String> data = new ArrayList<>();
        recyclerView.addItemDecoration(new ItemSpacingDecoration(getContext(), 10)); // 2dp 的間距

        // TODO: 添加更多項目...
        data.add("user1");
        data.add("user2");
        data.add("user3");
        data.add("user4");

        ItemAdapter adapter = new ItemAdapter(data);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    private void initPreferences() {
        sp = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);

        primaryUser = sp.getBoolean("primaryUser", true);
        userUid = sp.getString("userUid", "");
    }

    private void initOperationView(View rootView) {
        View operationViewPrimary = rootView.findViewById(R.id.op_prime_user);
        View operationViewSecondary = rootView.findViewById(R.id.op_secondary_user);

        if (primaryUser) {
            operationViewPrimary.setVisibility(View.VISIBLE);
            operationViewSecondary.setVisibility(View.INVISIBLE);

            // 處理操作區內容
            TextView primeUid = rootView.findViewById(R.id.prime_uid);
            TextView copyPrimeUid = rootView.findViewById(R.id.copy_prime_uid);

            primeUid.setText("UID: " + userUid.substring(0, 15) + "...");
            copyPrimeUid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 複製 UID
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("uid", userUid); // label為系統可見標籤, 非使用者可見標籤
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getContext(), R.string.uid_copied, Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            operationViewPrimary.setVisibility(View.INVISIBLE);
            operationViewSecondary.setVisibility(View.VISIBLE);

            // TODO: 處理操作區內容
        }
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}