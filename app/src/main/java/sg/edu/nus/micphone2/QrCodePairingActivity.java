package sg.edu.nus.micphone2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.regex.Matcher;

import eu.livotov.zxscan.ScannerView;


public class QrCodePairingActivity extends ActionBarActivity {

    private ScannerView mQrScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_pairing);

        // We're interested to know when a QR code is scanned.
        mQrScanner = (ScannerView) findViewById(R.id.qr_scanner);
        mQrScanner.setScannerViewEventListener(new MyScannerViewEventListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQrScanner.startScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mQrScanner.stopScanner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_qr_code_pairing, menu);
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

    private class MyScannerViewEventListener implements ScannerView.ScannerViewEventListener {
        @Override
        public boolean onCodeScanned(String data) {
            mQrScanner.stopScanner();

            // Check if data scanned is a valid IP address.
            Matcher matcher = Patterns.IP_ADDRESS.matcher(data);
            if (matcher.matches()) {
                Intent intent = new Intent(QrCodePairingActivity.this, MicActivity.class);
                intent.putExtra(MicActivity.I_NEED_IP, data);
                startActivity(intent);
            } else {
                Toast.makeText(
                        QrCodePairingActivity.this,
                        getString(R.string.qr_code_not_an_ip_address),
                        Toast.LENGTH_SHORT).show();
                mQrScanner.startScanner();
            }

            return true;
        }
    }
}
