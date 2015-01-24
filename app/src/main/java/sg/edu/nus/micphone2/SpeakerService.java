package sg.edu.nus.micphone2;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class SpeakerService extends IntentService {

    public final static String LOCAL_IP_ADDRESS = "LOCAL_IP_ADDRESS";
    private final static int PORT = 65530;
    private final static String TAG = "SpeakerService";
    private boolean mRunning = true;
    private AudioGroup mSpeaker;
    private ArrayList<Thread> mStreams;

    public SpeakerService() {
        super(TAG);
        mSpeaker = new AudioGroup();
        mSpeaker.setMode(AudioGroup.MODE_MUTED);
        mStreams = new ArrayList<Thread>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent){
        // Obtain the IP address from the intent.
        InetAddress localAddress = (InetAddress) intent.getSerializableExtra(LOCAL_IP_ADDRESS);
        if (localAddress == null) {
            Log.e(TAG, "onHandleIntent: localAddress was null");
            return;
        }

        try (ServerSocket ss = new ServerSocket(PORT, 50, localAddress)){
            Log.d(TAG, "Listening for Server on \n IP: " +
                    ss.getInetAddress().getHostAddress() +
                    "\n PORT:  " + PORT );

            while(mRunning) {
                Socket incoming = ss.accept();
                //TODO Commented Code to be tested after Integration
                //Thread micStream = new MicStream(ss.getInetAddress(), incoming);
                //micStream.start();
                //mStreams.add(micStream);
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
        for(Thread t : mStreams){
            t.interrupt();
        }
        super.onDestroy();
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
    private class MicStream extends Thread{
        private final static String TAG = "MicStream";
        private final static int STREAM_TYPE = AudioManager.STREAM_MUSIC;
        private final static int SAMPLING_RATE = 44100;
        private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
        private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
        private final static int MODE = AudioTrack.MODE_STREAM;
        private InetAddress mSpeakerAddress;
        private DatagramSocket mAudioStream;
        private int mBufferSize = 1000; //8k?

        MicStream(InetAddress speakerAddress, Socket clientSoc){
            this.mSpeakerAddress = speakerAddress;
            establishConnection(clientSoc);
            mBufferSize = AudioTrack.getMinBufferSize
                    (SAMPLING_RATE,CHANNEL_CONFIG,AUDIO_FORMAT)*2;
        }

        @Override
        public void run(){

            while(!this.isInterrupted()){
                try {
                    byte[] buffer = new byte[mBufferSize];
                    DatagramPacket datagram = new DatagramPacket(buffer, mBufferSize);
                    Log.d(TAG, "Waiting for Voice Data from Client" );
                    mAudioStream.receive(datagram);
                    AudioTrack speakerTrack = new AudioTrack(STREAM_TYPE,
                            SAMPLING_RATE,CHANNEL_CONFIG,AUDIO_FORMAT,mBufferSize,MODE);
                    int numByteWrote = speakerTrack.write(datagram.getData(),0, datagram.getLength());
                    Log.d(TAG, "Wrote " +numByteWrote +" to Track");
                    speakerTrack.play();

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        private void establishConnection(Socket clientSoc){
            try {
                this.mAudioStream = new DatagramSocket();

                AudioStream audioStream = new AudioStream(mSpeakerAddress);
                int speakerPort = audioStream.getLocalPort();

                OutputStream outputStream = clientSoc.getOutputStream();
                BufferedWriter outputWriter = new BufferedWriter(
                        new OutputStreamWriter(outputStream));
                outputWriter.write(Integer.toString(speakerPort) + "\n");
                outputWriter.flush();
                Log.v(TAG, "Wrote to client :" + speakerPort);

                clientSoc.close();
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }
}
