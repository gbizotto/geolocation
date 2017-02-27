package br.gbizotto.geolocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int PERMISSION_LOCATION = 1;

    @BindView(R.id.txtCoordinates)
    TextView mTxtCoordinates;
    @BindView(R.id.txtZipCode)
    TextView mTxtZipCode;
    @BindView(R.id.txtAddress)
    TextView mTxtAddress;

    private Context mContext;

    private GoogleApiClient mGoogleApiClient;
    private static Double mLastLongitude;
    private static Double mLastLatitude;
    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested = true;

    /**
     * The formatted location address.
     */
    protected String mAddressOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        ButterKnife.bind(this);

        mResultReceiver = new AddressResultReceiver(new Handler());

    }

    @OnClick(R.id.btnFind)
    public void onFindMyAddressClick() {
        if (hasLocationPermission()) {
            buildGoogleApiClient();
            // Only start the service to fetch the address if GoogleApiClient is
            // connected.
            if (mGoogleApiClient.isConnected() && mLastLocation != null) {
                startIntentService();
            }
            // If GoogleApiClient isn't connected, process the user's request by
            // setting mAddressRequested to true. Later, when GoogleApiClient connects,
            // launch the service to fetch the address. As far as the user is
            // concerned, pressing the Fetch Address button
            // immediately kicks off the process of getting the address.
            mAddressRequested = true;
        }
    }

    private boolean hasLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION);

            return false;
        }

        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            // Inicia o servico de localizacao
            startLocationSearch();
        }
    }

    private void startLocationSearch(){
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLatitude = location.getLatitude();
        mLastLongitude = location.getLongitude();

        StringBuilder coordinates = new StringBuilder();
        coordinates.append(mLastLatitude)
                .append(",")
                .append(mLastLongitude);

        mTxtCoordinates.setText(coordinates);

        mLastLocation = location;
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        try {
            LocationRequest mLocationRequest = LocationUtils.createLocationRequest();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(MainActivity.class.getSimpleName(),getString(R.string.error_localization));
                Toast.makeText(getApplication(), getString(R.string.error_localization), Toast.LENGTH_LONG).show();
            }else {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }

            // Only start the service to fetch the address if GoogleApiClient is
            // connected.
            if (mGoogleApiClient.isConnected() && mLastLocation != null) {
                startIntentService();
            }
            // If GoogleApiClient isn't connected, process the user's request by
            // setting mAddressRequested to true. Later, when GoogleApiClient connects,
            // launch the service to fetch the address. As far as the user is
            // concerned, pressing the Fetch Address button
            // immediately kicks off the process of getting the address.
            mAddressRequested = true;

        } catch (Exception e) {
            Log.e(MainActivity.class.getSimpleName(), e.getMessage(), e);
            tryAgain();
        }


        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            // It is possible that the user presses the button to get the address before the
            // GoogleApiClient object successfully connects. In such a case, mAddressRequested
            // is set to true, but no attempt is made to fetch the address (see
            // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
            // user has requested an address, since we now have a connection to GoogleApiClient.
            if (mAddressRequested) {
                startIntentService();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull com.google.android.gms.common.ConnectionResult connectionResult) {

    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(FetchAddressIntentService.RECEIVER, mResultReceiver);
        intent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    private void tryAgain() {
        LocationUtils.disconnectFromLocationServices(mGoogleApiClient, this);
        mGoogleApiClient.connect();
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == FetchAddressIntentService.SUCCESS_RESULT) {
                Toast.makeText(mContext, R.string.address_found, Toast.LENGTH_LONG).show();
            }

        }
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        mTxtAddress.setText(mAddressOutput);
    }
}
