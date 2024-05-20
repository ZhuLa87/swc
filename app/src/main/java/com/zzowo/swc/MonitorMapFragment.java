package com.zzowo.swc;

import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;
import static com.zzowo.swc.MainActivity.binding;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MonitorMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MonitorMapFragment extends Fragment {
    private static final String TAG = "MonitorMapFragment";
    private ProgressBar progressBar;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private SupportMapFragment supportMapFragment;
    private static final float DEFAULT_ZOOM = 19F;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MonitorMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MonitorMapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MonitorMapFragment newInstance(String param1, String param2) {
        MonitorMapFragment fragment = new MonitorMapFragment();
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
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_monitor_map, container, false);

        progressBar = rootView.findViewById(R.id.progressBar);
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        // init
        initStatusBarColor();
        initFirebase();


        // TODO: 添加使用者綁定的判斷
        getBoundUser();


        return rootView;
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
    }

    private void initFirebase() {
        Log.d(TAG, "initFirebase");
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
    }

    private void getBoundUser() {
        Log.d(TAG, "getting bound user");
        // 取得綁定的使用者
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                            // 取得綁定的使用者
                        String boundUser = task.getResult().getString("boundUser");
                        if (boundUser != null) {
                            // 找到綁定的使用者
                            Log.d(TAG, boundUser);
                            getBoundUserLocation(boundUser);
                            return;
                        }
                    }
                    showNotBoundDialog();
                });
    }

    private void showNotBoundDialog() {
        if (getContext() == null) return;

        // 顯示未綁定的 Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.unboundUserTitle);
        builder.setMessage(R.string.unBoundUserMsg);

        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            // 關閉畫面
            binding.bottomNavigationView.setSelectedItemId(R.id.bottom_binding);
        });

        builder.setCancelable(false);

        builder.show();
    }

    private void getBoundUserLocation(String boundUser) {
        DocumentReference docRef = db.collection("users").document(boundUser);

        // 取得綁定使用者的位置
        docRef.get()
        .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 取得綁定使用者的位置
                Map<String, Object> lastLocation = (Map<String, Object>) task.getResult().get("lastLocation");

                if (lastLocation != null) {
                    GeoPoint geoPoint = (GeoPoint) lastLocation.get("point");

                    // 時間戳
                    Timestamp timestamp = (Timestamp) lastLocation.get("timestamp");

                    showBoundUserLocation(geoPoint, timestamp);
                }
            }
        });

        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                if (value != null && value.exists()) {
                    progressBar.setVisibility(View.VISIBLE);
                    Map<String, Object> lastLocation = (Map<String, Object>) value.get("lastLocation");

                    if (lastLocation == null) return;

                    GeoPoint geoPoint = (GeoPoint) lastLocation.get("point");

                    // 時間戳
                    Timestamp timestamp = (Timestamp) lastLocation.get("timestamp");

                    Log.d(TAG, "Firestore data update" + geoPoint.getLongitude() + ", " + geoPoint.getLatitude());

                    showBoundUserLocation(geoPoint, timestamp);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    private void showBoundUserLocation(GeoPoint geoPoint, Timestamp timestamp) {

        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
        String time = sdf.format(date);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()))
                                .title(time));
                googleMap.animateCamera(newLatLngZoom(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()), DEFAULT_ZOOM));
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}