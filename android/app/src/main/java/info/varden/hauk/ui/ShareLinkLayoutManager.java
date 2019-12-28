package info.varden.hauk.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.Share;
import info.varden.hauk.ui.listener.ShareLinkClickListener;
import info.varden.hauk.ui.listener.StopLinkClickListener;
import info.varden.hauk.utils.Log;

/**
 * Class that manages a {@link ViewGroup} and designates it as a holder for a list of active share
 * links on the UI.
 *
 * @author Marius Lindvall
 */
public final class ShareLinkLayoutManager {
    /**
     * The activity on which the links should be placed.
     */
    private final Activity act;

    /**
     * The session manager to call upon to stop sharing specific links.
     */
    private final SessionManager manager;

    /**
     * The parent layout that should contain the list of links.
     */
    private final ViewGroup linkLayout;

    /**
     * The header above the link list, used to change the text when there are no active shares.
     */
    private final TextView headerView;

    /**
     * A list of links displayed on the UI that the client is contributing to, paired with the View
     * representing the link of that share and its controls in the link list.
     */
    private final Map<Share, View> shareViewMap;

    ShareLinkLayoutManager(Activity act, SessionManager manager, ViewGroup linkLayout, TextView headerView) {
        this.act = act;
        this.manager = manager;
        this.linkLayout = linkLayout;
        this.headerView = headerView;
        this.shareViewMap = new HashMap<>();
        removeAll();
    }

    /**
     * Adds a link to the list of links that represents the given share.
     *
     * @param share The share whose link should be added to the link list.
     */
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    void add(Share share) {
        this.act.runOnUiThread(new AddTask(share));
    }

    /**
     * Returns the number of shares that are visible on the UI.
     */
    public int getShareViewCount() {
        return this.shareViewMap.size();
    }

    /**
     * Removes a link from the list of links that represents the given share.
     *
     * @param share The share whose link should be removed from the link list.
     */
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    void remove(final Share share) {
        this.act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShareLinkLayoutManager.this.linkLayout.removeView(ShareLinkLayoutManager.this.shareViewMap.remove(share));
                if (ShareLinkLayoutManager.this.shareViewMap.isEmpty()) {
                    ShareLinkLayoutManager.this.headerView.setText(R.string.label_heading_no_links);
                }
            }
        });

    }

    /**
     * Removes all links from the list of links.
     */
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    void removeAll() {
        this.act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShareLinkLayoutManager.this.linkLayout.removeAllViews();
                ShareLinkLayoutManager.this.shareViewMap.clear();
                ShareLinkLayoutManager.this.headerView.setText(R.string.label_heading_no_links);
            }
        });
    }

    /**
     * The task that adds a new share to the UI. Runs on the UI thread.
     */
    private final class AddTask implements Runnable {
        private final Share share;

        private AddTask(Share share) {
            this.share = share;
        }

        @Override
        public void run() {
            Log.i("Adding share %s to list of links on the UI", this.share); //NON-NLS

            // Get the table row layout and inflate it into a view.
            LayoutInflater inflater = ShareLinkLayoutManager.this.act.getLayoutInflater();
            View linkView = inflater.inflate(R.layout.content_link, ShareLinkLayoutManager.this.linkLayout, false);

            // Add an event handler for the stop button. This will stop the given share only.
            Button btnStop = linkView.findViewById(R.id.linkBtnStop);
            if (this.share.getSession().getBackendVersion().isAtLeast(Constants.VERSION_COMPAT_VIEW_ID)) {
                Log.i("Server is compatible with individual share termination"); //NON-NLS
                btnStop.setOnClickListener(new StopLinkClickListener(
                        ShareLinkLayoutManager.this.act,
                        ShareLinkLayoutManager.this.manager,
                        this.share,
                        ShareLinkLayoutManager.this
                ));
                btnStop.setLayoutParams(new TableRow.LayoutParams(calculateRealWidth(btnStop), ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                Log.i("Server is not compatible with individual share termination"); //NON-NLS
                btnStop.setVisibility(View.GONE);
            }

            // Add an event handler for the share button.
            Button btnShare = linkView.findViewById(R.id.linkBtnShare);
            btnShare.setOnClickListener(new ShareLinkClickListener(ShareLinkLayoutManager.this.act, this.share));
            btnShare.setLayoutParams(new TableRow.LayoutParams(calculateRealWidth(btnShare), ViewGroup.LayoutParams.WRAP_CONTENT));

            // Update the text on the UI.
            TextView txtLink = linkView.findViewById(R.id.linkTxtLink);
            txtLink.setText(this.share.getID());
            TextView txtDesc = linkView.findViewById(R.id.linkTxtDesc);
            txtDesc.setText(ShareLinkLayoutManager.this.act.getString(this.share.getShareMode().getDescriptorResource()));

            // Add the view to the list of entries, so it can be removed later if the user stops the
            // share.
            Log.i("Putting share in class-level share list"); //NON-NLS
            ShareLinkLayoutManager.this.shareViewMap.put(this.share, linkView);
            ShareLinkLayoutManager.this.linkLayout.addView(linkView);
            ShareLinkLayoutManager.this.headerView.setText(R.string.label_heading_links);
        }
    }

    /**
     * Calculates the real displayed with of a {@link TextView}. The measurement of text view
     * (button) width is unreliable when inflating views, as it does not appear to take into account
     * drawables or padding properly. This method calculates the drawn width manually instead,
     * giving a reliable width that prevents character wrapping.
     *
     * @param view The text view to calculate the width for.
     * @return A width in pixels.
     */
    private static int calculateRealWidth(TextView view) {
        // Calculate the padding of the view.
        int realWidth = view.getCompoundDrawablePadding() + view.getTotalPaddingStart() + view.getTotalPaddingEnd();

        // Add the width of the contained text.
        String text = view.getTransformationMethod().getTransformation(view.getText(), view).toString();
        realWidth += view.getPaint().measureText(text);

        // Add the widths of any drawables on the button.
        for (Drawable drawable : view.getCompoundDrawablesRelative()) {
            if (drawable != null) realWidth += drawable.getIntrinsicWidth();
        }

        // Return the result.
        return realWidth;
    }
}
