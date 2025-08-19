package com.s23010301.taskping.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.s23010301.taskping.R;

import java.util.Arrays;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int DEFAULT_ZOOM_LEVEL = 15;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private GoogleMap mMap;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_picker_activity);

        // Initialize default location
        Bundle extras = getIntent().getExtras();
        double lat = extras != null ? extras.getDouble("lat", 7.2906) : 7.2906;
        double lng = extras != null ? extras.getDouble("lng", 80.6337) : 80.6337;
        selectedLatLng = new LatLng(lat, lng);


        initPlacesAPI();
        setupAutocomplete();
        setupMapFragment();
        setupLocationButton();
    }

    private void initPlacesAPI() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
    }

    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment == null) {
            Toast.makeText(this, "Search functionality unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.FORMATTED_ADDRESS
        ));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                handlePlaceSelection(place);
            }

            @Override
            public void onError(@NonNull Status status) {
                handlePlacesError(status);
            }
        });
    }

    private void handlePlaceSelection(Place place) {
        LatLng latLng = place.getLocation();
        String name = place.getDisplayName();
        String address = place.getFormattedAddress();
        if (latLng != null && mMap != null) {
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(name)
                    .snippet(address));
            selectedLatLng = latLng;
        }
    }

    private void handlePlacesError(Status status) {
        String errorMsg = "Location search failed: " + status.getStatusMessage();
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map loading failed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupLocationButton() {
        findViewById(R.id.btnPickLocation).setOnClickListener(v -> returnSelectedLocation());
    }

    private void returnSelectedLocation() {
        Intent result = new Intent();
        result.putExtra("lat", selectedLatLng.latitude);
        result.putExtra("lng", selectedLatLng.longitude);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, DEFAULT_ZOOM_LEVEL));
        mMap.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("Selected Location"));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected"));
        });

        enableMyLocationIfPermissionGranted();
    }

    private void enableMyLocationIfPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfPermissionGranted();
        }
    }
}