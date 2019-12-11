package info.varden.hauk;

import info.varden.hauk.struct.Version;
import info.varden.hauk.system.preferences.Preference;

/**
 * Constants used in the Hauk app.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings("HardCodedStringLiteral")
public enum Constants {
    ;

    // Duration units.
    public static final int DURATION_UNIT_MINUTES = 0;
    public static final int DURATION_UNIT_HOURS = 1;
    public static final int DURATION_UNIT_DAYS = 2;

    // Shared preferences dictionaries.
    public static final String SHARED_PREFS_CONNECTION = "connectionPrefs";
    public static final String SHARED_PREFS_RESUMABLE = "sessionResumption";
    public static final String SHARED_PREFS_AUTHORIZATIONS = "broadcastAuthorizations";
    public static final String SHARED_PREFS_DEVICE_SPECS = "deviceSpecs";

    // Keys for use in stored server preferences.
    public static final Preference<String> PREF_SERVER_ENCRYPTED = new Preference.EncryptedString("cryptServer", "");
    public static final Preference<String> PREF_USERNAME_ENCRYPTED = new Preference.EncryptedString("cryptUsername", "");
    public static final Preference<String> PREF_PASSWORD_ENCRYPTED = new Preference.EncryptedString("cryptPassword", "");
    public static final Preference<Integer> PREF_DURATION = new Preference.Integer("duration", 30);
    public static final Preference<Integer> PREF_INTERVAL = new Preference.Integer("interval", 1);
    public static final Preference<String> PREF_CUSTOM_ID = new Preference.String("requestLink", "");
    public static final Preference<Boolean> PREF_ENABLE_E2E = new Preference.Boolean("enableE2E", false);
    public static final Preference<String> PREF_E2E_PASSWORD = new Preference.EncryptedString("e2ePassword", "");
    public static final Preference<String> PREF_NICKNAME = new Preference.String("nickname", "");
    public static final Preference<Integer> PREF_DURATION_UNIT = new Preference.Integer("durUnit", Constants.DURATION_UNIT_MINUTES);
    public static final Preference<Boolean> PREF_REMEMBER_PASSWORD = new Preference.Boolean("rememberPassword", false);
    public static final Preference<Boolean> PREF_ALLOW_ADOPTION = new Preference.Boolean("allowAdoption", true);

    @Deprecated // Use PREF_SERVER_ENCRYPTED instead
    public static final Preference<String> PREF_SERVER = new Preference.String("server", "");
    @Deprecated // Use PREF_USERNAME_ENCRYPTED instead
    public static final Preference<String> PREF_USERNAME = new Preference.String("username", "");
    @Deprecated // Use PREF_PASSWORD_ENCRYPTED instead
    public static final Preference<String> PREF_PASSWORD = new Preference.String("password", "");

    // Keys for use in session resumption preferences.
    public static final String RESUME_AVAILABLE = "canResume";
    public static final String RESUME_CLIENT_VERSION = "clientVersion";
    public static final String RESUME_SESSION_PARAMS = "sessionParams";
    public static final String RESUME_SHARE_PARAMS = "shareParams";

    // Keys for use in device spec preferences.
    public static final String DEVICE_PREF_WARNED_BATTERY_SAVINGS = "hasPromptedBatterySavings";

    // Regular expression for extracting a share ID from a URL when adopting a share.
    public static final String REGEX_ADOPT_ID_FROM_LINK = "\\?([A-Za-z0-9-]+)";

    // Default date format.
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    // Keys for intent extras.
    public static final String EXTRA_SHARE = "share";
    public static final String EXTRA_STOP_TASK = "stopTask";
    public static final String EXTRA_GNSS_ACTIVE_TASK = "gnssActiveTask";
    public static final String EXTRA_BROADCAST_RECEIVER_REGISTRY_INDEX = "dataRegistryIndex";
    public static final String EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER = "source";
    public static final String EXTRA_SESSION_SERVER_URL = "server";
    public static final String EXTRA_SESSION_USERNAME = "username";
    public static final String EXTRA_SESSION_PASSWORD = "password";
    public static final String EXTRA_SESSION_DURATION = "duration";
    public static final String EXTRA_SESSION_CUSTOM_ID = "requestLink";
    public static final String EXTRA_SESSION_E2E_PASSWORD = "e2ePassword";
    public static final String EXTRA_SESSION_INTERVAL = "interval";
    public static final String EXTRA_SESSION_ALLOW_ADOPT = "adoptable";

    // Content types for intents.
    public static final String INTENT_TYPE_COPY_LINK = "text/plain";

    // Backend URLs.
    public static final String URL_PATH_ADOPT_SHARE = "api/adopt.php";
    public static final String URL_PATH_CREATE_SHARE = "api/create.php";
    public static final String URL_PATH_CREATE_NEW_LINK = "api/new-link.php";
    public static final String URL_PATH_POST_LOCATION = "api/post.php";
    public static final String URL_PATH_STOP_SHARING = "api/stop.php";

    // Packet parameter keys.
    public static final String PACKET_PARAM_ACCURACY = "acc";
    public static final String PACKET_PARAM_ADOPTABLE = "ado";
    public static final String PACKET_PARAM_DURATION = "dur";
    public static final String PACKET_PARAM_E2E_FLAG = "e2e";
    public static final String PACKET_PARAM_GROUP_PIN = "pin";
    public static final String PACKET_PARAM_ID_TO_ADOPT = "aid";
    public static final String PACKET_PARAM_INIT_VECTOR = "iv";
    public static final String PACKET_PARAM_INTERVAL = "int";
    public static final String PACKET_PARAM_LATITUDE = "lat";
    public static final String PACKET_PARAM_LONGITUDE = "lon";
    public static final String PACKET_PARAM_NICKNAME = "nic";
    public static final String PACKET_PARAM_PASSWORD = "pwd";
    public static final String PACKET_PARAM_SALT = "salt";
    public static final String PACKET_PARAM_SESSION_ID = "sid";
    public static final String PACKET_PARAM_SHARE_ID = "lid";
    public static final String PACKET_PARAM_SHARE_MODE = "mod";
    public static final String PACKET_PARAM_SPEED = "spd";
    public static final String PACKET_PARAM_TIMESTAMP = "time";
    public static final String PACKET_PARAM_USERNAME = "usr";

    // Packet OK response header. All valid packets start with this line.
    public static final String PACKET_RESPONSE_OK = "OK";

    // HTTP data.
    public static final String HTTP_HEADER_HAUK_VERSION = "X-Hauk-Version";

    // Minimum backend version supporting group shares.
    public static final Version VERSION_COMPAT_GROUP_SHARE = new Version("1.1");

    // Minimum backend version that sends the link ID as well as the view link itself.
    public static final Version VERSION_COMPAT_VIEW_ID = new Version("1.2");

    // Minimum backend/frontend version that support end-to-end encryption.
    public static final Version VERSION_COMPAT_E2E_ENCRYPTION = new Version("1.5");

    // End-to-end encryption specifications.
    public static final int E2E_AES_KEY_SIZE = 256;
    public static final int E2E_PBKDF2_ITERATIONS = 65536;
    public static final String E2E_KD_FUNCTION = "PBKDF2WithHmacSHA1";
    public static final String E2E_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final String E2E_KEY_SPEC = "AES";
}
