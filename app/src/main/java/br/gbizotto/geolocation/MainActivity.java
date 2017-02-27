package br.gbizotto.geolocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

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
    private Address mAddress;

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
            startIntentService();
        }
    }

    private boolean hasLocationPermission() {
        if (!PermissionsUtils.checkLocationPermission(this)) {
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
            startIntentService();
        }
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra(getString(R.string.fetch_address_receiver), mResultReceiver);
        startService(intent);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(getString(R.string.fetch_address_result_data));

            if (resultData.containsKey(getString(R.string.fetch_address_result_addres))) {
                mAddress = (Address) resultData.getParcelable(getString(R.string.fetch_address_result_addres));

                if (mAddress != null) {
                    displayAddressOutput();
                    mAddressRequested = false;
                }
            }

            // Show a toast message if an address was found.
            if (resultCode == getResources().getInteger(R.integer.fetch_address_failure_result)) {
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
