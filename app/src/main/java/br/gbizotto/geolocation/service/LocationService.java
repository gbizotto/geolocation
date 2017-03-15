package br.gbizotto.geolocation.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.gbizotto.geolocation.R;
import br.gbizotto.geolocation.utils.PermissionsUtils;


public class LocationService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String LOG_TAG = LocationService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    protected ResultReceiver mReceiver;

    public LocationService(String name) {
        super(name);
    }

    public LocationService(){
        super(LocationService.class.getSimpleName());
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra(getString(R.string.fetch_address_receiver));

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(LocationService.class.getSimpleName(), "No receiver received. There is nowhere to send the results.");
            return;
        }

        if (isNetworkAvailable()) {
            buildGoogleApiClient();
        } else {
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), getString(R.string.no_internet_connection));
        }
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
    public void onConnected(Bundle bundle) {
        try {
            mLocationRequest = createLocationRequest();
            if (PermissionsUtils.checkLocationPermission(this)) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), getString(R.string.error_localization));
            disconnectFromLocationServices(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG, "conexao suspensa, erro = " + i);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG_TAG,"entrou em LocationService, onLocationChanged");
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String errorMessage = "";

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(LocationService.class.getSimpleName(), errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(LocationService.class.getSimpleName(), errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(LocationService.class.getSimpleName(), errorMessage);
            }
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), errorMessage, null);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_success_result),
                    TextUtils.join(System.getProperty("line.separator"), addressFragments), address);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), getString(R.string.error_localization));
        disconnectFromLocationServices(mGoogleApiClient, this);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(30 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(10 * 1000); // 10 second, in milliseconds

        return locationRequest;
    }

    private void disconnectFromLocationServices(GoogleApiClient googleApiClient, LocationListener locationListener) {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
            googleApiClient.disconnect();
        }
    }

    /**
     * Sends a resultCode and message to the receiver.
     */
    private void deliverResultToReceiver(int resultCode, String message, Address address) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.fetch_address_result_data), message);
        if (address != null) {
            bundle.putParcelable(getString(R.string.fetch_address_result_addres), address);
        }
        mReceiver.send(resultCode, bundle);
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.fetch_address_result_data), message);
        mReceiver.send(resultCode, bundle);
    }
}
