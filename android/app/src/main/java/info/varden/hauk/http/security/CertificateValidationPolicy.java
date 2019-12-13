package info.varden.hauk.http.security;

import info.varden.hauk.system.preferences.IndexedEnum;

/**
 * An enum representing the various types of proxies available on the system, and their ID when
 * stored in preferences.
 *
 * @author Marius Lindvall
 */
public final class CertificateValidationPolicy extends IndexedEnum<CertificateValidationPolicy> {
    private static final long serialVersionUID = -1906017485528370776L;

    public static final CertificateValidationPolicy VALIDATE_ALL = new CertificateValidationPolicy(0);
    public static final CertificateValidationPolicy DISABLE_TRUST_ANCHOR_ONION = new CertificateValidationPolicy(1);
    public static final CertificateValidationPolicy DISABLE_ALL_ONION = new CertificateValidationPolicy(2);

    private CertificateValidationPolicy(int index) {
        super(index);
    }

    @Override
    public String toString() {
        return "CertificateValidationPolicy{" + super.toString() + "}";
    }
}
