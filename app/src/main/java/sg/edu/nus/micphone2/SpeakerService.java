package sg.edu.nus.micphone2;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class SpeakerService extends IntentService {

    private final static int PORT = 65530;
    private final static String TAG = "SpeakerService";
    private boolean mRunning = true;
    private AudioGroup mSpeaker;

    public SpeakerService() {
        super(TAG);
        mSpeaker = new AudioGroup();
        mSpeaker.setMode(AudioGroup.MODE_MUTED);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent){
        try(ServerSocket ss = new ServerSocket(PORT, 50, this.getLocalAddress())){

            Log.d(TAG, "Listening for Server on \n IP: " +
                    ss.getInetAddress().getHostAddress() +
                    "\n PORT:  " + PORT );

            while(mRunning) {
                Socket incoming = ss.accept();
                NetworkTask netTask = new NetworkTask(ss.getInetAddress());
                netTask.doInBackground(incoming);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mRunning = false;
        super.onDestroy();
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

    private class NetworkTask extends AsyncTask<Socket,Void,Void>{
        private final static String TAG = "NetworkTask";
        private InetAddress mSpeakerAddress;

        NetworkTask(InetAddress speakerAddress){
            this.mSpeakerAddress = speakerAddress;
        }

        @Override
        protected Void doInBackground(Socket... clientSoc) {
            try {
                InputStream inputStream = clientSoc[0].getInputStream();
                BufferedReader inputReader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String input = inputReader.readLine();
                Log.v(TAG, "Received from client :" + input);

                InetAddress micAddress = clientSoc[0].getInetAddress();
                int micPort = Integer.parseInt(input);

                AudioStream audioStream = new AudioStream(mSpeakerAddress);
                int speakerPort = audioStream.getLocalPort();

                OutputStream outputStream = clientSoc[0].getOutputStream();
                BufferedWriter outputWriter = new BufferedWriter(
                        new OutputStreamWriter(outputStream));
                outputWriter.write(Integer.toString(speakerPort) + "\n");
                outputWriter.flush();
                Log.v(TAG, "Wrote to client :" + speakerPort);

                clientSoc[0].close();

                // Associate with client RTP endpoint.
                audioStream.setMode(AudioStream.MODE_RECEIVE_ONLY);
                audioStream.associate(micAddress, micPort);
                audioStream.join(mSpeaker);

                // Print debug information about group.
                Log.d(TAG, "Speaker address: " + audioStream.getLocalAddress() + ":"
                        + audioStream.getLocalPort());
                Log.d(TAG, "Mic address: " + audioStream.getRemoteAddress()
                        + ":" + audioStream.getRemotePort());

            } catch (IOException ioe) {
                Log.e(TAG, "Error in socket" + ioe.getLocalizedMessage());
            }
            return null;
        }
    }
}
