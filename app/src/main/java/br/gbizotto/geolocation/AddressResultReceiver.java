package br.gbizotto.geolocation;

import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by Gabriela on 15/03/2017.
 */

public class AddressResultReceiver extends ResultReceiver {

    private final Context mContext;

    private final AddressReceiver mAddressReceiver;

    public AddressResultReceiver(Handler handler, Context context, AddressReceiver addressReceiver) {
        super(handler);
        this.mContext = context;
        this.mAddressReceiver = addressReceiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData.containsKey(mContext.getString(R.string.fetch_address_result_address))) {
            Address address = resultData.getParcelable(mContext.getString(R.string.fetch_address_result_address));

            if (address != null) {
                mAddressReceiver.receiveAddress(address);
            }
        }
    }
}
