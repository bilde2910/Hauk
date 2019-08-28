![Hauk](./backend/assets/logo.svg)

# Hauk

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/info.varden.hauk)

Hauk is a fully open source, self-hosted location sharing service. Install the
backend code on a PHP-compatible web server, install the companion app on your
phone, and you're good to go!

## Installation instructions

1. On your server, install Memcached, PHP and a web server.
2. Copy everything in the "backend" folder in this repository to somewhere in
   your web root.
3. Modify config.php to your liking.
4. Start Memcached and the web server.
5. Install the [companion Android app](https://f-droid.org/packages/info.varden.hauk/) on your phone.

## Demo server

If you'd like to see what Hauk can do, download the app and insert connection details for the demo server:

Server: https://apps.varden.info/demo/hauk/
Password: `demo`

Location shares on the demo server is limited to 2 minutes and is only meant for demonstration purposes. Set up your own server to use Hauk to its full extent.
