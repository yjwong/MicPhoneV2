package sg.edu.nus.micphone2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.net.InetAddress;


public class SpeakerActivity extends ActionBarActivity {
    private final static String TAG = "SpeakerActivity";
    private final static String STATE_KEY_LOADING = "loading";
    private final static String STATE_KEY_LOADED = "loaded";
    private final static String STATE_KEY_LOCAL_ADDRESS = "localAddress";

    private ProgressDialog mLoadingDialog;
    private boolean mLoading;
    private boolean mLoaded;
    private InetAddress mLocalAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);

        // Restore state.
        if (savedInstanceState != null) {
            mLoading = savedInstanceState.getBoolean(STATE_KEY_LOADING);
            mLoaded = savedInstanceState.getBoolean(STATE_KEY_LOADED);
            mLocalAddress = (InetAddress) savedInstanceState.getSerializable(STATE_KEY_LOCAL_ADDRESS);
        }

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
            onLocalAddressAvailable();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_LOADING, mLoading);
        outState.putBoolean(STATE_KEY_LOADED, mLoaded);
        outState.putSerializable(STATE_KEY_LOCAL_ADDRESS, mLocalAddress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
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
            onLocalAddressAvailable();

            // Hide the loading dialog.
            mLoading = false;
            mLoaded = true;
            mLoadingDialog.dismiss();
        }
    }
}
