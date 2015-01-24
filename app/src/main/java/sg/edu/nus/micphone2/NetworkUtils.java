package sg.edu.nus.micphone2;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

/**
 * Common network utilities used by both microphone and speaker.
 */
public class NetworkUtils {
    private final static String TAG = "NetworkUtils";

    /**
     * Obtains the local IP address of the phone.
     */
    public static InetAddress getLocalAddress(Context context)  {
        Log.d(TAG, "Getting Local Address");
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

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
}
