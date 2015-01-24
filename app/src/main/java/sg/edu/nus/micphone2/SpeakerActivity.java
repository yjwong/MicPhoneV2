package sg.edu.nus.micphone2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;


public class SpeakerActivity extends ActionBarActivity implements NfcAdapter.CreateNdefMessageCallback {
    private final static String TAG = "SpeakerActivity";
    private final static String STATE_KEY_LOADING = "loading";
    private final static String STATE_KEY_LOADED = "loaded";
    private final static String STATE_KEY_LOCAL_ADDRESS = "localAddress";
    private final static String STATE_KEY_LOCAL_ADDRESS_AVAILABLE = "localAddressAvailable";

    private ProgressDialog mLoadingDialog;
    private boolean mLoading;
    private boolean mLoaded;
    private boolean mLocalAddressAvailable;
    private InetAddress mLocalAddress;
    private NfcAdapter mNfcAdapter;
    private AlertDialog mLocalAddressUnavailableDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);

        // Restore state.
        if (savedInstanceState != null) {
            mLoading = savedInstanceState.getBoolean(STATE_KEY_LOADING);
            mLoaded = savedInstanceState.getBoolean(STATE_KEY_LOADED);
            mLocalAddress = (InetAddress) savedInstanceState.getSerializable(STATE_KEY_LOCAL_ADDRESS);
            mLocalAddressAvailable = savedInstanceState.getBoolean(STATE_KEY_LOCAL_ADDRESS_AVAILABLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start the speaker service.
        if (!mLoading && !mLoaded) {
            GetLocalAddressTask localAddressTask = new GetLocalAddressTask();
            localAddressTask.execute();
            mLoading = true;
        }

        // Create a progress dialog while the service is brought up.
        if (mLoading) {
            mLoadingDialog = ProgressDialog.show(this, "", getString(R.string.speaker_starting_speaker), true);
        }

        // If we already have the address, just populate everything.
        if (mLoaded) {
            if (mLocalAddressAvailable) {
                onLocalAddressAvailable();
            } else {
                onLocalAddressUnavailable();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_LOADING, mLoading);
        outState.putBoolean(STATE_KEY_LOADED, mLoaded);
        outState.putSerializable(STATE_KEY_LOCAL_ADDRESS, mLocalAddress);
        outState.putBoolean(STATE_KEY_LOCAL_ADDRESS_AVAILABLE, mLocalAddressAvailable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }

        if (mLocalAddressUnavailableDialog != null) {
            mLocalAddressUnavailableDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_speaker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the local address is available.
     */
    private void onLocalAddressAvailable() {
        Log.d(TAG, "onLocalAddressAvailable");

        // Display speaker IP address.
        TextView ipAddressView = (TextView) findViewById(R.id.speaker_ip_address);
        ipAddressView.setText(mLocalAddress.toString());

        // Advertise ourselves via NFC.
        Log.d(TAG, "Advertising via NFC");
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, getString(R.string.speaker_nfc_not_available), Toast.LENGTH_SHORT).show();
        } else {
            mNfcAdapter.setNdefPushMessageCallback(this, this);
        }
    }

    /**
     * Called when the local address is unavailable.
     * Probably happens when WiFi is not available.
     */
    private void onLocalAddressUnavailable() {
        Log.d(TAG, "onLocalAddressUnavailable");

        // Display a message asking the user to enable WiFi.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speaker_network_unavailable)
                .setMessage(R.string.speaker_connect_to_wifi)
                .setPositiveButton(R.string.speaker_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Reset the variables, in case we decide to come back.
                        mLoading = false;
                        mLoaded = false;
                        mLocalAddressAvailable = false;
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        mLocalAddressUnavailableDialog = builder.create();
        mLocalAddressUnavailableDialog.show();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        Log.d(TAG, "createNdefMessage: " + event);
        return new NdefMessage(
                new NdefRecord[]{
                        NdefRecord.createMime("application/vnd.sg.edu.nus.micphone2", mLocalAddress.getAddress())
                }
        );
    }

    private class GetLocalAddressTask extends AsyncTask<Void, Void, InetAddress> {
        private final static String TAG = "GetLocalAddressTask";

        @Override
        protected InetAddress doInBackground(Void... params) {
            return NetworkUtils.getLocalAddress(SpeakerActivity.this);
        }

        @Override
        protected void onPostExecute(InetAddress address) {
            Log.v(TAG, "onPostExecute: " + address);
            Intent intent = new Intent(SpeakerActivity.this, SpeakerService.class);
            intent.putExtra(SpeakerService.LOCAL_IP_ADDRESS, address);
            startService(intent);

            // Save the local address.
            mLocalAddress = address;
            if (mLocalAddress == null) {
                onLocalAddressUnavailable();
            } else {
                mLocalAddressAvailable = true;
                onLocalAddressAvailable();
            }

            // Hide the loading dialog.
            mLoading = false;
            mLoaded = true;
            mLoadingDialog.dismiss();
        }
    }
}
