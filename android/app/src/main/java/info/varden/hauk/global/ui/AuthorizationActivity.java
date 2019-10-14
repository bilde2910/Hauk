package info.varden.hauk.global.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import info.varden.hauk.Constants;
import info.varden.hauk.R;

/**
 * Activity that is displayed to prompt the user to authorize a broadcast receiver source.
 *
 * @author Marius Lindvall
 */
public final class AuthorizationActivity extends AppCompatActivity {
    /**
     * A string passed along with the broadcast intent to identify the source of the broadcast.
     */
    private String identifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorize_broadcast);
        this.identifier = getIntent().getStringExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER);
        ((TextView) findViewById(R.id.authorizeIdentifier)).setText(this.identifier);
    }

    /**
     * Called if the user presses the Yes button.
     */
    public void accept(@SuppressWarnings("unused") View view) {
        savePreference(true);
    }

    /**
     * Called if the user presses the No button.
     */
    public void deny(@SuppressWarnings("unused") View view) {
        savePreference(false);
    }

    /**
     * Saves the user's preference on whether or not the given source should be approved and closes
     * the dialog.
     *
     * @param preference Whether or not the source was authorized.
     */
    private void savePreference(boolean preference) {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_AUTHORIZATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(this.identifier, preference);
        editor.apply();
        finish();
    }
}
