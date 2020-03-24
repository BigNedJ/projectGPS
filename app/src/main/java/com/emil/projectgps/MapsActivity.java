package com.emil.projectgps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        RoutingListener,
        GoogleMap.OnPolylineClickListener{

    private static final String TAG = "SearchActivity";
    private static final int REQUEST_CODE = 101 ;
    int PROXIMITY_RADIUS = 10000;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    String currentUserId;


    private LocationManager locationManager;
    private LocationListener locationListener;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker currentUserLocationMarker;
    private LatLng markerLatLong;
    double latitude,longitude;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.blue1, R.color.gray1, R.color.gray1, R.color.gray1, R.color.gray1};

    private GoogleMap mMap;
    private EditText searchText;
    private ImageView micImage;
    private ImageView centerImage;
    private TextView currentSpeed;
    private Switch shareLocationSwitch;
    private  boolean shareLocationFlag;

    private boolean startUpZoom;
    private boolean startUpZoom2 = true;
    private boolean startUpConnection = true;
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String SWITCH = "switch";


    private String[] loggedInMenuList = {"Add Friends", "Chat With Friends","View Friends", "Show Friends On Map","Nearby Places", "About The App", "Sign Out"};
    private String[] guestMenuList = { "About The App","Nearby Places", "Sign In"};
    private ListView listView;

    private ArrayList<Route> routes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();


        Log.d(TAG, "onCreate: created");

        // submenu options
        listView = findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                loggedInMenuList));

        currentSpeed = findViewById(R.id.speedTextView);
        searchText = (EditText) findViewById(R.id.inputSearch);
        micImage = (ImageView) findViewById(R.id.micImage);
        //centerImage = (ImageView) findViewById(R.id.centerImage);
        shareLocationSwitch = findViewById(R.id.switchBtn);
        // shareLocationFlag = false;

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
            initSwitchBtn();
        } else {
            shareLocationSwitch.setVisibility(View.INVISIBLE);
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylines = new ArrayList<>();

        //centerView();
        textSearch();
        getSpeechInput();
        changeActivity();
        loadSwitchState();

        startUpZoom= true;


      /*  Intent intent = getIntent();

      //  Bundle extras = getIntent().getExtras();
       // if (extras != null) {
            //Toast.makeText(this, "Bundle is not null", Toast.LENGTH_LONG).show();
                //Log.d(TAG, "onCreate: extre bundless != null");
                String userID = intent.getStringExtra("USER_ID");
                String username = intent.getStringExtra("USER_NAME");

                if (userID!=null && username!=null){
                    showLocationOfFriendOnMap(userID, username);
                }
               // Toast.makeText(this, "UserID: " + userID + " Name: " + username, Toast.LENGTH_LONG).show();
               // Log.d("MapsActivty On Create:", "ID " + userID + " Name: " + username);
               // showLocationOfFriendOnMap(userID, username);
                //The key argument here must match that used in the other activity
       // }

       */
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: destroyed");
        saveSwitchState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpZoom= true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        startUpZoom= true;
    }

    public void initSwitchBtn(){
        shareLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getApplicationContext(), "Switch on", Toast.LENGTH_SHORT).show();
                    shareLocationFlag = true;

                }else {
                    Toast.makeText(getApplicationContext(), "Switch off", Toast.LENGTH_SHORT).show();
                    shareLocationFlag = false;
                    setShareLocationFalseFirestore();
                }
            }
        });
    }
    public void saveSwitchState(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SWITCH, shareLocationSwitch.isChecked());

        editor.apply();
    }

    public void loadSwitchState(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        boolean switchState = sharedPreferences.getBoolean(SWITCH,false);

        shareLocationSwitch.setChecked(switchState);
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

    public void showLocationOfFriendOnMap(String userID, final String username){
        DocumentReference dR = firestore.collection("users").document(userID);
        dR.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                double lat = documentSnapshot.getDouble("lat");
                double longitude = documentSnapshot.getDouble("long");

                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, longitude)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat,longitude)).title(username));

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: showLocationOfFriendOnMap");
            }
        });
    }

    public void showAllFriendsOnMap(){
        final List<UsernameAndID> list = new ArrayList<>();
        final Map<String, LatLng> friendPosMap = new HashMap<>();
        final List<String> friendNames;

        firestore.collection("users").document(currentUserId).collection("Friends")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                String name = (String)document.get("friend");
                                String userID = document.getId();
                                list.add(new UsernameAndID(name, userID));
                            }
                            CollectionReference cR = firestore.collection("users");
                            cR.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                           // Log.d(TAG, document.getId() + " => " + document.getData());
                                            String name = (String)document.get("Username");
                                            String userID = document.getId();
                                            for (UsernameAndID usi:list) {
                                                if (usi.getId().equals(userID)){
                                                    boolean isSharing = (boolean)document.get("shareLocation");
                                                    Log.d(TAG, name + " exists in friends ");
                                                    if (isSharing) {

                                                        double lat = (double) document.get("lat");
                                                        double longitude = (double) document.get("long");
                                                        LatLng latLng = new LatLng(lat, longitude);
                                                        friendPosMap.put(name, latLng);
                                                    }
                                            }
                                           }
                                            }

                                        if (!friendPosMap.isEmpty()){
                                            Log.d(TAG, "users are sharing their pos");
                                          for (Map.Entry<String, LatLng> entry: friendPosMap.entrySet()){
                                              putMarkerOnMap(entry.getValue(), entry.getKey());
                                          }

                                        }else  {
                                            Log.d(TAG, "No users are sharing their pos");
                                            Toast.makeText(MapsActivity.this, "No friends are currently sharing their position", Toast.LENGTH_LONG).show();
                                        }

                                           // list.add(new UsernameAndID(name, userID));
                                        }
                                }
                            });

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    public void putMarkerOnMap(LatLng latLng, String title){
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
    }

    @Override
    protected void onStop() {
        super.onStop();
       // shareLocationFlag = false;
        Log.d(TAG, "onStop: ");
        saveSwitchState();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startUpZoom= true;

        if (locationRequest==null) {
           addLocationRequest();
            Log.d(TAG, "onStart: locaionrequestt null");
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (googleApiClient != null && googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
                Log.d(TAG, "onStart: client is connected and googleapiclient= not null");
            }
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
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    }
                    if (position == 2) {
                        // View Friends
                        startActivity(new Intent(getApplicationContext(),FriendList.class));
                    }
                    if (position == 3) {
                        // showFriends
                        showAllFriendsOnMap();
                    }
                    if (position == 4) {
                        // Nearby Places
                        displayNearbyPlaces();

                    }
                    if (position == 5) {
                        // About The App
                        startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
                    }
                    if (position == 6) {
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
                        // About us
                        startActivity(new Intent(getApplicationContext(),AboutUsActivity.class));
                    }
                    if (position == 1) {
                        // Nearby Places
                        displayNearbyPlaces();

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
        Log.d(TAG, "onMapReady: ");

        mMap.setPadding(150,180,0,0);
        GoogleMapOptions options = new GoogleMapOptions();
        options.compassEnabled(true);
        options.zoomControlsEnabled(true);
        //options.ambientEnabled(false);


        mMap.setOnPolylineClickListener(this);
        Log.d(TAG, "Is it granted?");
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
                return;
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted");
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

//    public void centerView() {
//        centerImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (lastLocation!=null) {
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
//                    mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
//                } else Toast.makeText(getApplicationContext(), "Current location not available. Try turning GPS on", Toast.LENGTH_LONG).show();
//
//            }
//
//        });
//
//    }

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
        markerLatLong = latLng;

        // calculate the distance between marker an user position
        float[] results = new float[1];
        Location.distanceBetween(lastLocation.getLatitude(),lastLocation.getLongitude(),
                latLng.latitude,latLng.longitude,results);
        int distance = (int) results[0] / 1000; // result in metric mil

        //Toast.makeText(getApplicationContext(),"val: " + distance, Toast.LENGTH_SHORT).show();
        float zoomTo = 6.0f;
        if (distance < 10){
            zoomTo = 12.0f;
        }else if (distance < 20){
            zoomTo = 9.5f;
        }
        else if (distance < 40){
           zoomTo = 8.5f;
        }
        else if (distance < 60){
          zoomTo = 8.0f;
        }
        else if (distance < 80){
            zoomTo = 7.8f;
        }
        else if (distance < 100){
            zoomTo = 7.5f;
        }
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomTo));

        // add route
        getRouteToMarker(latLng);

    }

    // clear the route if destination is less den 25m from user location
    public void atDestination(){
        float[] results = new float[1];
        Location.distanceBetween(lastLocation.getLatitude(),lastLocation.getLongitude(),
                markerLatLong.latitude,markerLatLong.longitude,results);
        int distance = (int) results[0]; // result in meter

        if (distance <= 25){
            erasePolylines();
            mMap.clear();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        if (polylines.size() > 0){
            atDestination();
        }

        // current speed
        int speed = (int) location.getSpeed();
        currentSpeed.setText(speed + " Km/h");

        lastLocation = location;

        if (shareLocationFlag){
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
            Log.d(TAG, "onLocationChanged: start up zoom: lat"+ location.getLatitude() +"Long: "+ location.getLongitude());
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            Intent intent = getIntent();
            String userID = intent.getStringExtra("USER_ID");
            String username = intent.getStringExtra("USER_NAME");

            if (userID!=null && username!=null){
                showLocationOfFriendOnMap(userID, username);
            }else {
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                //mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
            }

            startUpZoom = false;
        }

        if (startUpZoom2){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));

            startUpZoom2 = false;
        }

    }

    public void addLocationRequest(){
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG, "onConnected and onstart: true");
        if (startUpConnection){
            locationRequest = new LocationRequest();
            locationRequest.setInterval(100);
            locationRequest.setFastestInterval(100);
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            startUpConnection = false;
        }
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //Display nearby places

    public void displayNearbyPlaces() {

        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        String url;

        String hospital = "hospital";
        url = getUrl(latitude, longitude, hospital);
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        getNearbyPlacesData.execute(dataTransfer);
        Toast.makeText(MapsActivity.this, "Showing Nearby Hospitals", Toast.LENGTH_SHORT).show();
/*

        String school = "school";
        url = getUrl(latitude, longitude, school);
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        getNearbyPlacesData.execute(dataTransfer);
        Toast.makeText(MapsActivity.this, "Showing Nearby Schools", Toast.LENGTH_SHORT).show();


        String restaurant = "restaurant";
        url = getUrl(latitude, longitude, restaurant);
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        getNearbyPlacesData.execute(dataTransfer);
        Toast.makeText(MapsActivity.this, "Showing Nearby Restaurants", Toast.LENGTH_SHORT).show();

 */

    }

    private String getUrl(double latitude , double longitude , String nearbyPlace)
    {

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type="+nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        //Byta API key
        //Bytad
        googlePlaceUrl.append("&key="+"AIzaSyCOrnaEDRPY9t2mwxCdtZuPhGEZdAcoowQ");

        Log.d("MapsActivity", "url = "+googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }
}
