package com.emil.projectgps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        RoutingListener,
        GoogleMap.OnPolylineClickListener{

    private static final String TAG = "SearchActivity";
    private static final int REQUEST_CODE = 101 ;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    String currentUserId;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker currentUserLocationMarker;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.blue1, R.color.gray1, R.color.gray1, R.color.gray1, R.color.gray1};

    private GoogleMap mMap;
    private EditText searchText;
    private ImageView micImage;
    private ImageView centerImage;
    private ImageView clearRouteImage;
    private Switch shareLocationSwitch;
    private  boolean shareLocationFlag;

    private boolean startUpZoom = true;
    private boolean startUpConnection = true;

    private String[] loggedInMenuList = {"Add Friends", "Chat With Friends","View Friends", "Settings", "About The App", "Sign Out"};
    private String[] guestMenuList = {"Settings", "About The App", "Sign in"};
    private ListView listView;

    private ArrayList<Route> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        firestore= FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();


        Log.d(TAG, "onCreate: created");

        // submenu options
        listView = findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                loggedInMenuList));

        searchText = (EditText) findViewById(R.id.inputSearch);
        micImage = (ImageView) findViewById(R.id.micImage);
        centerImage = (ImageView) findViewById(R.id.centerImage);
        clearRouteImage = (ImageView) findViewById(R.id.centerImage);
        shareLocationSwitch = findViewById(R.id.switchBtn);
       // shareLocationFlag = false;

        if(firebaseAuth.getCurrentUser()!=null){
            currentUserId = firebaseAuth.getCurrentUser().getUid();
            initSwitchBtn();
        } else{
            shareLocationSwitch.setVisibility(View.INVISIBLE);
        }



        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylines = new ArrayList<>();

        centerView();
        textSearch();
        getSpeechInput();
        changeActivity();

    }

    public void initSwitchBtn(){
        shareLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getApplicationContext(), "Switch on", Toast.LENGTH_LONG).show();
                    shareLocationFlag = true;
                }else {
                    Toast.makeText(getApplicationContext(), "Switch off", Toast.LENGTH_LONG).show();
                    shareLocationFlag = false;
                    setShareLocationFalseFirestore();
                }
            }
        });
    }
    public void setShareLocationFalseFirestore(){
        DocumentReference dR = firestore.collection("users").document(currentUserId);
        Map<String, Object> userShareLocFalse = new HashMap<>();
        userShareLocFalse.put("shareLocation", false);
        dR.set(userShareLocFalse, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "shareLoc: Success: ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "shareLoc: Failure: ");
            }
        });

    }
    
    public void updatePositionFirestore(Location location){
        DocumentReference dR = firestore.collection("users").document(currentUserId);
        Map<String, Object> userPos = new HashMap<>();
        userPos.put("lat", location.getLatitude());
        userPos.put("long", location.getLongitude());
        userPos.put("shareLocation", true);
        dR.set(userPos, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "updatePositionFireStore: Success: Position added to database");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "updatePositionFireStore: Failure: Couldn't add position");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
       // shareLocationFlag = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (locationRequest==null) {
           addLocationRequest();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (googleApiClient != null && googleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }

    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    public void changeActivity() {
        if (FirebaseAuth.getInstance().getCurrentUser()!=null) {
            listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                    loggedInMenuList));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // Example to change activity
                    // startActivity(new Intent(getApplicationContext(),Login.class));
                    if (position == 0) {
                        // Add Friends
                        startActivity(new Intent(getApplicationContext(),AddNewFriend.class));
                    }
                    if (position == 1) {
                        // Chat With Friends
                    }
                    if (position == 2) {
                        // View Friends
                        startActivity(new Intent(getApplicationContext(),FriendList.class));
                    }
                    if (position == 3) {
                        // Settings
                    }
                    if (position == 4) {
                        // About The App
                        startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
                    }
                    if (position == 5) {
                        // Sign Out
                       FirebaseAuth.getInstance().signOut();
                       finish();
                       startActivity(new Intent(getApplicationContext(),LoginActivity.class));
                    }
                }
            });
        } else {
            listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                    guestMenuList));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // Example to change activity
                    // startActivity(new Intent(getApplicationContext(),Login.class));
                    if (position == 0) {
                        // Settings
                    }
                    if (position == 1) {
                        // About us
                        startActivity(new Intent(getApplicationContext(),AboutUsActivity.class));
                    }
                    if (position == 2) {
                        // Sign in
                        startActivity(new Intent(getApplicationContext(),LoginActivity.class));
                    }
                }
            });

        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        GoogleMapOptions options = new GoogleMapOptions();
        options.compassEnabled(false);
        options.ambientEnabled(false);

        mMap.setOnPolylineClickListener(this);
        Log.d(TAG, "Is it granted?");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted?");
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56,13.6), 14));

        } else  Log.d(TAG, "Permission not granted");

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void clearRoute(View v) {
        erasePolylines();
        mMap.clear();
    }

    public void centerView() {
        centerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));

            }

        });

    }

    // method checks if the enter button has been pressed
    public void textSearch() {
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN ||
                        event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    // execute method for searching
                    Log.d(TAG, "Before geolocate");
                    geoLocate();
                    Log.d(TAG, "After geolocate");
                }

                return false;
            }
        });
    }


    private void geoLocate() {
        closeKeyboard();

        String searchString = searchText.getText().toString();
        Log.d(TAG, "Search string: "+searchString);

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        // adds the found address to a list
        try {
            list = geocoder.getFromLocationName(searchString, 1);
            Log.d(TAG, "Address list: "+list.get(0));
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        // if there is an address found
        if (list.size() == 1) {
            Address address = list.get(0);
            Log.d(TAG, "Address Lat: "+address.getLatitude());
            Log.d(TAG, "Address Long: "+address.getLongitude());

            //starts the move camera method and pass the lat/ long and the found address
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), address.getAddressLine(0));
        } else {
            //displays a toast if no location is found
            Toast toast = Toast.makeText(getApplicationContext(), "No location found", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // method checks if the micImage has been clicked on
    public void getSpeechInput() {
        micImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

                // calls method to handle the result from the voice input
                startActivityForResult(intent, 10);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    geoLocate();
                }
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                // if there is an result the searchText will be set to that
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> reslut = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    searchText.setText(reslut.get(0));
                    geoLocate();
                }
                break;
        }
    }


    // method remove the old marker and sets the new marker position and moves the camera
    public void moveCamera(LatLng latLng, String title) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));

        // add route
        getRouteToMarker(latLng);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onLocationChanged(Location location) {


        lastLocation = location;
        if (shareLocationFlag){
            Log.d(TAG, "onLocationChanged: Lat: "+lastLocation.getLatitude() +" Long: "+ lastLocation.getLongitude());
            Log.d(TAG, "onLocationChanged: Send location to database");
            updatePositionFirestore(lastLocation);
        }else {
            Log.d(TAG, "onLocationChanged: Lat: " + lastLocation.getLatitude() + " Long: " + lastLocation.getLongitude());
        }

        if (currentUserLocationMarker != null) {
            currentUserLocationMarker.remove();
        }

        // this method is only call once on start up to zoom in on user location
        if (startUpZoom){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));

            startUpZoom = false;
        }

    }

    public void addLocationRequest(){
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (startUpConnection){
            locationRequest = new LocationRequest();
            locationRequest.setInterval(100);
            locationRequest.setFastestInterval(100);
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            startUpConnection = false;
        }
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }

    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void getRouteToMarker(LatLng markerLatLng) {
        Routing routing = new Routing.Builder()
                .key("AIzaSyAyLRDqDERlNknRlJ5PTAFzTtTAIEk-qII")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), markerLatLng)
                .build();
        routing.execute();

    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }


        //add route(s) to the map.
        polylines = new ArrayList<>();

        for (int i = 0; i < route.size(); i++) {

            // copy arrayList
            routes = new ArrayList<>(route);

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.clickable(true);
            polyOptions.addAll(route.get(i).getPoints());

            if (i == 0) {
                polyOptions.zIndex(1);
            } else {
                polyOptions.zIndex(0);
            }

            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

        }

        Toast.makeText(getApplicationContext(), "Fastest Route\n" + route.get(0).getDistanceText() +
                        "\n" + route.get(0).getDurationText()
                , Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }


    @Override
    public void onPolylineClick(Polyline polyline) {

        for (int i = 0; i < polylines.size(); i++) {

            polylines.get(i).setZIndex(i); // shortest first in array
            routes.get(i).setName(Integer.toString(i)); // shortest first in array

        }

        int polylineZIndex = (int) polyline.getZIndex();

        if (polylineZIndex == Integer.parseInt(routes.get(0).getName())) {
            Toast.makeText(getApplicationContext(), routes.get(0).getDurationText() + "\n" + routes.get(0).getDistanceText(), Toast.LENGTH_SHORT).show();

        } else if (polylineZIndex == Integer.parseInt(routes.get(1).getName())) {

            Toast.makeText(getApplicationContext(), routes.get(1).getDurationText() + "\n" + routes.get(1).getDistanceText(), Toast.LENGTH_SHORT).show();

        } else if (polylineZIndex == Integer.parseInt(routes.get(2).getName())) {
            Toast.makeText(getApplicationContext(), routes.get(2).getDurationText() + "\n" + routes.get(2).getDistanceText(), Toast.LENGTH_SHORT).show();
        }


        // makes all the polylines gray
        for (Polyline p : polylines) {
            p.setColor(Color.rgb(172, 181, 189));
            p.setZIndex(0);
        }


        // makes the clicked on plolyline blue and moved to the top
        polyline.setColor(Color.rgb(70, 156, 227));
        polyline.setZIndex(1);


    }

}
