package com.xanderfehsenfeld.swearjar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import cz.msebera.android.httpclient.Header;

// Much of this code was pulled in pieces from the Braintree how to page, then specific implementation
// for this project was added, especially for the charities.

public class CharityActivity extends AppCompatActivity {

    public static final int PAYMENT_REQUEST = 3500;
    private static final String DEBUG = "DEBUG";
    private static final String BRAINTREE_URL = "http://172.23.16.99:8080";

    int swearCountMultiplier = 1;
    float swearCost = 0;
    String clientToken;
    AsyncHttpClient client = new AsyncHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charity);

        getClientToken();

        TextView countView = (TextView) findViewById(R.id.count);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int amount  =  extras.getInt(MainActivity.SWEAR_COUNT);
            countView.setText("$" + amount + ".00");
        }
    }

    public void onBraintreeSubmit(View v) {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(clientToken);
        startActivityForResult(paymentRequest.getIntent(this), PAYMENT_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PAYMENT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentMethodNonce paymentMethodNonce = data.getParcelableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE
                );
                String nonce = paymentMethodNonce.getNonce();
                float amount = swearCost * swearCountMultiplier;
                Spinner charitySpinner = (Spinner) findViewById(R.id.spinner_charity);
                String charity = charitySpinner.getSelectedItem().toString();

                postToServer(nonce, amount);
            }
        }
    }

    private void postToServer(String nonce, float amount) {
        RequestParams params = new RequestParams();
        params.put("payment_method_nonce", nonce);
        params.put("amount", amount);
        client.post(BRAINTREE_URL + "/checkout", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Toast t = Toast.makeText(getApplicationContext(), "checkout successful", Toast.LENGTH_LONG);
                        t.show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                }
        );
    }
    public void getClientToken() {
        client.get(BRAINTREE_URL +"/client_token", new TextHttpResponseHandler() {

            @Override
            public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                Log.d(DEBUG, "Failed to get the client token");
            }

            @Override
            public void onSuccess(int i, Header[] headers, String s) {
                Toast t = Toast.makeText(getApplicationContext(), "clientToken Recieved: " + s, Toast.LENGTH_LONG );
                t.show();
                clientToken = s;

            }
        });
    }
}
