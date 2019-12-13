package info.varden.hauk.http.security;

/**
 * An enum representing the various types of proxies available on the system, and their ID when
 * stored in preferences.
 *
 * @author Marius Lindvall
 */
public enum CertificateValidationPolicy {
    VALIDATE_ALL(0),
    DISABLE_TRUST_ANCHOR_ONION(1),
    DISABLE_ALL_ONION(2);

    /**
     * An internal ID for this certificate validation policy that represents the value it is stored
     * as in preferences.
     */
    private final int index;

    CertificateValidationPolicy(int index) {
        this.index = index;
    }

    /**
     * Returns a validation policy by its index.
     *
     * @param index The index of the validation policy.
     * @return A validation policy enum member.
     * @throws EnumConstantNotPresentException if there is no matching type for the given index.
     */
    public static CertificateValidationPolicy fromIndex(int index) throws EnumConstantNotPresentException {
        for (CertificateValidationPolicy type : CertificateValidationPolicy.values()) {
            if (type.getIndex() == index) return type;
        }
        throw new EnumConstantNotPresentException(CertificateValidationPolicy.class, "index=" + index);
    }

    @Override
    public String toString() {
        return "CertificateValidationPolicy{index=" + this.index  + "}";
    }

    public int getIndex() {
        return this.index;
    }
}
