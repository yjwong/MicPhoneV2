package sg.edu.nus.micphone2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;


public class PairingActivity extends ActionBarActivity {
    private final static String TAG = "PairingActivity";
    private final static int FRAGMENT_NFC_INFO_POSITION = 0;
    private final static int FRAGMENT_QR_CODE_INFO_POSITION = 1;
    private final static int FRAGMENT_MANUAL_POSITION = 2;
    private final static int FRAGMENT_COUNT = 3;

    private ViewPager mViewPager;
    private FragmentPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        // Initialize the ViewPager.
        mPagerAdapter = new PairingPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pairing_pager);
        mViewPager.setAdapter(mPagerAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pairing, menu);
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

    private class PairingPagerAdapter extends FragmentPagerAdapter {
        private final static String TAG = "PairingPagerAdapter";

        public PairingPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case FRAGMENT_NFC_INFO_POSITION:
                    fragment = NfcInfoFragment.newInstance(PairingActivity.this);
                    break;
                case FRAGMENT_QR_CODE_INFO_POSITION:
                    fragment = QrCodeInfoFragment.newInstance(PairingActivity.this);
                    break;
                case FRAGMENT_MANUAL_POSITION:
                    fragment = ManualInfoFragment.newInstance(PairingActivity.this);
                    break;
                default:
                    Log.w(TAG, "Invalid fragment position requested: " + position);
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = null;
            switch (position) {
                case FRAGMENT_NFC_INFO_POSITION:
                    title = getString(R.string.pairing_title_nfc);
                    break;
                case FRAGMENT_QR_CODE_INFO_POSITION:
                    title = getString(R.string.pairing_title_qr_code);
                    break;
                case FRAGMENT_MANUAL_POSITION:
                    title = getString(R.string.pairing_title_manual);
                    break;
                default:
                    Log.w(TAG, "Invalid fragment position requested: " + position);
            }

            return title;
        }
    }

    public static class NfcInfoFragment extends Fragment {
        private Context mContext;

        public static NfcInfoFragment newInstance(Context context) {
            NfcInfoFragment fragment = new NfcInfoFragment();
            fragment.mContext = context;
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pairing_nfc_info, container, false);
            return rootView;
        }
    }

    public static class QrCodeInfoFragment extends Fragment {
        private Context mContext;

        public static QrCodeInfoFragment newInstance(Context context) {
            QrCodeInfoFragment fragment = new QrCodeInfoFragment();
            fragment.mContext = context;
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pairing_qr_code_info, container, false);
            Button pairButton = (Button) rootView.findViewById(R.id.pairing_choice_qr_code);
            pairButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, QrCodePairingActivity.class);
                    startActivity(intent);
                }
            });

            return rootView;
        }
    }

    public static class ManualInfoFragment extends Fragment {
        private Context mContext;
        private EditText mIpAddressField;

        public static ManualInfoFragment newInstance(Context context) {
            ManualInfoFragment fragment = new ManualInfoFragment();
            fragment.mContext = context;
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pairing_manual_info, container, false);

            // Obtain the form details.
            mIpAddressField = (EditText) rootView.findViewById(R.id.manual_pairing_ip_address);
            Button pairButton = (Button) rootView.findViewById(R.id.manual_pairing_pair_btn);
            pairButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String data = mIpAddressField.getText().toString();

                    // Check if data scanned is a valid IP address.
                    Matcher matcher = Patterns.IP_ADDRESS.matcher(data);
                    if (matcher.matches()) {
                        Intent intent = new Intent(mContext, MicActivity.class);
                        intent.putExtra(MicActivity.I_NEED_IP, data);
                        startActivity(intent);
                    } else {
                        Toast.makeText(
                                mContext,
                                getString(R.string.manual_pairing_not_an_ip_address),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            return rootView;
        }
    }
}
