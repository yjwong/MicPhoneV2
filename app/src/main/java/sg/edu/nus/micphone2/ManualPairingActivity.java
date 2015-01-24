package sg.edu.nus.micphone2;

import android.content.Context;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


public class ManualPairingActivity extends ActionBarActivity {
    private final static String TAG = "ManualPairingActivity";
    private final static int PORT = 65530;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_pairing);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manual_pairing, menu);
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
        WifiManager wifiManager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        for(Method method : wifiManager.getClass().getMethods()){
            if(method.getName().equalsIgnoreCase("IsWifiAPEnabled")){
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
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    private void clientConnection(InetAddress speakerAddress) {
        Log.d(TAG, "beginAudioStream to " + speakerAddress + ":" + PORT);
        try {
            AudioStream micStream = new AudioStream(this.getLocalAddress());
            int localPort = micStream.getLocalPort();

            try {
                // Negotiate the RTP endpoints of the server.
                Socket socket = new Socket(speakerAddress, PORT);
                OutputStream outputStream = socket.getOutputStream();
                BufferedWriter outputWriter = new BufferedWriter(
                        new OutputStreamWriter(outputStream));
                outputWriter.write(Integer.toString(localPort) + "\n");
                Log.v(TAG, "Wrote Port " + localPort + " to Speaker." );
                outputWriter.flush();

                InputStream inputStream = socket.getInputStream();
                BufferedReader inputReader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String input = inputReader.readLine();
                Log.v(TAG, "Read " + input + " from Speaker." );
                int speakerPort = Integer.parseInt(input);
                socket.close();

                // Associate with server RTP endpoint.
                AudioGroup streamGroup = new AudioGroup();
                // streamGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
                streamGroup.setMode(AudioGroup.MODE_NORMAL);

                micStream.setMode(AudioStream.MODE_SEND_ONLY);
                micStream.associate(speakerAddress, speakerPort);
                micStream.join(streamGroup);

                // Print debug information about group.
                Log.d(TAG, "Local address: " + micStream.getLocalAddress() + ":"
                        + micStream.getLocalPort());
                Log.d(TAG, "Remote address: " + micStream.getRemoteAddress() + ":"
                        + micStream.getRemotePort());

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
