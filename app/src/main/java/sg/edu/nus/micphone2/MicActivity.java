package sg.edu.nus.micphone2;

import android.app.FragmentManager;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class MicActivity extends ActionBarActivity {

    private static final String TAG = "MicActivity";
    private static final int PORT = 65530;
    public static final String I_NEED_IP = "I_NEED_IP";
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLING_RATE = 44100;
    public static final int CHANNEL_CONFIG =  AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private static final AudioCodec CODEC = AudioCodec.GSM_EFR;

    FragmentManager fragmentManager = getFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Creating Mic Activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            // Check if the IP is available via NFC.
            Intent intent = getIntent();
            InetAddress speakerAddress = null;
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage message = (NdefMessage) rawMessages[0];
                speakerAddress = InetAddress.getByAddress(message.getRecords()[0].getPayload());
            } else {
                String inetIP = intent.getStringExtra(I_NEED_IP);
                speakerAddress = InetAddress.getByName(inetIP);
            }

            if (speakerAddress == null) {
                Log.e(TAG, "speakerAddress is null");
            } else {
                MicrophoneTask microphoneTask = new MicrophoneTask();
                microphoneTask.execute(speakerAddress);
            }

        } catch (IOException ioe) {
            Log.e(TAG, "Error in creating speakerAddress : " + ioe.getMessage());
        }

        // Sample rate 44100Hz

    }

    /***
     *
     * Magic From Yong Jie
     */
    private void clientConnection(InetAddress speakerAddress) {
        Log.d(TAG, "beginAudioStream to " + speakerAddress + ":" + PORT);
        try {
            AudioStream micStream = new AudioStream(NetworkUtils.getLocalAddress(this));
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

                micStream.setCodec(CODEC);
                micStream.setMode(AudioStream.MODE_SEND_ONLY);
                micStream.associate(speakerAddress, speakerPort);
                micStream.join(streamGroup);    //To leave audioStream, micStream.join(null);

                // Print debug information about group.
                Log.d(TAG, "Local address: " + micStream.getLocalAddress() + ":"
                        + micStream.getLocalPort());
                Log.d(TAG, "Remote address: " + micStream.getRemoteAddress() + ":"
                        + micStream.getRemotePort());

            } catch (IOException ioe) {
                Log.e(TAG, "IOException at clientConnection " + ioe.getMessage());
            }
        } catch (IOException ioe) {
            Log.e(TAG, "IOException at AudioStream Creation");
        }
    }

    // Magic!!!!! DO NOT TOUCH!!!!!!
    public void verifyDisconnect(View view)
    {
        Button confirmDisconnect = (Button) findViewById(R.id.verifyDisconnect);
        confirmDisconnect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                VerifyDisconnectFragment verifyDisconnectFragment = new VerifyDisconnectFragment();
                verifyDisconnectFragment.show(fragmentManager, "AH HA HA HA");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Magic, Do not touch
        switch(item.getItemId())
        {

            case R.id.action_settings:
                OpenSetting();
                return true;

            case R.id.action_about:
                OpenAbout();
                return true;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void OpenSetting()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void OpenAbout()
    {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private class MicrophoneTask extends AsyncTask<InetAddress, Void, Void> {
        @Override
        protected Void doInBackground(InetAddress... params) {
            if (BuildConfig.DEBUG && params.length != 1) {
                throw new AssertionError("MicrophoneTask only accepts 1 parameter!");
            }

            clientConnection(params[0]);
            return null;
        }
    }

    private class MicrophoneStream extends AsyncTask<InetAddress, Void, Void> {
        private final static String TAG = "MicrophoneStream";

        @Override
        protected Void doInBackground(InetAddress... params) {
            if (BuildConfig.DEBUG && params.length != 1) {
                throw new AssertionError("MicrophoneTask only accepts 1 parameter!");
            }
            try {
                // Getting UDP Port from Speaker Via Control Channel
                Socket socket = new Socket(params[0], PORT);
                InputStream inputStream = socket.getInputStream();
                BufferedReader inputReader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String input = inputReader.readLine();
                Log.v(TAG, "Read " + input + " from Speaker.");
                int speakerPort = Integer.parseInt(input);
                socket.close();

                DatagramSocket datagramSocket = new DatagramSocket();
                datagramSocket.connect(params[0], speakerPort);
                Log.v(TAG, "Established UDP Connection to " + params[0] + " : " + speakerPort);

                while (true) {
                    DatagramPacket datagramPacket = null;

                    int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                    Log.d(TAG, "BufferSize : " + bufferSize);
                    ByteBuffer audioBuffer = ByteBuffer.allocate(bufferSize);

                    AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE,
                            SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

                    //TODO WHAT IS THIS FOR??
                    int size = audioRecord.read(audioBuffer, bufferSize);

                    // Conversion to byte array to be sent
                    byte[] bytes = new byte[audioBuffer.capacity()];
                    audioBuffer.get(bytes, 0, bytes.length);
                    Log.d(TAG, "Sending UDP Packet with payload : " + bytes.length + " bytes");
                    datagramPacket = new DatagramPacket(bytes, bytes.length);
                    datagramSocket.send(datagramPacket);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Error in sockets " + ioe.getMessage());
            }
            return null;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        //micStream.join(streamGroup);
       finish();
    }

}
