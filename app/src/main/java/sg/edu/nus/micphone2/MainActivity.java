package sg.edu.nus.micphone2;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Magic, Do not touch
        Intent intent;
        switch(item.getItemId())
        {

            case R.id.action_settings:
                openSettings();
                return true;

            case R.id.action_about:
                openAbout();
                return true;

            // STUB
            // TO BE REMOVE BEFORE RELEASE
            case R.id.action_mic:
                openMic();
                return true;

            case R.id.action_pairing:
                intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_start_speaker:
                intent = new Intent(this, SpeakerActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openSettings()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openAbout()
    {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    // STUB
    // TO BE REMOVE BEFORE RELEASE
    public void openMic()
    {
        Intent intent = new Intent(this, MicActivity.class);
        startActivity(intent);
    }
}
