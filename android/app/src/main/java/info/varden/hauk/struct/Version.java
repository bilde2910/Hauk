package info.varden.hauk.struct;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * A class for comparing version strings.
 *
 * @author Marius Lindvall
 */
public final class Version implements Comparable<Version>, Serializable {
    private static final long serialVersionUID = 3855294306995801704L;

    /**
     * The version number in question.
     */
    private final String ver;

    public Version(String ver) {
        // Version defaults to 1.0 if null, as 1.0.x backends did not return an X-Hauk-Version
        // header.
        this.ver = ver != null ? ver : "1.0";
    }

    /**
     * Checks whether or not this version number is equal to or greater than the version number
     * passed as the argument to this function.
     *
     * @param other The version to compare to.
     */
    public boolean isAtLeast(Version other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Version other) {
        String[] thisParts = this.ver.split("\\.");
        String[] otherParts = other.toString().split("\\.");

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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) return false;
        return compareTo((Version) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.ver.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return this.ver;
    }
}
