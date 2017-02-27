package br.gbizotto.geolocation;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FetchAddressIntentService extends IntentService {

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    protected ResultReceiver mReceiver;

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v(FetchAddressIntentService.class.getSimpleName(), "entrou!");

        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(getString(R.string.fetch_address_receiver));

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(FetchAddressIntentService.class.getSimpleName(), "No receiver received. There is nowhere to send the results.");
            return;
        }

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(getString(R.string.fetch_address_location_extra));

        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(FetchAddressIntentService.class.getSimpleName(), errorMessage);
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), errorMessage, null);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(FetchAddressIntentService.class.getSimpleName(), errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(FetchAddressIntentService.class.getSimpleName(), errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(FetchAddressIntentService.class.getSimpleName(), errorMessage);
            }
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_failure_result), errorMessage, null);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using {@code getAddressLine},
            // join them, and send them to the thread. The {@link android.location.address}
            // class provides other options for fetching address details that you may prefer
            // to use. Here are some examples:
            // getLocality() ("Mountain View", for example)
            // getAdminArea() ("CA", for example)
            // getPostalCode() ("94043", for example)
            // getCountryCode() ("US", for example)
            // getCountryName() ("United States", for example)
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(FetchAddressIntentService.class.getSimpleName(), getString(R.string.address_found));
            deliverResultToReceiver(getResources().getInteger(R.integer.fetch_address_success_result),
                    TextUtils.join(System.getProperty("line.separator"), addressFragments), address);
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


}
