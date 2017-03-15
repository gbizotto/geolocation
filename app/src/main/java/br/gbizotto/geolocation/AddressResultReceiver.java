package br.gbizotto.geolocation;

import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

/**
 * Created by Gabriela on 15/03/2017.
 */

public class AddressResultReceiver extends ResultReceiver {

    private Context mContext;

    private AddressReceiver mAddressReceiver;

    public AddressResultReceiver(Handler handler, Context context, AddressReceiver addressReceiver) {
        super(handler);
        this.mContext = context;
        this.mAddressReceiver = addressReceiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData.containsKey(mContext.getString(R.string.fetch_address_result_addres))) {
            Address address = (Address) resultData.getParcelable(mContext.getString(R.string.fetch_address_result_addres));

            if (address != null) {
                mAddressReceiver.receiveAddress(address);
            }
        }
    }
}
