package com.example.ch8_mycontactlist;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
/* imports from first part of chapter 7
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;*/

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
//import android.support.design.widget.Snackbar;
//import android.support.v4.app.ActivityCompat;
import android.widget.EditText;

import android.support.v4.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContactMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    //private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    //private static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    final int PERMISSION_REQUEST_LOCATION = 101;
    GoogleMap gMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    //change last above word to "fusedLocationClient" ?
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    ArrayList<Contact> contacts = new ArrayList<>();
    Contact currentContact = null;
    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView textDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_map);

        //sensor manager = system service so must reference not instantiate
        //          gets references to 2 sensors used to measure heading
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //accelerometer = reports device acceleration in 3 dimensions
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //magnetometer = reports geomagnetic field in 3 dimensions
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //not all devices have sensors
        //          if it does, sensorManager associates each sensor with same event listener & passes parameter
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(mySensorEventListener, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(mySensorEventListener, magnetometer,
                    SensorManager. SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Sensors not found",Toast.LENGTH_LONG).show();
        }
        textDirection = findViewById(R.id.textHeading);

        Bundle extras = getIntent().getExtras();
        try {
            ContactDataSource ds = new ContactDataSource(ContactMapActivity.this);
            ds.open();
            if(extras !=null){
                currentContact = ds.getSpecificContact(extras.getInt("contactid"));
            }
            else {
                contacts = ds.getContacts("contactname", "ASC");
            }
            ds.close();
        }
        catch (Exception e) {
            Toast.makeText(this, "Contact(s) could not be retrieved.", Toast.LENGTH_LONG).show();

        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        createLocationRequest();
        createLocationCallback();

        initListButton();
        initSettingsButton();
        initMapTypeButtons();
    }

    private void initListButton() {
        ImageButton ibList = findViewById(R.id.imageButtonList);
        ibList.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(ContactMapActivity.this, ContactListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initSettingsButton() {
        ImageButton ibList = findViewById(R.id.imageButtonSettings);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ContactMapActivity.this, ContactSettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Toast.makeText(getBaseContext(), "Lat: " + location.getLatitude() +
                                    " Long: " + location.getLongitude() +
                                    " Accuracy:  " + location.getAccuracy(),
                            Toast.LENGTH_LONG).show();
                }
            };
        };
    }

    private void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null );
        gMap.setMyLocationEnabled(true);
    }

    private void stopLocationUpdates() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void initMapTypeButtons() {
        RadioGroup rgMapType = findViewById(R.id.radioGroupMapType);
        rgMapType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                RadioButton rbNormal = findViewById(R.id.radioButtonNormal);
                if (rbNormal.isChecked()) {
                    gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                else  {
                    gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        RadioButton rbNormal = findViewById(R.id.radioButtonNormal);
        rbNormal.setChecked(true);

        Point size = new Point();
        WindowManager w = getWindowManager();
        w.getDefaultDisplay().getSize(size);
        int measuredWidth = size.x;
        int measuredHeight = size.y;

        if (contacts.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i = 0; i < contacts.size(); i++) {
                currentContact = contacts.get(i);

                Geocoder geo = new Geocoder(this);
                List<Address> addresses = null;

                String address = currentContact.getStreetAddress() + ", " +
                        currentContact.getCity() + ", " +
                        currentContact.getState() + " " +
                        currentContact.getZipCode();

                try {
                    addresses = geo.getFromLocationName(address, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LatLng point = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                builder.include(point);

                gMap.addMarker(new MarkerOptions().position(point).
                        title(currentContact.getContactName()).snippet(address));
            }
            gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),
                    measuredWidth, measuredHeight, 450));
        } else {
            if (currentContact != null) {
                Geocoder geo = new Geocoder(this);
                List<Address> addresses = null;

                String address = currentContact.getStreetAddress() + ", " +
                        currentContact.getCity() + ", " +
                        currentContact.getState() + " " +
                        currentContact.getZipCode();

                try {
                    addresses = geo.getFromLocationName(address, 1);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                LatLng point = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());

                gMap.addMarker(new MarkerOptions().position(point).
                        title(currentContact.getContactName()).snippet(address));
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16));
            }
            else {
                AlertDialog alertDialog = new AlertDialog.Builder(ContactMapActivity.this).create();
                alertDialog.setTitle("No Data");
                alertDialog.setMessage("No data is available for the mapping function.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                alertDialog.show();
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(ContactMapActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (ContactMapActivity.this,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                        Snackbar.make(findViewById(R.id.activity_contact_map),
                                        "MyContactList requires this permission to locate " +
                                                "your contacts",
                                        Snackbar.LENGTH_INDEFINITE).setAction("OK",
                                        new View.OnClickListener() {
                               @Override
                               public void onClick(View v) {
                                   ActivityCompat.requestPermissions(ContactMapActivity.this,
                                           new String[]{
                                                   android.Manifest.permission.ACCESS_FINE_LOCATION
                                           }, PERMISSION_REQUEST_LOCATION);
                               }
                        })
                                .show();
                    } else {
                        ActivityCompat.requestPermissions(ContactMapActivity.this, new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        }, PERMISSION_REQUEST_LOCATION);
                    }
                } else {
                    startLocationUpdates();
                }
            } else {
                startLocationUpdates();
            }
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Error requesting permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    //SensorEventListener = class handles actual events from sensors & takes action on them
    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        //requires implementation of two methods onAccuracyChanged & onSensorChanged, but we don't need accuracy
        public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

        float[] accelerometerValues;
        float[] magneticValues;

        //determines which sensor triggered event and then captures values it provided
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticValues = event.values;
            //if values available for both sensors, SensorManager asked for 2 rotational matrices
            //          for orientation calculation
            if (accelerometerValues!= null && magneticValues!= null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticValues);
                //if matrices successfully calculated, SensorManager asked to calculate device orientation
                //device orientation measured in 3 dimensions
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    //value used to calculate heading; reported in radians, so changed to degrees
                    float azimut = (float) Math.toDegrees(orientation[0]);
                    //convert heading reported to eliminate negative numbers
                    if (azimut < 0.0f) { azimut+=360.0f;}
                    //use degree heading to get text description; done in 90-degree increments
                    String direction;
                    if (azimut >= 315 || azimut < 45) { direction = "N"; }
                    else if (azimut >= 225 && azimut < 315) { direction = "W"; }
                    else if (azimut >= 135 && azimut < 225) { direction = "S"; }
                    else { direction = "E"; }
                    textDirection.setText(direction);
                }
            }
        }
    };
}