package br.gbizotto.geolocation;

import android.Manifest;
import android.content.Intent;
import android.location.Address;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import java.util.ArrayList;

import br.gbizotto.geolocation.service.LocationService;
import br.gbizotto.geolocation.utils.PermissionsUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements AddressReceiver {

    private static final int PERMISSION_LOCATION = 1;

    @BindView(R.id.txtZipCode)
    TextView mTxtZipCode;
    @BindView(R.id.txtAddress)
    TextView mTxtAddress;

    private AddressResultReceiver mResultReceiver;

    private Intent mIntentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mResultReceiver = new AddressResultReceiver(new Handler(), this, this);
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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            startIntentService();
        }
    }

    private void startIntentService() {
        mIntentService = new Intent(this, LocationService.class);
        mIntentService.putExtra(getString(R.string.fetch_address_receiver), mResultReceiver);
        startService(mIntentService);
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput(Address address) {
        mTxtZipCode.setText(address.getPostalCode());

        ArrayList<String> addressFragments = new ArrayList<>();

        for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
            addressFragments.add(address.getAddressLine(i));
        }

        mTxtAddress.setText(TextUtils.join(System.getProperty("line.separator"), addressFragments));

        if (mIntentService != null) {
            stopService(mIntentService);
        }
    }

    @Override
    public void receiveAddress(Address address) {
        displayAddressOutput(address);
    }
}
