<?php const CONFIG = array(

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

// A prefix to use for all variables sent to memcached. Useful if you have a
// shared memcached instance or run multiple instances of Hauk.
"memcached_prefix"  => 'hauk',

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
