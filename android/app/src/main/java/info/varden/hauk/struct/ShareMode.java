package info.varden.hauk.struct;

import java.io.Serializable;

import info.varden.hauk.R;

public enum ShareMode implements Serializable {

    CREATE_ALONE(0, R.string.link_type_solo),
    CREATE_GROUP(1, R.string.link_type_group_host),
    JOIN_GROUP(2, R.string.link_type_group_member);

    public static ShareMode fromMode(int mode) {
        for (ShareMode m : ShareMode.values()) {
            if (m.getMode() == mode) return m;
        }
        return null;
    }

    private final int mode;
    private final int descriptorResource;

    ShareMode(int mode, int descriptorResource) {
        this.mode = mode;
        this.descriptorResource = descriptorResource;
    }

    public int getMode() {
        return this.mode;
    }

    public boolean isGroupType() {
        return this == CREATE_GROUP || this == JOIN_GROUP;
    }

    public int getDescriptorResource() {
        return this.descriptorResource;
    }
}
