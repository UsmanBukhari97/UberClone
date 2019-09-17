package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class DriverRequestListActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Button btnGetRequest;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListView listView;
    private ArrayList<String> nearbyDriveRequests;

    private ArrayAdapter adapter;

    //as we want location of the driver and passenger so we pass values of latitude and longitude
    private ArrayList<Double> passengersLatitudes;
    private ArrayList<Double> passengersLongitudes;

    private ArrayList<String> requestCarUsernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request_list);


         btnGetRequest = findViewById(R.id.btnGetRequest);
         btnGetRequest.setOnClickListener(this);

        listView = findViewById(R.id.requestListView);

        nearbyDriveRequests = new ArrayList<>();

        passengersLatitudes = new ArrayList<>();
        passengersLongitudes = new ArrayList<>();

        requestCarUsernames = new ArrayList<>();

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nearbyDriveRequests);

        listView.setAdapter(adapter);
        //to get requests only once
        nearbyDriveRequests.clear();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //runtime permission for accessing the location
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){


//            try {
//                //this happens when user have already given us the permission
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//            } catch (Exception e){
//
//                e.printStackTrace();
//
//            }
//            locationListener = new LocationListener() {
//                @Override
//                public void onLocationChanged(Location location) {
//
//                    //now we can ask for location updates. fixing the bug
//                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//
//                }
//
//                @Override
//                public void onStatusChanged(String s, int i, Bundle bundle) {
//
//                }
//
//                @Override
//                public void onProviderEnabled(String s) {
//
//                }
//
//                @Override
//                public void onProviderDisabled(String s) {
//
//                }
//            };

            //created amethod coz it was returning null causing application to crash
            initializeLocationListener();

        }

        listView.setOnItemClickListener(this);




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.driver_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.driverLogOutItem){

            ParseUser.logOutInBackground(new LogOutCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        finish();
                    }
                }
            });

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        //permission codes
        //for getting location of the user


//
//        locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(Location location) {
//
//                updateRequestListView(location);
//
//            }
//
//            @Override
//            public void onStatusChanged(String s, int i, Bundle bundle) {
//
//            }
//
//            @Override
//            public void onProviderEnabled(String s) {
//
//            }
//
//            @Override
//            public void onProviderDisabled(String s) {
//
//            }
//        };


        if (Build.VERSION.SDK_INT < 23) {

            //if OS < 23 then it will ask for location update
           // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //updating location and calling camera in current location
            updateRequestListView(currentDriverLocation);
        } else if (Build.VERSION.SDK_INT >= 23) {
            //asking user to give us the permission. requesting to access location
            //when the user havent given permission so we asking here for it

            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(DriverRequestListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);


            }


            //when user has already given us the permission
            else {

                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //updating location and calling camera in current location
                updateRequestListView(currentDriverLocation);


            }
        }

    }
    //we are passing driver location so it we pass thismethod and it shouldnt be null

    private void updateRequestListView(Location driverLocation) {

        if (driverLocation != null) {

            saveDriverLocationToParse(driverLocation);
            //request will be coming non stop so making it a memory friendly we will create an instance variabble of list view
            //


           // ListView listView = findViewById(R.id.requestListView);
            //instantiated so
           //listView = findViewById(R.id.requestListView);

//            ArrayList<String> nearbyDriveRequests = new ArrayList<>();
//            ArrayAdapter adapter = new ArrayAdapter(this,
//                    android.R.layout.simple_expandable_list_item_1, nearbyDriveRequests);

  //          listView.setAdapter(adapter);


            //accessing location of the driver

            final ParseGeoPoint driverCurrentLocation = new ParseGeoPoint(driverLocation.getLatitude(), driverLocation.getLongitude());

            ParseQuery<ParseObject> requestCarQuery = ParseQuery.getQuery("RequestCar");

            requestCarQuery.whereNear("passengerLocation", driverCurrentLocation);

            //near to driver and a;ready gave a ride
            requestCarQuery.whereDoesNotExist("driverOfMe");
            requestCarQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {


                        if (objects.size() > 0) {
                          //  nearbyDriveRequests.clear();
                            if (nearbyDriveRequests.size() > 0){

                                nearbyDriveRequests.clear();

                            }
                            if (passengersLatitudes.size() > 0){

                                passengersLatitudes.clear();

                            }
                            if (passengersLongitudes.size() > 0){

                                passengersLongitudes.clear();

                            }
                            if (requestCarUsernames.size() > 0){

                                requestCarUsernames.clear();
                            }



                            for (ParseObject nearRequest : objects) {

                                //creating a variable o Parse Geo Point
                                ParseGeoPoint pLocation = (ParseGeoPoint) nearRequest.get("passengerLocation");
                                //we want to have access of the distance from current driver to passenger
                               // Double milesDistanceToPassenger = driverCurrentLocation.distanceInMilesTo((ParseGeoPoint) nearRequest.get("passengerLocation"));
                                //First^ now:
                                Double milesDistanceToPassenger = driverCurrentLocation.distanceInMilesTo(pLocation);

                                //result 58.63352627
                                //after round 59
                                //  58.2467970
                                //58
                                //58/10 = 5.8 so this is user friendly
                                float roundedDistanceValue = Math.round(milesDistanceToPassenger * 10) / 10;

                                nearbyDriveRequests.add("There are " + roundedDistanceValue + " miles to " + nearRequest.get("username"));

                                //these two array list will contain all lat and lon of passengers
                                passengersLatitudes.add(pLocation.getLatitude());
                                passengersLongitudes.add(pLocation.getLongitude());

                                //referring
                                requestCarUsernames.add(nearRequest.get("username") + "");

                            }
                            //to update list accordingly
                           // adapter.notifyDataSetChanged();

                        } else {
                            Toast.makeText(DriverRequestListActivity.this, "No requests yet", Toast.LENGTH_LONG).show();
                        }
                        //to update list accordingly
                        adapter.notifyDataSetChanged();
                    }


                }
            });

        }



    }


    //depending on the permission the user will give us
    //deals with permission dialogue
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                initializeLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                //already did this in on location changed so we only need location updates^
                //now using this again because we initialized location listener in class and deleted one in method
                Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//
//                //when its asking location updates
               updateRequestListView(currentDriverLocation);
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //implementing Adapter on item click listener for list view
    // so that if we click on a list item it takes us to next activity
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

      //  Toast.makeText(DriverRequestListActivity.this, "Clicked", Toast.LENGTH_LONG).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location cdLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


            if (cdLocation != null) {
                //passing  lat and lon to another activity
                Intent intent = new Intent(this, ViewLocationsMapActivity.class);
                intent.putExtra("dLatitude", cdLocation.getLatitude());
                intent.putExtra("dLongitude", cdLocation.getLongitude());
                intent.putExtra("pLatitude", passengersLatitudes.get(position));
                intent.putExtra("pLongitude", passengersLongitudes.get(position));

                intent.putExtra("rUsername", requestCarUsernames.get(position));
                startActivity(intent);
            }


        }



    }

    private void initializeLocationListener(){

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //now we can ask for location updates. fixing the bug
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

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

    }

    private void saveDriverLocationToParse(Location location){

        ParseUser driver = ParseUser.getCurrentUser();
        ParseGeoPoint driverLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        driver.put("driverLocation", driverLocation);
        driver.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {

                if (e == null){

                    Toast.makeText(DriverRequestListActivity.this, "Location Saved", Toast.LENGTH_SHORT).show();


                }

            }
        });

    }
}
