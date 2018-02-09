package com.sjsu.se195.uniride;

import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextWatcher;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sjsu.se195.uniride.models.DriverOfferPost;
import com.sjsu.se195.uniride.models.RideRequestPost;
import com.sjsu.se195.uniride.models.User;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageClickListener;
import com.synnapps.carouselview.ViewListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.Geocoder;
import android.location.Location;


import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewPostActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "NewPostActivity";
    private static final String REQUIRED = "Required";
    int NUMBER_OF_PAGES = 3;
    CarouselView formCarousel;
    private String source_place;
    private String source_place_redraw_check;
    private String destination_place;
    private boolean pickup_point_check = false;
    private int currentPosition;
    // [START declare_database_ref]
    private DatabaseReference mDatabase;
    // [END declare_database_ref]

    private PlaceAutocompleteFragment mSourceField;
    private EditText sourceFieldWatcher;
    private PlaceAutocompleteFragment mDestinationField;
    private FloatingActionButton mSubmitButton;
    private boolean postType = false; //true = driveOffer; false = rideRequest

    private EditText mpassengerCount;

    private LatLng mpickupPoint;

    private static final String DRIVER_TITLE = "Offer a Ride";
    private static final String RIDER_TITLE = "Request a Ride";

    private GoogleMap m_map;

    private MarkerOptions set_marker;
    private LatLng location_latlng;

    @Override
    public void onMapReady(GoogleMap map){
        //mapReady = true;
        m_map = map;
        if(!pickup_point_check) {
            set_marker = new MarkerOptions()
                    .position(new LatLng(this.location_latlng.latitude, this.location_latlng.longitude))
                    .title("title").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_48dp));
            m_map.addMarker(set_marker);
            CameraPosition target = CameraPosition.builder().target(location_latlng).zoom(14).build();
            m_map.moveCamera(CameraUpdateFactory.newCameraPosition(target));
        }

        else{
            m_map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener(){
                @Override
                public void onCameraMove(){
                    Log.d("Camera postion change" + "", m_map.getCameraPosition().target + "");
                    mpickupPoint = m_map.getCameraPosition().target;
                }
            });
        }
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            //coder.getFromLocationName(strAddress, 5)
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        return p1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        postType = getIntent().getBooleanExtra("driveOffer", true);

        if(postType){
            setContentView(R.layout.activity_2_drive_offer_post);
        }
        else{
            setContentView(R.layout.activity_2_ride_request_post);
        }

        // [START initialize_database_ref]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // [END initialize_database_ref]

        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_submit_post);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });

        formCarousel = (CarouselView) findViewById(R.id.carouselView);
        formCarousel.setPageCount(NUMBER_OF_PAGES);
        formCarousel.setViewListener(viewListener);
        formCarousel.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i1, float t, int i2) {
            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                pickup_point_check = false;
                if(position == 0) {
                    pickup_point_check = false;
                    mSourceField = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.field_source);
                    mSourceField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                        @Override
                        public void onPlaceSelected(Place place) {
                            //TODO: Get info about the selected place
                            Log.i(TAG, "Place: " + place.getName());
                            source_place = place.getAddress().toString();
                        }

                        @Override
                        public void onError(Status status) {
                            //TODO: Handle the error
                            Log.i(TAG, "An error occured: " + status);
                        }
                    });
                    if(source_place != null && !source_place.equals("") && !source_place.equals(source_place_redraw_check)) {
                        NewPostActivity.this.location_latlng = NewPostActivity.this.getLocationFromAddress(NewPostActivity.this, NewPostActivity.this.source_place);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(NewPostActivity.this);
                        source_place_redraw_check = source_place;
                    }
                }
                if(position == 1) {
                    pickup_point_check = false;
                    mDestinationField = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.field_destination);
                    mDestinationField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                        @Override
                        public void onPlaceSelected(Place place) {
                            //TODO: Get info about the selected place
                            Log.i(TAG, "Place: " + place.getName());
                            System.out.println("line 191: " +place.getName() );
                            destination_place = place.getAddress().toString();
                            NewPostActivity.this.location_latlng = NewPostActivity.this.getLocationFromAddress(NewPostActivity.this, NewPostActivity.this.destination_place);
                            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
                            mapFragment.getMapAsync(NewPostActivity.this);
                        }

                        @Override
                        public void onError(Status status) {
                            //TODO: Handle the error
                            Log.i(TAG, "An error occured: " + status);
                        }
                    });
                }
                if(position==2){
                    pickup_point_check = false;
                    if(postType)mpassengerCount = (EditText) findViewById(R.id.passengerCount);
                    else{
                        pickup_point_check = true;
                    }
                    mSubmitButton.setVisibility(View.VISIBLE);
                    mSubmitButton.invalidate();
                }
                else{
                    mSubmitButton.setVisibility(View.GONE);
                    mSubmitButton.invalidate();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    ViewListener viewListener = new ViewListener() {
        @Override
        public View setViewForPosition(int i) {
            View post_from = null;
            pickup_point_check = false;
            if(i == 0) {
                pickup_point_check = false;
                post_from= getLayoutInflater().inflate(R.layout.post_source_carousel, null);
                mSourceField = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.field_source);
                mSourceField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(Place place) {
                        //TODO: Get info about the selected place
                        Log.i(TAG, "Place: " + place.getName());
                        source_place = place.getAddress().toString();
                        NewPostActivity.this.location_latlng = NewPostActivity.this.getLocationFromAddress(NewPostActivity.this, NewPostActivity.this.source_place);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(NewPostActivity.this);
                    }

                    @Override
                    public void onError(Status status) {
                        //TODO: Handle the error
                        Log.i(TAG, "An error occured: " + status);
                    }
                });


            }
            if(i == 1) {
                pickup_point_check = false;
                post_from= getLayoutInflater().inflate(R.layout.post_destination_carousel, null);
                mDestinationField = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.field_destination);
                mDestinationField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(Place place) {
                        //TODO: Get info about the selected place
                        Log.i(TAG, "Place: " + place.getName());
                        System.out.println("line 258: " +place.getName() );
                        destination_place = place.getAddress().toString();
                        NewPostActivity.this.location_latlng = NewPostActivity.this.getLocationFromAddress(NewPostActivity.this, NewPostActivity.this.destination_place);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
                        mapFragment.getMapAsync(NewPostActivity.this);
                    }

                    @Override
                    public void onError(Status status) {
                        //TODO: Handle the error
                        Log.i(TAG, "An error occured: " + status);
                    }
                });
            }
            if(i==2){
                if(postType){
                    post_from= getLayoutInflater().inflate(R.layout.post_passengercount_carousel, null);
                    mpassengerCount = (EditText) post_from.findViewById(R.id.passengerCount);
                }
                else {
                    pickup_point_check = false;
                    post_from= getLayoutInflater().inflate(R.layout.post_pickuppoint_carousel, null);
                }
                mSubmitButton.setVisibility(View.VISIBLE);
                mSubmitButton.invalidate();
            }
            else{
                mSubmitButton.setVisibility(View.GONE);
                mSubmitButton.invalidate();
            }
            NewPostActivity.this.setTitle((TextView) post_from.findViewById(R.id.carousel_title));
            return post_from;
        }
    };


    /****Helper functions*****/
    private void setTitle(TextView tv){
        if(postType)tv.setText(DRIVER_TITLE);
        else tv.setText(RIDER_TITLE);
    }

    private void submitPost() {
        final String source = source_place;
        final String destination = destination_place;

        //LatLng pickupPoint_temp;
        int passengerCount_temp = 0;
        if(postType && !mpassengerCount.getText().toString().equals("")){
            passengerCount_temp = Integer.parseInt(mpassengerCount.getText().toString());
        }
        else if(postType && mpassengerCount.getText().toString().equals("")){
            mpassengerCount.setError(REQUIRED);
            return;
        }
        else{
            this.pickupPoint = mpickupPoint;
        }

        final int passengerCount = passengerCount_temp;
        final LatLng pickupPoint = pickupPoint_temp;

        //if drive offer post and passenger count empty
        if(postType && passengerCount_temp==0) {
            mpassengerCount.setError("Must be greater than 0.");
            return;
        }

        //if ride request post and pickup point empty
        if (!postType && TextUtils.isEmpty(pickupPoint_temp)) {
            mpickupPoint.setError(REQUIRED);
            return;
        }

        // Title is required
        if (TextUtils.isEmpty(source)) {
            //mSourceField.setError(REQUIRED);
            return;
        }

        // Body is required
        if (TextUtils.isEmpty(destination)) {
            //mDestinationField.setError(REQUIRED);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(NewPostActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            if(postType) {
                                writeNewDriveOfferPost(userId, user.username, source, destination, passengerCount);
                            }
                            else{
                                writeNewRideRequestPost(userId, user.username, source, destination, pickupPoint);
                            }
                        }

                        // Finish this Activity, back to the stream
                        setEditingEnabled(true);
                        finish();
                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        // [START_EXCLUDE]
                        setEditingEnabled(true);
                        // [END_EXCLUDE]
                    }
                });
        // [END single_value_read]
    }

    private void setEditingEnabled(boolean enabled) {
        //mSourceField.setEnabled(enabled);
        //mDestinationField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    // [START write_fan_out]

    //creating a drive offer
    private void writeNewDriveOfferPost(String userId, String username, String source, String destination, int count) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = mDatabase.child("posts").child("driveOffers").push().getKey();

        DriverOfferPost driverPost = new DriverOfferPost(userId, username, source, destination, count);
        Map<String, Object> postValues = driverPost.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/driveOffers/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/driveOffers/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
    }

    //creating a ride request
    private void writeNewRideRequestPost(String userId, String username, String source, String destination, String pickupPoint){
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = mDatabase.child("posts").child("rideRequests").push().getKey();

        RideRequestPost rideRequest = new RideRequestPost(userId, username, source, destination, pickupPoint);
        Map<String, Object> postValues = rideRequest.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/rideRequests/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/rideRequests/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
    }
    // [END write_fan_out]
}
