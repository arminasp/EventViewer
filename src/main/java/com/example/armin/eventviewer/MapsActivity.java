package com.example.armin.eventviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import static com.example.armin.eventviewer.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // constants
    private final int MY_PERMISSIONS_REQUEST_READ_ACCESS_FINE_LOCATION = 0;

    // connection
    private Connection connection;
    private String accessToken;

    // map values
    private int distance;

    // main vars
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private HashMap<Marker, Event> eventMap;
    private Event currentEvent;
    private Location myLocation;

    // booleans
    private boolean firstStart = true;
    private boolean connected = false;
    private boolean locationAvailable = false;
    private boolean markersFilled = false;
    private boolean downloadFailed = false;
    private boolean doubleBackToExitPressedOnce = false;

    // view components
    SlidingUpPanelLayout layout;

    private class Connection extends AsyncTask<String, Void, String> {
        // connecting, connected, location, download, success, fail
        String state = "";
        ConnectivityManager connectivityManager;
        NetworkInfo activeNetworkInfo;

        Connection() {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        protected String doInBackground(String... params) {
            while (true) {
                if (isCancelled()) {
                    break;
                }

                if (Objects.equals(state, "download")) {
                    if (markersFilled) {
                        state = "success";
                        publishProgress();
                    } else if (downloadFailed) {
                        downloadFailed = false;

                        if (distance >= 1000) {
                            distance -= 1000;
                        }

                        state = "fail";
                        publishProgress();
                    }
                }

                if ((!Objects.equals(state, "success")) && (!Objects.equals(state, "fail"))) {
                    activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

                    if ((activeNetworkInfo != null) && (activeNetworkInfo.isConnected())) {
                        if (!connected) {
                            connected = true;
                            state = "connected";
                            publishProgress();
                            continue;
                        }
                    } else {
                        if (connected) {
                            connected = false;
                        }
                        state = "connecting";
                    }

                    if (myLocation == null) {
                        state = "location";
                    } else if (!locationAvailable) {
                        locationAvailable = true;
                    }

                    if (!Objects.equals(state, "download")) {
                        if (!markersFilled && connected && locationAvailable) {
                            state = "download";
                            publishProgress();
                            continue;
                        }

                        if (!Objects.equals(state, "connected")) {
                            publishProgress();
                        }
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Void... values) {
            LinearLayout loadingPanel = (LinearLayout) findViewById(R.id.loading_panel);
            TextView loadingText = (TextView) findViewById(R.id.loading_text);

            switch (state) {
                case "connecting":
                    if (loadingPanel.getVisibility() == LinearLayout.INVISIBLE) {
                        loadingPanel.setVisibility(LinearLayout.VISIBLE);
                    }
                    if (loadingText.getVisibility() == TextView.INVISIBLE) {
                        loadingText.setVisibility(TextView.VISIBLE);
                    }
                    loadingText.setText("Waiting for network access");
                    break;
                case "location":
                    if (loadingPanel.getVisibility() == LinearLayout.INVISIBLE) {
                        loadingPanel.setVisibility(LinearLayout.VISIBLE);
                    }
                    if (loadingText.getVisibility() == TextView.INVISIBLE) {
                        loadingText.setVisibility(TextView.VISIBLE);
                    }
                    loadingText.setText("Waiting for location access");
                    break;
                case "connected":
                    if (markersFilled && (loadingPanel.getVisibility() == LinearLayout.VISIBLE)) {
                        loadingPanel.setVisibility(LinearLayout.INVISIBLE);
                    } else if (loadingText.getVisibility() == TextView.VISIBLE) {
                        loadingText.setVisibility(TextView.INVISIBLE);
                    }
                    break;
                case "download":
                    if (loadingText.getVisibility() == TextView.VISIBLE) {
                        loadingText.setVisibility(TextView.INVISIBLE);
                    }
                    downloadData(myLocation, distance);
                    break;
                case "success":
                    if (loadingPanel.getVisibility() == TextView.VISIBLE) {
                        loadingPanel.setVisibility(TextView.INVISIBLE);
                    }
                    state = "connected";
                    break;
                case "fail":
                    state = "download";
                    downloadData(myLocation, distance);
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(1000);

        accessToken = getString(R.string.access_token);
        distance = getResources().getInteger(R.integer.distance);
        layout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        eventMap = new HashMap<>();
        connection = new Connection();
        connection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        if (connection.isCancelled()) {
            connection = new Connection();
            connection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (currentEvent == null) {
            layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if (connection.getStatus() == AsyncTask.Status.RUNNING) {
            connection.cancel(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recreate();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_ACCESS_FINE_LOCATION);
            }
        } else {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(new MarkerClickListener(this));
        }
    }

    @SuppressLint("SetTextI18n")
    public void fillEvent(Event event) {
        ImageView eventPicturePanel = (ImageView) findViewById(R.id.event_picture);
        ImageView profilePicturePanel = (ImageView) findViewById(R.id.profile_picture);
        TextView descriptionPanel = (TextView) findViewById(R.id.description);
        TextView titlePanel = (TextView) findViewById(R.id.title_view);
        TextView dateView = (TextView) findViewById(R.id.date_view);
        TextView attendingView = (TextView) findViewById(R.id.attending_view);
        Button shareButton = (Button) findViewById(R.id.share_button);
        Button nextEventButton = (Button) findViewById(R.id.next_event_button);
        ScrollView scrollView = (ScrollView) findViewById(R.id.description_scroll);

        eventPicturePanel.setImageBitmap(null);

        if (event.getEventPicture() == null) {
            if (connected) {
                event.downloadEventPicture(eventPicturePanel);
            }
        } else {
            eventPicturePanel.setImageBitmap(event.getEventPicture());
        }

        profilePicturePanel.setImageBitmap(event.getProfilePicture());
        descriptionPanel.setText(event.getDescription());
        titlePanel.setText(event.getName());

        if ((event.getStartTime().compareTo(new Date()) < 0) && (calculateDistance(
                myLocation.getLatitude(), myLocation.getLongitude(),
                event.getLatitude(), event.getLongitude()) <= 1.5)) {
            dateView.setVisibility(TextView.GONE);
            shareButton.setVisibility(Button.VISIBLE);
        } else {
            dateView.setVisibility(TextView.VISIBLE);
            shareButton.setVisibility(Button.GONE);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dateView.setText(formatter.format(event.getStartTime()));
        }

        attendingView.setText(event.getAttendingCount().toString() + " going");

        if ((event.getConnectedEventList().size() == 0) && (event.getPrimaryEvent() == null)) {
            nextEventButton.setVisibility(Button.GONE);
        } else {
            nextEventButton.setVisibility(Button.VISIBLE);
        }

        scrollView.smoothScrollTo(scrollView.getScrollX(), 0);
        currentEvent = event;
    }

    public void onNextEventButtonClicked(View view) {
        if (currentEvent != null) {
            if (currentEvent.getConnectedEventList().size() != 0) {
                ArrayList<Event> eventList = currentEvent.getConnectedEventList();
                Event newEvent = eventList.get(0);

                if (newEvent.getPrimaryEvent() == null) {
                    newEvent.setPrimaryEvent(currentEvent);
                }

                if (newEvent.getProfilePicture() == null) {
                    newEvent.setProfilePicture(currentEvent.getProfilePicture());
                }
                fillEvent(newEvent);
            } else if (currentEvent.getPrimaryEvent() != null) {
                Event primaryEvent = currentEvent.getPrimaryEvent();
                ArrayList<Event> eventList = primaryEvent.getConnectedEventList();
                int index = eventList.indexOf(currentEvent);
                Event newEvent;

                if ((index != -1) && (eventList.size() > index + 1)) {
                    newEvent = eventList.get(index + 1);

                    if (newEvent.getPrimaryEvent() == null) {
                        newEvent.setPrimaryEvent(primaryEvent);
                    }

                    if (newEvent.getProfilePicture() == null) {
                        newEvent.setProfilePicture(primaryEvent.getProfilePicture());
                    }
                } else {
                    newEvent = primaryEvent;
                }

                fillEvent(newEvent);
            }
        }
    }

    private void sharePhotoToFacebook(Bitmap image) {
        if (ShareDialog.canShow(SharePhotoContent.class)) {
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(image)
                    .build();

            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(photo)
                    .build();

            ShareDialog shareDialog = new ShareDialog(this);
            shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int REQUEST_IMAGE_CAPTURE = 1;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            sharePhotoToFacebook(imageBitmap);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1);
        }
    }

    public void onShareButtonClicked(View view) {
        dispatchTakePictureIntent();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (myLocation == null) {
            myLocation = location;
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                if (firstStart) {
                    myLocation = location;
                    handleNewLocation(location);
                    firstStart = false;
                }
            }
        }
    }

    private void handleNewLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
    }

    private void downloadData(Location location, int distance) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String ip = getString(R.string.ip);
        int port = getResources().getInteger(R.integer.port);

        new DataDownloadTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "http://" + ip +
                ":" + port + "/events?lat=" + latitude + "&lng=" + longitude + "&distance=" + distance +
                "&sort=venue&accessToken=" + accessToken);
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (layout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 1000);
        }
    }

    // getters & setters
    public GoogleMap getmMap() {
        return mMap;
    }

    public boolean isConnected() {
        return connected;
    }

    public SlidingUpPanelLayout getLayout() {
        return layout;
    }

    public void setLayout(SlidingUpPanelLayout layout) {
        this.layout = layout;
    }

    public HashMap<Marker, Event> getEventMap() {
        return eventMap;
    }

    public Event getCurrentEvent() {
        return currentEvent;
    }

    public void setMarkersFilled(boolean markersFilled) {
        this.markersFilled = markersFilled;
    }

    public void setDownloadFailed(boolean downloadFailed) {
        this.downloadFailed = downloadFailed;
    }
}