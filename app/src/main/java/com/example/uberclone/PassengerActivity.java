package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PassengerActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Button btnRequestCar;

    private Button btnBeep;
    //true by default when an app opens coz still seraching for location
    private Boolean isUberCancelled = true;

    private Boolean isCarReady = false;

    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        btnRequestCar = findViewById(R.id.btnRequestCar);
        btnRequestCar.setOnClickListener(PassengerActivity.this);

        btnBeep = findViewById(R.id.btnBeepBeep);
        //letting passenger know the driver wants to give u a ride after pressing the button
        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getDriverUpdates();

            }
        });

        //cancelling our request after opening app again because we already requested it before then closed the app.
        // so after opening the app again it should show cancel your request
        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (objects.size() > 0 && e == null){
                    //Uber is not cancelled yet
                    isUberCancelled = false;
                    btnRequestCar.setText("Cancel your uber request");

                    getDriverUpdates();

                }

            }
        });

        findViewById(R.id.btnLogOutFromPassengerActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {

                        if (e == null){

                            finish();

                        }

                    }
                });


            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

//                //this method will be called when the location of themarker is changed
//                //location is based on longitude and latitude
//                //2 args: lat-latitude and lng-longitude
//                LatLng passengerLocation = new LatLng(location.getLatitude(), location.getLongitude());
//                //when changing position ccalling clear to move to new location
//                mMap.clear();
//                //need camera to show location
//                //update our app and show user the location
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
//                //adding marker
//                mMap.addMarker(new MarkerOptions().position(passengerLocation).title("You are here!"));


                //the location as we get from the location changes method "location" from location listener
                updateCameraPassengerLocation(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        if (Build.VERSION.SDK_INT < 23) {

            //if OS < 23 then it will ask for location update
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        } else if (Build.VERSION.SDK_INT > 23){
            //asking user to give us the permission. requesting to access location
            //when the user havent given permission so we asking here for it

            if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

                ActivityCompat.requestPermissions(PassengerActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);


            }

        }
        //when user has already given us the permission
        else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //updating location and calling camera in current location
            updateCameraPassengerLocation(currentPassengerLocation);


        }
    }

    //depending on the permission the user will give us
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                //when its asking location updates
                updateCameraPassengerLocation(currentPassengerLocation);
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void updateCameraPassengerLocation(Location pLocation){

//this method will be called when the location of themarker is changed
        //i car not ready show location of passenger only
        if (isCarReady == false) {


            //location is based on longitude and latitude
            //2 args: lat-latitude and lng-longitude
            LatLng passengerLocation = new LatLng(pLocation.getLatitude(), pLocation.getLongitude());
            //when changing position ccalling clear to move to new location
            mMap.clear();
            //need camera to show location
            //update our app and show user the location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLocation, 15));
            //adding marker
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("You are here!"));
        }
    }

    @Override
    public void onClick(View view) {

        if (isUberCancelled) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location passengerCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (passengerCurrentLocation != null) {

                //request for a car
                //dealing with parse server so,
                ParseObject requestCar = new ParseObject("RequestCar");
                requestCar.put("username", ParseUser.getCurrentUser().getUsername());

                //where paassenger is waiting in the location
                ParseGeoPoint userLocation = new ParseGeoPoint(passengerCurrentLocation.getLatitude(), passengerCurrentLocation.getLongitude());
                //now send object to server
                requestCar.put("passengerLocation", userLocation);

                requestCar.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {

                        if (e == null) {

                            Toast.makeText(PassengerActivity.this, "A car request is sent", Toast.LENGTH_SHORT).show();
                            btnRequestCar.setText("Cancel your Uber order");
                            //we have a request so it show should to cancel the request
                            isUberCancelled = false;
                        }

                    }
                });

            } else {

                Toast.makeText(this, "Unknown Error. Something went wrong", Toast.LENGTH_SHORT).show();

            }
        }
        }
        //when isberCancelled is false it will delete the order from the server
        else {

            ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
            carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requestList, ParseException e) {

                    if (requestList.size() > 0 && e == null){

                        //bcoz user doesnt want any car and want to cancel the uber
                        isUberCancelled = true;
                        btnRequestCar.setText("Request a new uber");

                        for (ParseObject uberRequest : requestList){

                            uberRequest.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {

                                    if (e == null){

                                        Toast.makeText(PassengerActivity.this, "Request deleted", Toast.LENGTH_LONG).show();

                                    }

                                }
                            });

                        }

                    }

                }
            });

        }

    }
    //updates from driver
    private void getDriverUpdates(){

        //global variable counter
     //   int counter = 1;

        //the code put in inside the timer because when request is cancelled it will show only the
        //passengers location after the timer time and we wont have to press anything eg:
        //beep beep button to show passenger location after cancelling request.

         t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                ParseQuery<ParseObject> uberRequestQuery = ParseQuery.getQuery("RequestCar");
                //car request we get must be equal to the current user
                uberRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                uberRequestQuery.whereEqualTo("requestAccepted", true);
                uberRequestQuery.whereExists("driverOfMe");

                uberRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {

                        if (objects.size() > 0 && e == null){

                            for (final ParseObject requestObject : objects) {

                                //location from driver to passenger
                                ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                                driverQuery.whereEqualTo("username", requestObject.getString("driverOfMe"));
                                driverQuery.findInBackground(new FindCallback<ParseUser>() {
                                    @Override
                                    public void done(List<ParseUser> drivers, ParseException e) {

                                        if (drivers.size() > 0 && e == null){

                                            //when ride is requested
                                            isCarReady = true;


                                            for (ParseUser driverOfRequest : drivers){
                                                //now we have driver of request
                                                //passing location of driver
                                                ParseGeoPoint driverOfRequestLocation = driverOfRequest.getParseGeoPoint("driverLocation");

                                                if (ContextCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                    //current location ofpassenger
                                                    Location passengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                                    //converting location to parse geo point
                                                    ParseGeoPoint pLocationAsParseGeoPoint = new ParseGeoPoint(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                                    //calculating distance in miles
                                                    double milesDistance = driverOfRequestLocation.distanceInMilesTo(pLocationAsParseGeoPoint);

                                                    if (milesDistance < 0.3){


                                                        requestObject.deleteInBackground(new DeleteCallback() {
                                                            @Override
                                                            public void done(ParseException e) {

                                                                if (e == null){

                                                                    Toast.makeText(PassengerActivity.this,"Your Uber has arrived", Toast.LENGTH_LONG).show();
                                                                    //car has reached so false
                                                                    isCarReady = false;
                                                                    //if u wanna call another order
                                                                    isUberCancelled = true;
                                                                    btnRequestCar.setText("You can order a new uber now!");

                                                                }

                                                            }
                                                        });

                                                    }else {

                                                        //rounded distance
                                                        float roundedDistance = Math.round(milesDistance * 10) / 10;
                                                        Toast.makeText(PassengerActivity.this, requestObject.getString("driverOfMe") + " is " +
                                                                roundedDistance + " miles from you. Please wait!", Toast.LENGTH_LONG).show();


                                                        // location of driver
                                                        LatLng dLocation = new LatLng(driverOfRequestLocation.getLatitude(),
                                                                driverOfRequestLocation.getLongitude());

//        //location of the passenger
                                                        LatLng pLocation = new LatLng(pLocationAsParseGeoPoint.getLatitude(),
                                                                pLocationAsParseGeoPoint.getLongitude());


                                                        //driver and passenger both locations
                                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                        Marker driverMarker = mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Location"));
                                                        Marker passengerMarker = mMap.addMarker(new MarkerOptions().position(pLocation).title("Passenger Location"));

                                                        //we need to have Markers inside array list
                                                        ArrayList<Marker> myMarkers = new ArrayList<>();
                                                        myMarkers.add(driverMarker);
                                                        myMarkers.add(passengerMarker);

                                                        //for loop in order t iterate inside an array list
                                                        for (Marker marker : myMarkers) {

                                                            //we can have markers of array list coz we include builder on the map
                                                            builder.include(marker.getPosition());

                                                        }

                                                        LatLngBounds bounds = builder.build();

                                                        //offset shows value fromthe edges of the map
                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                                                        mMap.animateCamera(cameraUpdate);
                                                    }

                                                }


                                            }

                                        }


                                    }
                                });


                                // Toast.makeText(PassengerActivity.this, object.get("driverOfMe") + " wants to give a ride", Toast.LENGTH_LONG).show();

                            }
                        } else {


                            isCarReady = false;

                        }

                    }
                });

            }

        }, 0, 3000);



    }


}
