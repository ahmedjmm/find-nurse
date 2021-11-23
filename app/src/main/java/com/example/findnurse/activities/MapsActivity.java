package com.example.findnurse.activities;

import static com.example.findnurse.Home.firebaseAuth;
import static com.example.findnurse.Home.firebaseUser;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.findnurse.Home;
import com.example.findnurse.Nurses;
import com.example.findnurse.R;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //user data
    public static String userId;
    private String deviceTokenID;
    private String email;
    private String password;
    public static String name;
    private String gender;
    private String age;
    private String mobileNumber;
    private boolean locationPermissionGranted = false;
    private Location currentLocation;
    private String longitude, latitude;

    //firestore
    public static DocumentReference userDocRef;

    private GoogleMap mMap;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private LocationCallback locationCallback;


    private final List<Nurses> nursesList = new ArrayList<>();
    private NursesListAdapter nursesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkUserData();

        checkLanguage();
        setContentView(R.layout.activity_maps);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.title);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseSignIn();

        getLocationPermission();
        createLocationRequest();
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                try {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        // ...
                        lastLocation = location;
                        longitude = String.valueOf(lastLocation.getLongitude());
                        latitude = String.valueOf(lastLocation.getLatitude());
                        Map<String, Object> locationUpdates = new HashMap<>();
                        locationUpdates.put("longitude", longitude);
                        locationUpdates.put("latitude", latitude);
                        userDocRef.update(locationUpdates);
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(16f));
                    }
                } catch (NullPointerException e) {
                    Log.e("location callback error", Objects.requireNonNull(e.getMessage()));
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.getMainLooper());

        TextView emptyList = findViewById(R.id.empty_list);
        // views
        ListView nursesListView = findViewById(R.id.nurses_list);
        nursesListView.setEmptyView(emptyList);
        nursesListAdapter = new NursesListAdapter(this, nursesList);
        nursesListView.setAdapter(nursesListAdapter);
        Nurses[] nurses = new Nurses[1];
        Home.database.collection("nurses_available").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("snapshot", "listen:error", error);
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            for (DocumentChange dc : Objects.requireNonNull(value).getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    nurses[0] = new Nurses(dc.getDocument().getString("name"),
                            dc.getDocument().getId(),
                            dc.getDocument().getString("FCM_token"), userId);
                    nursesListAdapter.add(nurses[0]);
                }
                if (dc.getType() == DocumentChange.Type.REMOVED)
                    nursesListAdapter.remove(nurses[0]);
            }
        });
        nursesListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("documentId", nursesList.get(position).document_id);
            intent.putExtra("nurseName", nursesList.get(position).nurse_name);
            intent.putExtra("FCM_token", nursesList.get(position).FCM_token);
            intent.putExtra("auth_id", nursesList.get(position).auth_id);
            startActivity(intent);
        });
    }

    private void checkUserData() {
        try{
            email = PreferenceManager.getDefaultSharedPreferences(this).getString("email", "");
            password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", "");
            name = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");
            gender = PreferenceManager.getDefaultSharedPreferences(this).getString("gender", "");
            age = PreferenceManager.getDefaultSharedPreferences(this).getString("age", "");
            if (email.equals("") || password.equals("") || name.equals("") || Objects.requireNonNull(age).equals("")
                    || gender.equals("")) {
                Toast.makeText(getApplicationContext(), "No user data", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
            }
        }
        catch(NullPointerException nullPointerException){
        Log.d("checkUserData()", nullPointerException.getMessage());
        }
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
//         Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        if (locationPermissionGranted && checkInternet())
            getDeviceLocation();
    }

    private boolean checkInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            if (networkInfo != null && networkInfo.isConnected())
                return true;
            else
                Toast.makeText(this, R.string.check_connection, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int item : grantResults) {
                    if (grantResults[item] != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            locationPermissionGranted = true;
            initMap();
        }
    }

    public void firebaseSignIn() {
        String email = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("email", "");
        String password = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("password", "");
        if (Objects.requireNonNull(email).equals("") || Objects.requireNonNull(password).equals("")) {
            startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
            finish();
        } else {
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    mobileNumber = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getPhoneNumber();
                    userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    deviceTokenID = FirebaseInstanceId.getInstance().getId();
                    Map<String, Object> userDataMap = new HashMap<>();
                    userDataMap.put("user_id", userId);
                    userDataMap.put("FCM_token", Home.FCMToken);
                    userDataMap.put("device_token", deviceTokenID);
                    userDataMap.put("name", name);
                    userDataMap.put("mobile", mobileNumber);
                    userDataMap.put("age", age);
                    userDataMap.put("gender", gender);
                    userDataMap.put("latitude", latitude);
                    userDataMap.put("longitude", longitude);
                    userDocRef = Home.database.collection("users_available").document(userId);
                    userDocRef.set(userDataMap);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("sign in fail", e.toString());
                startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                finish();
            });
        }
    }

    private void initMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(supportMapFragment).getMapAsync(this);
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {
                            currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(Objects.requireNonNull(currentLocation).getLatitude(),
                                    currentLocation.getLongitude()), 15f);
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMapToolbarEnabled(true);
//                              addLocationAndTokenToFirestore(currentLocation);
                            //to add mark on current location
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())).title("Marker in Sydney"));
                        } catch (NullPointerException e) {
                            Toast.makeText(MapsActivity.this, R.string.enable_location,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("getDeviceLocation()", Objects.requireNonNull(e.getMessage()));
                });
            }
        } catch (SecurityException ignored) {
        }
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void moveCamera(LatLng latLng, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    public void getLocationPermission() {
        String[] permissions = {FINE_LOCATION, COURSE_LOCATION};
        if (ContextCompat.checkSelfPermission(getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            if (ContextCompat.checkSelfPermission(getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initMap();
            } else
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        else
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void language(String langCode) {
        Resources res = getResources();
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        config.setLayoutDirection(locale);
        res.updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void checkLanguage() {
        String langCode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("language", "en");
        if (Objects.requireNonNull(langCode).equals("ar"))
            language("ar");
        else
            language("en");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.reset_app:
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                editor.remove("password");
                editor.remove("gender");
                editor.remove("mobileNumber");
                editor.remove("email");
                editor.remove("name");
                editor.remove("age");
                editor.commit();
                startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
                finish();
                break;
            case R.id.arabic:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "ar").commit();
                language("ar");
                i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
            case R.id.english:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "en").commit();
                language("en");
                i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        checkLanguage();
//        if (email == "" || password == "" || name == "" || mobileNumber == ""
//                || age == "" || gender == "") {
//            startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
//            finish();
//        } else if (firebaseAuth == null) {
//            firebaseAuth = FirebaseAuth.getInstance();
//        }
//    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        checkLanguage();
//        if (Home.firebaseUser == null) {
//            firebaseSignIn();
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//        }
//    }


    @Override
    protected void onStop() {
        super.onStop();
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (email == "" || password =="" || name =="" || age == "" || gender =="") {
            startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
            finish();
        }
        checkLanguage();
        if (firebaseUser == null) {
            firebaseSignIn();
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper());
        }
    }

    static class NursesListAdapter extends ArrayAdapter<Nurses> {
        Context context;
        List<Nurses> nursesList;
        ViewHolder viewHolder;

        public NursesListAdapter(@NonNull Context context, @NonNull List<Nurses> nursesList) {
            super(context, 0, nursesList);
            this.context = context;
            this.nursesList = nursesList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).
                        inflate(R.layout.nurses_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.nurse_name);
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.textView.setText(nursesList.get(position).nurse_name);
            return convertView;
        }

        @Override
        public int getCount() {
            return nursesList.size();
        }

        @Override
        public Nurses getItem(int position) {
            return nursesList.get(position);
        }

        static class ViewHolder {
            TextView textView;
        }
    }
}