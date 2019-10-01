package info.varden.hauk.struct;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * A class for comparing version strings.
 *
 * @author Marius Lindvall
 */
public class Version implements Comparable<Version>, Serializable {
    private final String ver;

    public Version(String ver) {
        if (ver == null) this.ver = "1.0";
        else this.ver = ver;
    }

    public boolean atLeast(Version other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Version other) {
        String[] thisParts = this.ver.split("\\.");
        String[] otherParts = other.ver.split("\\.");

        // Compare each segment of the version to determine if the version is newer, older or the
        // same as the current version.
        int maxLen = Math.max(thisParts.length, otherParts.length);
        for (int i = 0; i < maxLen; i++) {
            int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
            int otherPart = i < otherParts.length ? Integer.parseInt(otherParts[i]) : 0;
            if (thisPart < otherPart) return -1;
            if (thisPart > otherPart) return 1;
        }

        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return this.ver;
    }
}
