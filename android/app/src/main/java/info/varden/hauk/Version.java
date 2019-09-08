package info.varden.hauk;

public class Version implements Comparable<Version> {
    private final String ver;

    public Version(String ver) {
        if (ver == null) ver = "1.0";
        this.ver = ver;
    }

    public boolean newerThan(Version other) {
        return this.compareTo(other) > 0;
    }

    public boolean olderThan(Version other) {
        return this.compareTo(other) < 0;
    }

    public boolean atLeast(Version other) {
        return this.compareTo(other) <= 0;
    }

    public boolean atMost(Version other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Version other) {
        String[] thisParts = this.ver.split("\\.");
        String[] otherParts = other.ver.split("\\.");

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
    public String toString() {
        return this.ver;
    }
}
