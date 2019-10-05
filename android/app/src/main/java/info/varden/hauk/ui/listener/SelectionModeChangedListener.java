package info.varden.hauk.ui.listener;

import android.view.View;
import android.widget.AdapterView;

import java.util.Objects;

import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.utils.Log;

/**
 * On-change listener for the select box that lets user set the sharing mode when creating a new
 * sharing session.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class SelectionModeChangedListener implements AdapterView.OnItemSelectedListener {
    /**
     * The UI view to show when the sharing mode is set to create a share alone.
     */
    private final View soloView;

    /**
     * The UI view to show when the sharing mode is set to create or join a group.
     */
    private final View nicknameView;

    /**
     * The UI view to show when the sharing mode is set to join a group with a group code.
     */
    private final View groupCodeView;

    public SelectionModeChangedListener(View soloView, View nicknameView, View groupCodeView) {
        this.soloView = soloView;
        this.nicknameView = nicknameView;
        this.groupCodeView = groupCodeView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // This handler determines which UI elements should be visible, based on the user's
        // selection of sharing modes.
        switch (Objects.requireNonNull(ShareMode.fromMode(position))) {
            case CREATE_ALONE:
                Log.i("Sharing mode switched to single-user"); //NON-NLS
                this.soloView.setVisibility(View.VISIBLE);
                this.nicknameView.setVisibility(View.GONE);
                this.groupCodeView.setVisibility(View.GONE);
                break;
            case CREATE_GROUP:
                Log.i("Sharing mode switched to create group"); //NON-NLS
                this.soloView.setVisibility(View.GONE);
                this.nicknameView.setVisibility(View.VISIBLE);
                this.groupCodeView.setVisibility(View.GONE);
                break;
            case JOIN_GROUP:
                Log.i("Sharing mode switched to join group"); //NON-NLS
                this.soloView.setVisibility(View.GONE);
                this.nicknameView.setVisibility(View.VISIBLE);
                this.groupCodeView.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.wtf("Nothing selected as sharing mode"); //NON-NLS
    }
}
