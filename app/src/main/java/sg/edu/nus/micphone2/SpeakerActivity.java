package sg.edu.nus.micphone2;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


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

    private InetAddress getLocalAddress()  {
        Log.d(TAG, "Getting Local Address");
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        for (Method method : wifiManager.getClass().getMethods()){
            if (method.getName().equalsIgnoreCase("IsWifiAPEnabled")){
                try {
                    InetAddress ipAddress = null;
                    if ((boolean) method.invoke(wifiManager)) {
                        // Hardcoded Value in Android "192.168.43.1."
                        try {
                            ipAddress = InetAddress.getByName("192.168.43.1");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                    } else {
                        // We need to obtain the address to broadcast on this interface.
                        int ipAddressInt = wifiManager.getConnectionInfo().getIpAddress();
                        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                            ipAddressInt = Integer.reverseBytes(ipAddressInt);
                        }

                        byte[] ipByteArray = BigInteger.valueOf(ipAddressInt).toByteArray();
                        try {
                            ipAddress = InetAddress.getByAddress(ipByteArray);
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "Unable to get host address");
                            ipAddress = null;
                        }
                    }

                    return ipAddress;

                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private class GetLocalAddressTask extends AsyncTask<Void, Void, InetAddress> {
        private final static String TAG = "GetLocalAddressTask";

        @Override
        protected InetAddress doInBackground(Void... params) {
            return getLocalAddress();
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
