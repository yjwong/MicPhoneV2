package sg.edu.nus.micphone2;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.net.InetAddress;


public class SpeakerActivity extends ActionBarActivity {
    private final static String TAG = "SpeakerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);

        // Start the speaker service.
        GetLocalAddressTask localAddressTask = new GetLocalAddressTask();
        localAddressTask.execute();
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
        }
    }
}
