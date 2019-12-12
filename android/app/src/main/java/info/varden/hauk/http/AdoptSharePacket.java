package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;

/**
 * Packet representing the action of adopting a solo share into a group share.
 *
 * @author Marius Lindvall
 */
public abstract class AdoptSharePacket extends Packet {
    /**
     * Called when the user is adopted.
     *
     * @param nickname The nickname assigned to the user.
     */
    protected abstract void onSuccessfulAdoption(String nickname);

    /**
     * The assigned nickname of the user to adopt.
     */
    private final String nickname;

    /**
     * Creates the packet.
     *
     * @param ctx      Android application context.
     * @param target   The group share to adopt the solo share into.
     * @param origin   The share ID of the share to adopt.
     * @param nickname The nickname that should be assigned to the user when adopted.
     */
    protected AdoptSharePacket(Context ctx, Share target, String origin, String nickname) {
        super(ctx, target.getSession().getServerURL(), target.getSession().getProxy(), Constants.URL_PATH_ADOPT_SHARE);
        this.nickname = nickname;
        setParameter(Constants.PACKET_PARAM_SESSION_ID, target.getSession().getID());
        setParameter(Constants.PACKET_PARAM_NICKNAME, nickname);
        setParameter(Constants.PACKET_PARAM_ID_TO_ADOPT, origin);
        setParameter(Constants.PACKET_PARAM_GROUP_PIN, target.getJoinCode());
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Check that the data is valid.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        } else {
            // A successful response always has OK on line 1.
            if (data[0].equals(Constants.PACKET_RESPONSE_OK)) {
                onSuccessfulAdoption(this.nickname);
            } else {
                // Unknown response.
                StringBuilder err = new StringBuilder();
                for (String line : data) {
                    err.append(line);
                    err.append(System.lineSeparator());
                }
                throw new ServerException(err.toString());
            }
        }
    }
}
