package com.s23010301.taskping;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng = new LatLng(7.2906, 80.6337); // default Kandy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_picker_activity);

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Setup autocomplete search
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                if (latLng != null && mMap != null) {
                    mMap.clear();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                    selectedLatLng = latLng;
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MapPickerActivity.this, "Search error: " + status, Toast.LENGTH_SHORT).show();
            }
        });

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.btnPickLocation).setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("lat", selectedLatLng.latitude);
            result.putExtra("lng", selectedLatLng.longitude);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
        mMap.addMarker(new MarkerOptions().position(selectedLatLng).title("Selected"));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected"));
        });
    }
}

