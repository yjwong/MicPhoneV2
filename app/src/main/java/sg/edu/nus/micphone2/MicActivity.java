package sg.edu.nus.micphone2;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class MicActivity extends ActionBarActivity {
    private static final String TAG = "MicActivity";
    private static final int PORT = 65530;
    public static final String I_NEED_IP = "I_NEED_IP";
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLING_RATE = 44100;
    public static final int CHANNEL_CONFIG =  AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private static final AudioCodec CODEC = AudioCodec.GSM_EFR;
    private static final int SOCKET_TIMEOUT = 2500;

    private static final String STATE_KEY_LOADING = "loading";
    private static final String STATE_KEY_LOADED = "loaded";
    private static final String STATE_KEY_SPEAKER_CONNECTED = "speakerConnected";

    private boolean mLoading;
    private boolean mLoaded;
    private boolean mSpeakerConnected;
    private ProgressDialog mLoadingDialog;
    private AlertDialog mConnectionFailedDialog;
    private AudioStream mMicStream;
    private AudioGroup mMicStreamGroup;

    FragmentManager fragmentManager = getFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Creating Mic Activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic);

        // Restore state.
        if (savedInstanceState != null) {
            mLoading = savedInstanceState.getBoolean(STATE_KEY_LOADING);
            mLoaded = savedInstanceState.getBoolean(STATE_KEY_LOADED);
            mSpeakerConnected = savedInstanceState.getBoolean(STATE_KEY_SPEAKER_CONNECTED);
        }

        // Start the microphone service.
        if (!mLoading && !mLoaded) {
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

            mLoading = true;
        }

        // Create a progress dialog while the service is brought up.
        if (mLoading) {
            mLoadingDialog = ProgressDialog.show(this, "", getString(R.string.mic_connecting), true);
        }

        // Set up button events.
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_LOADING, mLoading);
        outState.putBoolean(STATE_KEY_LOADED, mLoaded);
        outState.putBoolean(STATE_KEY_SPEAKER_CONNECTED, mSpeakerConnected);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Join the stream when we're back.
        if (mMicStream != null && mMicStreamGroup != null) {
            mMicStream.join(mMicStreamGroup);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Leave the stream when we're out.
        if (mMicStream != null) {
            mMicStream.join(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }

        if (mConnectionFailedDialog != null) {
            mConnectionFailedDialog.dismiss();
        }

        if (mMicStream != null) {
            mMicStream.join(null);
        }
    }

    private void onConnectFailed() {
        Log.d(TAG, "onConnectFailed");
        mLoadingDialog.dismiss();
        mLoading = false;
        mLoaded = true;

        // Display a message to tell the user that connection failed.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mic_connection_failed)
                .setMessage(R.string.mic_check_connection)
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        mConnectionFailedDialog = builder.create();
        mConnectionFailedDialog.show();
    }

    private void onConnectedToSpeaker() {
        Log.d(TAG, "onConnectedToSpeaker");
        mLoadingDialog.dismiss();
        mLoading = false;
        mLoaded = true;
        mSpeakerConnected = true;
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

    private class MicrophoneTask extends AsyncTask<InetAddress, Void, Boolean> {
        @Override
        protected Boolean doInBackground(InetAddress... params) {
            if (BuildConfig.DEBUG && params.length != 1) {
                throw new AssertionError("MicrophoneTask only accepts 1 parameter!");
            }

            return clientConnection(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                onConnectedToSpeaker();
            } else {
                onConnectFailed();
            }
        }

        /***
         * Magic From Yong Jie
         */
        private boolean clientConnection(InetAddress speakerAddress) {
            Log.d(TAG, "beginAudioStream to " + speakerAddress + ":" + PORT);
            try {
                mMicStream = new AudioStream(NetworkUtils.getLocalAddress(MicActivity.this));
                int localPort = mMicStream.getLocalPort();

                try {
                    // Negotiate the RTP endpoints of the server.
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(speakerAddress, PORT), SOCKET_TIMEOUT);

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
                    mMicStreamGroup = new AudioGroup();
                    // streamGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
                    mMicStreamGroup.setMode(AudioGroup.MODE_NORMAL);

                    mMicStream.setCodec(CODEC);
                    mMicStream.setMode(AudioStream.MODE_SEND_ONLY);
                    mMicStream.associate(speakerAddress, speakerPort);
                    mMicStream.join(mMicStreamGroup);    //To leave audioStream, micStream.join(null);

                    // Print debug information about group.
                    Log.d(TAG, "Local address: " + mMicStream.getLocalAddress() + ":"
                            + mMicStream.getLocalPort());
                    Log.d(TAG, "Remote address: " + mMicStream.getRemoteAddress() + ":"
                            + mMicStream.getRemotePort());

                    // Everything's set up at this point.
                    return true;

                } catch (IOException ioe) {
                    Log.e(TAG, "IOException at clientConnection " + ioe.getMessage());
                }

            } catch (IOException ioe) {
                Log.e(TAG, "IOException at AudioStream Creation");

            }

            return false;
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

}
