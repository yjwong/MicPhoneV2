package sg.edu.nus.micphone2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;


public class ManualPairingActivity extends ActionBarActivity {
    private final static String TAG = "ManualPairingActivity";

    private EditText mIpAddressField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_pairing);

        // Obtain the form details.
        mIpAddressField = (EditText) findViewById(R.id.manual_pairing_ip_address);
        Button pairButton = (Button) findViewById(R.id.manual_pairing_pair_btn);
        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = mIpAddressField.getText().toString();

                // Check if data scanned is a valid IP address.
                Matcher matcher = Patterns.IP_ADDRESS.matcher(data);
                if (matcher.matches()) {
                    Intent intent = new Intent(ManualPairingActivity.this, MicActivity.class);
                    intent.putExtra(MicActivity.I_NEED_IP, data);
                    startActivity(intent);
                } else {
                    Toast.makeText(
                            ManualPairingActivity.this,
                            getString(R.string.manual_pairing_not_an_ip_address),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
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
}
