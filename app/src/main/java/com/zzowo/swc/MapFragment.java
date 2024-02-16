package com.zzowo.swc;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment";
    ProgressBar progressBar;
//    Boolean isLocationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationProviderClient;
    SupportMapFragment supportMapFragment;
    private GoogleMap map;
    private Location lastKnownLocation;
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(24.998639, 121.5547245);
    private static final float DEFAULT_ZOOM = 19F;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        progressBar = rootView.findViewById(R.id.progressBar);

        // init
        initStatusBarColor();

        // TODO: 判斷是否為主使用者

        // 建立 SupportMapFragment 物件
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // 建立 FusedLocationProviderClient 物件
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                map = googleMap;
                Log.d(TAG, "onMapReady: " + googleMap.toString());

                // Turn on the My Location layer and the related control on the map.
                updateLocationUI();

                // Get the current location of the device and set the position of the map.
                getDeviceLocation();

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        return rootView;
    }

    private void initStatusBarColor() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.colorSurface));
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI(){
        Log.d(TAG, "update location UI");
        if (map == null) {
            Log.d(TAG, "Map is empty");
            return;
        }
        try {
            Log.d(TAG, "Showing my location UI on map...");
            // 在地圖上啟用「我的位置」圖層和相關控制項
            map.setMyLocationEnabled(true); // 顯示藍色的圓形圖標，表示使用者的位置
            map.getUiSettings().setMyLocationButtonEnabled(true); // 顯示「我的位置」按鈕

            map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(@NonNull Location location) {
                    Log.d(TAG, "Location changed: " + location.toString());
                    // 當位置變更時，自動將Camera移動到使用者的位置
                    LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, DEFAULT_ZOOM));
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        progressBar.setVisibility(View.VISIBLE);

        MainActivity mainActivity = (MainActivity) getActivity();

        if (mainActivity != null) {
            Double lastLatitude = mainActivity.getLastLatitude();
            Double lastLongitude = mainActivity.getLastLongitude();

            if (lastLatitude != null && lastLongitude != null) {
                Log.d(TAG, "Set Camera to Last latitude: " + lastLatitude + ", Last longitude: " + lastLongitude);

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lastLatitude, lastLongitude),
                        DEFAULT_ZOOM));
                progressBar.setVisibility(View.GONE);
            } else {
                Log.d(TAG, "Current location is null. Using defaults.");
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                map.getUiSettings().setMyLocationButtonEnabled(false);
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}