package sg.edu.nus.micphone2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Flyer on 24/1/15.
 */
public class VerifyDisconnectFragment extends DialogFragment{
    @Override
    public Dialog onCreateDialog (Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sure or not?")
               .setMessage(R.string.mic_verify_disconnect)
               .setPositiveButton(R.string.mic_verify_yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                    // Confirm Disconnect
                    }
                })
                .setNegativeButton(R.string.mic_verify_no, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // Cancel Disconnect
                    }
                });

        return builder.create();
    }
}
