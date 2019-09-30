package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.R;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;
import info.varden.hauk.throwable.ServerException;

/**
 * Packet representing the action of adopting a solo share into a group share.
 *
 * @author Marius Lindvall
 */
public abstract class AdoptSharePacket extends Packet {
    public abstract void onSuccessfulAdoption(String nickname);

    private final String nickname;

    /**
     * Creates the packet.
     *
     * @param ctx      Android application context.
     * @param target   The group share to adopt the solo share into.
     * @param origin   The share ID of the share to adopt.
     * @param nickname The nickname that should be assigned to the user when adopted.
     */
    public AdoptSharePacket(Context ctx, Share target, String origin, String nickname) {
        super(ctx, target.getSession().getServerURL(), "api/adopt.php");
        this.nickname = nickname;
        addParameter("sid", target.getSession().getID());
        addParameter("nic", nickname);
        addParameter("aid", origin);
        addParameter("pin", target.getJoinCode());
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Check that the data is valid.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        } else {
            // A successful response always has OK on line 1.
            if (data[0].equals("OK")) {
                onSuccessfulAdoption(this.nickname);
            } else {
                // Unknown response.
                StringBuilder err = new StringBuilder();
                for (String line : data) {
                    err.append(line);
                    err.append("\n");
                }
                throw new ServerException(err.toString());
            }
        }
    }
}
