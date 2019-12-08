<?php const CONFIG = array(

// The type of storage backend Hauk will use. Valid values include:
// MEMCACHED, REDIS
//
// For MEMCACHED, you need either the `memcached` or `memcache` extensions
// enabled in PHP.
//
// For REDIS, you need `redis` extension enabled. Note that `redis` depends on
// `igbinary`, so if you get an error that a redis extension was not found, even
// though you enabled `redis`, you may have to also install and enable
// `igbinary` in PHP.
"storage_backend"       => MEMCACHED,

/*----------------------------------------------------------------------------*\
|  MEMCACHED SPECIFIC SETTINGS                                                 |
\*----------------------------------------------------------------------------*/

// Connection to memcached for data storage. To connect via UNIX socket instead
// of TCP, set host to 'unix:///path/to/memcached.sock' and port to 0.
"memcached_host"        => 'localhost',
"memcached_port"        => 11211,

// If you use SASL authentication, change both `memcached_binary` and
// `memcached_use_sasl` to true, and enter your SASL username and password.
// Note: SASL authentication is only supported in the PHP `memcached` extension!
// If you are using `memcache` and need SASL, consider switching to `memcached`.
"memcached_binary"      => false,
"memcached_use_sasl"    => false,
"memcached_sasl_user"   => "",
"memcached_sasl_pass"   => "",

// A prefix to use for all variables sent to memcached. Useful if you have a
// shared memcached instance or run multiple instances of Hauk.
"memcached_prefix"  => 'hauk',

/*----------------------------------------------------------------------------*\
|  REDIS SPECIFIC SETTINGS                                                     |
\*----------------------------------------------------------------------------*/

// Connection to Redis for data storage. To connect via UNIX socket instead of
// TCP, set host to '/path/to/redis.sock'.
"redis_host"            => 'localhost',
"redis_port"            => 6379,

// If you use password authentication in Redis, set `redis_use_auth` to true and
// enter the password in `redis_auth`.
"redis_use_auth"        => false,
"redis_auth"            => '',

// A prefix to use for all variables sent to Redis. Useful if you have a shared
// Redis instance or run multiple instances of Hauk.
"redis_prefix"          => 'hauk',

/*----------------------------------------------------------------------------*\
|  AUTHENTICATION                                                              |
\*----------------------------------------------------------------------------*/

// Users must be authenticated to use the Hauk server. The default
// authentication method is using a static server password that is shared by all
// users, without the need for a username. You can, however, use other
// authentication methods. Valid values here include:
//
// - PASSWORD: Use a static, shared server password for everyone
// - HTPASSWD: Require a username and separate password for each user
"auth_method"       => PASSWORD,

/*----------------------------------------------------------------------------*\
|  PASSWORD AUTHENTICATION                                                     |
\*----------------------------------------------------------------------------*/

// A hashed password that is required for creating sessions and posting location
// data to Hauk. To generate this value on the terminal:
//   - MD5 (insecure!):     openssl passwd -1
//   - bcrypt (secure):     htpasswd -nBC 10 "" | tail -c +2
"password_hash"     => '$2y$10$4ZP1iY8A3dZygXoPgsXYV.S3gHzBbiT9nSfONjhWrvMxVPkcFq1Ka',
// Default value above is empty string (no password) and is VERY INSECURE.
// Trust me, you really should change this unless you intentionally want a
// public instance that anyone in the world can use freely.
//
// Also note that users have the option to save the server password locally on
// their devices using a "Remember password" checkbox. If they choose to do so,
// the password will be stored in plain text (unhashed) on their devices. You
// are encouraged to generate a random password to prevent risks associated with
// credential reuse, should the password somehow be leaked from their devices.

/*----------------------------------------------------------------------------*\
|  HTPASSWD AUTHENTICATION                                                     |
\*----------------------------------------------------------------------------*/

// A file that contains a pairing between users and hashed passwords. To
// generate this file on the terminal:
//   - htpasswd -cBC 10 /etc/hauk/users.htpasswd <username>
// To add additional users to an existing file:
//   - htpasswd -BC 10 /etc/hauk/users.htpasswd <username>
"htpasswd_path"     => '/etc/hauk/users.htpasswd',

/*----------------------------------------------------------------------------*\
|  GENERAL SETTINGS                                                            |
\*----------------------------------------------------------------------------*/

// Hauk v1.4 and on allows you to request a custom link ID instead of having the
// server randomly generate one. Custom links can use characters A-Z, a-z, 0-9,
// - (dash), and _ (underscore). If you want to disallow the option to request
// custom links, set this to false.
//
// If a user requests particular custom link that is already in use, that user
// will not have their request honored and will get a randomly generated link
// instead.
"allow_link_req"    => true,

// If you want certain links to only be usable by some users, you can reserve
// them here. The following example reserves https://example.com/?WheresAlice
// for user "alice" only, and reserves https://example.com/?TheRealBob
// for use by both "bob" and "charlie".
//
// If you use Tasker or another automation platform to automatically start
// sharing to a specific link ID, it's a good idea to specify it here so that
// others cannot use it while you are inactive.
//
// Note that for this setting to have any effect, you have to specify an
// auth_method that requires both a username and a password, such as HTPASSWD.
"reserved_links"    => [
    'WheresAlice'       => ['alice'],
    'TheRealBob'        => ['bob', 'charlie'],
],

// If you want to enable pre-approved custom links only, you can choose to
// enable reservation whitelist mode. If this setting is set to true, custom
// link IDs will only be accepted if they are present in the reserved_links
// array above - requests to share to other links than those in the array will
// not be honored.
"reserve_whitelist" => false,

// The type of links to generate when making new links for shares. Can be any
// of the following:
//
// | Link style                 | Example                               | No. of combinations   | Avg. bruteforce time          |
// +----------------------------+---------------------------------------+-----------------------+-------------------------------+
// | LINK_4_PLUS_4_UPPER_CASE   | EIRG-0CYE                             | 2.82 * 10^12 (36^8)   | 44.7 years                    |
// | LINK_4_PLUS_4_LOWER_CASE   | qae3-ulna                             | 2.82 * 10^12 (36^8)   | 44.7 years                    |
// | LINK_4_PLUS_4_MIXED_CASE   | WRho-uHLG                             | 1.68 * 10^14 (60^8)   | 2663 years                    |
// | LINK_UUID_V4               | 09c8a3b1-e78f-48b1-a604-0da49e99cb5d  | 5.32 * 10^36 (2^122)  | 84.2 septillion years         |
// | LINK_16_HEX                | 6cde14c4c6551b41                      | 1.84 * 10^19 (2^64)   | 292 million years             |
// | LINK_16_UPPER_CASE         | 49OFGRK6SGPU93KV                      | 7.95 * 10^24 (36^16)  | 126 trillion years            |
// | LINK_16_LOWER_CASE         | bdyslxszs14cj359                      | 7.95 * 10^24 (36^16)  | 126 trillion years            |
// | LINK_16_MIXED_CASE         | NTHX2HDsTn0kS3aj                      | 2.82 * 10^28 (60^16)  | 447 quadrillion years         |
// | LINK_32_HEX                | 22adf21f11491ae8f3ae128e23a6782f      | 3.40 * 10^38 (2^128)  | 5.39 octillion years          |
// | LINK_32_UPPER_CASE         | MG42MW2DKIMHM87B4AO0WAB2PIY26TR1      | 6.33 * 10^49 (36^32)  | 1 duodecillion years          |
// | LINK_32_LOWER_CASE         | itgbolrbq1c02eot5o46c5wixhdrdb5m      | 6.33 * 10^49 (36^32)  | 1 duodecillion years          |
// | LINK_32_MIXED_CASE         | cTK82MJ7rUOP138WNVznQR0Ck3BwZp6b      | 7.96 * 10^57 (60^32)  | 12.6 quattuordecillion years  |
//
// For any MIXED_CASE variants, upper-case I and lower-case L will not appear
// because they are visually very similar and are easily confused.
//
// The default value is LINK_4_PLUS_4_UPPER_CASE, which is still considered very
// secure. The bruteforce times in the table below are the average time it would
// take to find a valid sharing link, when there is one link active, at 1000
// guesses per second. For the default setting, this means it would take almost
// 45 years to find the link.
//
// This is assuming that the link is active 24/7 for that entire time. If you
// only have a link active 2% of the time, it would take over 2200 years.
//
// At 1000 guesses per second, you will likely notice that your server is
// noticeably slower and rapidly filling up with access logs.
//
// Very long links are also time-consuming to type, should you find yourself
// in need of typing in a link manually on another computer. This is the reason
// that short links are default.
//
// ---- PLEASE NOTE ----
// This option is provided to you only because several people have requested it
// as a convenience. You are free to change it, but you should know that
// changing the default here gives you, for all intents and purposes, no
// security advantage in practice.
//
"link_style"        => LINK_4_PLUS_4_UPPER_CASE,

// Leaflet tile URI template for the map frontend. Here are some examples:
//
// - OpenStreetMap directly:
//      https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
// - Mapbox:
//      https://api.tiles.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=YOUR_ACCESS_TOKEN
// - Thunderforest:
//      https://{s}.tile.thunderforest.com/neighbourhood/{z}/{x}/{y}.png?apikey=YOUR_API_KEY
// - Esri:
//      https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}
// - OpenMapSurfer:
//      https://maps.heigit.org/openmapsurfer/tiles/roads/webmercator/{z}/{x}/{y}.png
// - Hydda (OSM Sweden):
//      https://{s}.tile.openstreetmap.se/hydda/full/{z}/{x}/{y}.png
//
// Make sure you have permission to use the source you choose, and also use a
// proper attribution for that provider.
"map_tile_uri"      => 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',

// Attribution HTML code to be displayed in the bottom right corner of the map.
// The default value is suitable for OpenStreetMap tiles.
"map_attribution"   => 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>',

// Default and maximum zoom levels allowed on the map (0-20), higher value means
// closer zooming.
"default_zoom"      => 14,
"max_zoom"          => 19,

// Maximum duration of a single location share, in seconds.
"max_duration"      => 86400,

// Minimum time between each location update, in seconds.
"min_interval"      => 1,

// The time that should pass without any location updates received, in seconds,
// before the user is marked "offline" on the map.
"offline_timeout"   => 30,

// The timeout in seconds for map update requests from the map view. If a web
// request takes this long without a response, the map viewer is considered
// offline and will get a warning notifying them that they have lost their
// network connection.
"request_timeout"   => 10,

// Maximum number of data points stored for each share before old points are
// deleted. Map clients will see up to this amount of data points when they load
// the page.
"max_cached_pts"    => 3,

// Maximum number of data points that may be visible on the map at any time.
// This is used to draw trails behind the current location map marker. Higher
// values will show longer trails, but may reduce performance.
"max_shown_pts"     => 100,

// Number of seconds of data that should be used to calculate velocity.
"v_data_points"     => 2,

// The color of the marker trails. HTML color name or #rrggbb hex color code.
"trail_color"       => '#d80037',

// The unit of measurement of velocity. Valid are:
// KILOMETERS_PER_HOUR, MILES_PER_HOUR, METERS_PER_SECOND
"velocity_unit"     => KILOMETERS_PER_HOUR,

// The publicly accessible URL to reach Hauk, with trailing slash.
"public_url"        => 'https://example.com/'

);
