package info.varden.hauk.utils;

/**
 * Utility class to process strings.
 *
 * @author Marius Lindvall
 */
public enum StringUtils {
    ;

    // Byte array to hex string function by maybeWeCouldStealAVan
    // https://stackoverflow.com/a/9855338

    @SuppressWarnings("HardCodedStringLiteral")
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    @SuppressWarnings("MagicNumber")
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
