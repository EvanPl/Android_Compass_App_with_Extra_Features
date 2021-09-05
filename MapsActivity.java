package com.example.vaggelis.assignment_1;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//Accessed by swiping to the right from the 1st activity. They user is prompted to allow access
//to location services (for android version of  Marshmallow and higher) and wifi is turned ON (for the map to be refreshed).
// If permission is granted, the latitude and longitude coordinates of the phone (if  location is ON) are shown in text form as well as a point on Google Maps,
//otherwise an appropriate message is displayed. The speed of the phone in km/h and knots (for use in marine) are also displayed.
//The user can return to the main activity by pressing the back button image on the top right corner.
//If permission is not granted (for android version of  Marshmallow and higher) we return to the MainActivity
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Double myLat,myLat2; //store the Latitude (in Degrees Format)
    private Double myLong,myLong2; //store the Longitude (in Degrees Format)
    private float Myspeed; //stores the speed in m/s over ground
    private float Myspeedkhm; //stores the speed in km/h over ground
    private float Myspeedknots; //stores the speed in knots (for ships)
    int lat_deg; //holds latitude degrees
    int lat_min; //holds latitude minutes
    double lat_sec; //holds latitude seconds
    int longi_deg; //holds longitude degrees
    int longi_min; //holds longitude minutes
    double longi_sec; //holds longitude seconds
    private String lat; //is N or S (if we are either on the up or down Hemisphere)
    private String logit; //holds W or E (if we either are on the left or right Hemisphere)
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION=101; //Code for ACCESS_FINE_LOCATION
    private boolean permissionIsGranted=false; //True if location permission from the user is granted
    SupportMapFragment mapFragment;
    TextView latitude,longitude,speedkmh,speedknots; //TextViews used for displaying the longitude and latitude coordinates

    WifiManager wifi; // Note, that it is important to make sure that the Wifi is on in order for the map image
                      //to be loaded. Thus, we create an object (instance) of class WifiManager.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Match the TextView variables with the appropriate TextView IDs
        latitude=(TextView) findViewById(R.id.LatTextView);
        longitude=(TextView) findViewById(R.id.LongTextView);
        speedkmh=(TextView) findViewById(R.id.speedkmhTextView);
        speedknots=(TextView) findViewById(R.id.speedknotsTextView);

        //Initialise the WifiManager instance
        wifi=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Now, we check the WIFi status on the mobile phone and if the wifi is OFF we turn it ON because it is needed for the map image to be loaded
        if (wifi.getWifiState()==wifi.WIFI_STATE_DISABLED){
            wifi.setWifiEnabled(true);
            Toast.makeText(getApplicationContext(),"Wifi Enabled", Toast.LENGTH_SHORT).show();

        }

        // Crucial to make the location google play services working (this is a builder for GoogleApiClient)
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest(); //used for obtaining the location
        locationRequest.setInterval(15 * 1000);  //Set the desired interval for active location updates. We will look every minute in the location provider
        locationRequest.setFastestInterval(10 * 1000);  //This controls the fastest rate at which the application will receive location updates, which might be faster than setInterval
        // in some situations (for example, if other applications are triggering location updates).
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //For the location we aim for accuracy (more likely to use GPS)
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

        // Add a marker in Sydney and move the camera
        mMap.clear();
        LatLng myLoc = new LatLng(myLat, myLong);
        mMap.addMarker(new MarkerOptions().position(myLoc).title("You are here!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc)); //move map's camera to that location;
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(18);
        mMap.animateCamera(zoom);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect(); //connect to Google API Client

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Here we request Location Updates if and only if location permission has been granted by the user
        if (permissionIsGranted) {
            if (googleApiClient.isConnected()) {
                requestLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove Location Updates if we were using them in the first place (because if the user does not grant access to location services we will not be requesting Location Updates)
        if (permissionIsGranted) LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect from the Google API Client if the user has given access to location services in the first place
        if (permissionIsGranted) googleApiClient.disconnect();
    }


    // When we have been connected to Google API Client
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // The code below is executed if the user does not grand location permissions. But in our case we assume that the user is always providing permission to location (and if not then the app will not work)
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //for versions of Marshmallow and higher
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_FINE_LOCATION); //Request from the user to enable location permissions
            }
            else {
                permissionIsGranted=true; //This means that the user enabled location permissions (or that the phone is operating on a lower version of Marshmallow)
            }
            return;
        }
        else permissionIsGranted=true; //Permissions have been granted
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    //PUT LOG MESSAGES HERE
    @Override
    public void onConnectionSuspended(int i) { }
    //PUT LOG MESSAGES HERE
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    //When the location has changed (within the specified time intervals) we update display the updated location (latitude and longitude) as well as the altitude
    @Override
    public void onLocationChanged(Location location) {
        myLat = location.getLatitude(); //get the latitude
        myLong = location.getLongitude(); //get the longitude
        Myspeed = location.getSpeed(); //get the speed (m/s)over the ground
        mapFragment.getMapAsync(this);
        myLong2=myLong;
        myLat2=myLat;
        lat_deg = myLat2.intValue();
        if (lat_deg > 0) lat = "N"; //We are on top Hemispherem
        else if (lat_deg < 0) lat = "S"; //We are on the down Hemisphere
        myLat2 = Math.abs(myLat2);
        myLat2 *= 60;
        myLat2 -= (Math.abs(lat_deg) * 60);
        lat_min = myLat2.intValue();
        myLat2 *= 60;
        myLat2 -= (Math.abs(lat_min) * 60);
        lat_sec = myLat2;
        latitude.setText(lat_deg + "°" + lat_min + "'" + String.format("%.2f", lat_sec) + "''" + lat);
        //Now for the longitude (to split degrees in degrees, minutes and seconds)
        longi_deg = myLong2.intValue();
        if (longi_deg > 0) logit = "E"; //We are on right Hemisphere
        else if (longi_deg < 0) logit = "W"; //We are on the left Hemisphere
        myLong2 = Math.abs(myLong2);
        myLong2 *= 60;
        myLong2 -= (Math.abs(longi_deg) * 60);
        longi_min = myLong2.intValue();
        myLong2 *= 60;
        myLong2 -= (Math.abs(longi_min) * 60);
        longi_sec = myLong2;
        longitude.setText(longi_deg + "°" + longi_min + "'" + String.format("%.2f", longi_sec) + "''" + logit);
        speedkmh.setText(String.valueOf(Myspeed*3.6)+" km/h"); //display speed in km/h
        speedknots.setText(String.valueOf(Myspeed*1.94384449)+" knots"); //display speed in knots (for ships)
    }

    //Here we handle the location permission request and we make sure to inform the user in case he/she does
    //not grant access to location services that the longitude and latitude cannot be displayed and thus we display N/A for them
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_REQUEST_FINE_LOCATION:
                //Here permission is granted
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){//Permission is granted
                    permissionIsGranted=true;
                }
                //Here permission is denied and thus we do as described above
                else {
                    permissionIsGranted=false;
                    //Inform the user that we need to have the permission
                    Toast.makeText(getApplicationContext(),"For latitude, longitude and speed the app requires location permissions to be granted", Toast.LENGTH_LONG).show();
                    Intent back=new Intent(MapsActivity.this,MainActivity.class);
                    startActivity(back);
                    overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                }
                break;
        }
    }

    //Method which is being executed whenever we click the back arrow (on the top tight of the screen) and we return to the MainActivity
    public void go_back(View view){
        Intent back=new Intent(MapsActivity.this,MainActivity.class);
        startActivity(back);
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
    }

    //Make the slide animation whenever the back phone key is pressed
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            Intent back=new Intent(MapsActivity.this,MainActivity.class);
            startActivity(back);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
        }
        return super.onKeyUp( keyCode, event );
    }


}
