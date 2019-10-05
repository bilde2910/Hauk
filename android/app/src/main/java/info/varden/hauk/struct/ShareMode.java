package info.varden.hauk.struct;

import java.io.Serializable;

import info.varden.hauk.R;

/**
 * An enum describing various ways a share may be created when starting a sharing session.
 *
 * @author Marius Lindvall
 */
public enum ShareMode implements Serializable {

    CREATE_ALONE(0, R.string.link_type_solo),
    CREATE_GROUP(1, R.string.link_type_group_host),
    JOIN_GROUP(2, R.string.link_type_group_member);

    /**
     * Resolves a sharing mode by its index.
     *
     * @param index The index of the sharing mode.
     * @return A sharing mode enum member.
     * @throws EnumConstantNotPresentException if there is no matching mode for the given index.
     */
    public static ShareMode fromMode(int index) throws EnumConstantNotPresentException {
        for (ShareMode mode : ShareMode.values()) {
            if (mode.getIndex() == index) return mode;
        }
        throw new EnumConstantNotPresentException(ShareMode.class, "index=" + index);
    }

    /**
     * The index of the sharing mode. Used as the mode identifier when starting a session on the
     * server. Also used as the index of the sharing mode in the share mode dropdown in the UI,
     * hence the name of this field.
     */
    private final int index;

    /**
     * A string resource ID for displaying the type of share in the list of active sharing links.
     */
    @SuppressWarnings("FieldNotUsedInToString")
    private final int descriptorResource;

    ShareMode(int index, int descriptorResource) {
        this.index = index;
        this.descriptorResource = descriptorResource;
    }

    @Override
    public String toString() {
        return "ShareMode{index=" + this.index + "}";
    }

    public int getIndex() {
        return this.index;
    }

    /**
     * Returns whether or not this sharing mode is a group share variant.
     */
    public boolean isGroupType() {
        return this == CREATE_GROUP || this == JOIN_GROUP;
    }

    public int getDescriptorResource() {
        return this.descriptorResource;
    }
}
