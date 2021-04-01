// This is the main script file for Hauk's web view client.

const SHARE_TYPE_ALONE = 0;
const SHARE_TYPE_GROUP = 1;

const LOC_PROVIDER_FINE = 0;
const LOC_PROVIDER_COARSE = 1;

const STATE_LIVE_COLOR = '#d80037';
const STATE_ROUGH_COLOR = '#ff9c00';
const STATE_DEAD_COLOR = '#555555';

const EARTH_DIAMETER_KM = 6371 * 2;
const HAV_MOD = EARTH_DIAMETER_KM * 1000;

// Find preferred language.
var locales = ['ca', 'de', 'en', 'eu', 'fr', 'it', 'nb_NO', 'nl', 'nn', 'pt_BR', 'ro', 'ru', 'tr', 'uk'];
var prefLang = 'en';
if (navigator.languages) {
    for (var i = navigator.languages.length - 1; i >= 0; i--) {
        for (var j = 0; j < locales.length; j++) {
            if (navigator.languages[i] == locales[j]) {
                prefLang = locales[j].split('-').join('_');
            }
        }
    }
}

// Load localization English as fallback.
var LANG = {};
var xhr = new XMLHttpRequest();
xhr.onreadystatechange = function() {
    if (this.readyState == 4 && this.status == 200) {
        LANG = JSON.parse(this.responseText);
        // Overwrite the L10N data with localizations for the preferred language.
        if (prefLang != 'en') {
            var xhr2 = new XMLHttpRequest();
            xhr2.onreadystatechange = function() {
                if (this.readyState == 4 && this.status == 200) {
                    var data = JSON.parse(this.responseText);
                    for (var key in data) {
                        if (!data.hasOwnProperty(key)) continue;
                        LANG[key] = data[key];
                    }
                    localizeHTML();
                    init();
                }
            };
            xhr2.open('GET', './assets/lang/' + prefLang + '.json', true);
            xhr2.send();
        } else {
            localizeHTML();
            init();
        }
    }
};
xhr.open('GET', './assets/lang/en.json', true);
xhr.send();

// Put localized strings in HTML.
function localizeHTML() {
    var tags = document.querySelectorAll('[data-i18n]');
    for (var key in tags) {
        if (!tags.hasOwnProperty(key)) continue;
        var i18nKey = tags[key].getAttribute('data-i18n');
        if (tags[key].hasAttribute('data-i18n-attr')) {
            tags[key].setAttribute(tags[key].getAttribute('data-i18n-attr'), LANG[i18nKey]);
        } else {
            tags[key].textContent = LANG[i18nKey];
        }
    }
}

// Starts fetching location data from the server. This is called after I18N has
// loaded to ensure strings are present for prompts.
function init() {
    if (location.href.indexOf("?") === -1 || id == "") {
        // If there is no share ID, show the root page.
        var url = location.href.indexOf("?") === -1 ? location.href : location.href.substring(0, location.href.indexOf("?"));

        var urlE = document.getElementById("url");
        var indexE = document.getElementById("index");
        var storeIconFdroidE = document.getElementById("store-icon-fdroid");
        var storeIconGplayE = document.getElementById("store-icon-gplay");
        if (urlE !== null) urlE.textContent = url;
        if (indexE !== null) indexE.style.display = "block";
        if (storeIconFdroidE !== null) storeIconFdroidE.src = LANG["f_droid_badge_url"];
        if (storeIconGplayE !== null) storeIconGplayE.src = LANG["google_play_badge_url"];
    } else {
        // Attempt to fetch location data from the server once.
        getJSON("./api/fetch.php?id=" + id, function(data) {
            // Initialize the Leaflet map.
            initMap();
            noGPS.style.display = "block";
            processUpdate(data, true);
        }, function() {
            var notFoundE = document.getElementById("notfound");
            if (notFoundE !== null) notFoundE.style.display = "block";
        });
    }
}

// For locating the viewer of the map. Watcher is a watchPosition() reference.
var watcher = null;

// Marker and accuracy circle of the person viewing the map.
var selfCircle = null;
var selfMarker = null;

// Create a geolocation control.
L.control.Locate = L.Control.extend({
    onAdd: function(map) {
        var btn = L.DomUtil.create('div', 'leaflet-bar leaflet-control');
        var anchor = L.DomUtil.create('a', 'leaflet-control-locate-inactive');

		btn.style.backgroundColor = '#fff';
        btn.appendChild(anchor);

        anchor.href = '#';
        anchor.title = LANG["control_show_self"];
        anchor.onclick = function(e) {
            // Prevent # from appearing in URL.
            e.preventDefault();

            if (watcher === null) {
                // If there is no location watcher active, create one.
                // Update the icon to indicate that the location is pending.
                anchor.className = 'leaflet-control-locate-pending';
                watcher = navigator.geolocation.watchPosition(function(pos) {
                    // If the circle and marker is missing, add it and set the
                    // location status to active.
                    if (selfCircle == null && selfMarker == null) {
                        anchor.className = 'leaflet-control-locate-active';
                        selfCircle = L.circle([pos.coords.latitude, pos.coords.longitude], {
                            radius: pos.coords.accuracy,
                            fillColor: '#1e90ff',
                            fillOpacity: 0.25,
                            color: '#1e90ff',
                            opacity: 0.5,
                            interactive: false
                        }).addTo(circleLayer);
                        var selfIcon = L.divIcon({
                            html: '<div class="marker"><div class="arrow still-self"></div></div>',
                            iconAnchor: [33, 18]
                        });
                        selfMarker = L.marker([pos.coords.latitude, pos.coords.longitude], {
                            icon: selfIcon,
                            interactive: false
                        }).addTo(markerLayer);

                        // Unfollow any curretly followed user, then pan to the
                        // device location.
                        following = null;
                        map.panTo([pos.coords.latitude, pos.coords.longitude]);
                    } else {
                        selfCircle.setLatLng([pos.coords.latitude, pos.coords.longitude]);
                        selfMarker.setLatLng([pos.coords.latitude, pos.coords.longitude]);
                        selfCircle.setRadius(pos.coords.accuracy);
                    }
                });
            } else {
                // If there is already a watcher, clicking the control should
                // unregister it and hide the user's location.
                anchor.className = 'leaflet-control-locate-inactive';
                navigator.geolocation.clearWatch(watcher);
                watcher = null;
                circleLayer.removeLayer(selfCircle);
                markerLayer.removeLayer(selfMarker);
                selfCircle = null;
                selfMarker = null;
            }
            return false;
        };

		return btn;
    }
});

// Create a "show list of users" control.
L.control.Radar = L.Control.extend({
    onAdd: function(map) {
        var btn = L.DomUtil.create('div', 'leaflet-bar leaflet-control');
        var anchor = L.DomUtil.create('a', 'leaflet-control-radar');

		btn.style.backgroundColor = '#fff';
        btn.appendChild(anchor);

        anchor.href = '#';
        anchor.title = LANG["control_show_self"];
        anchor.onclick = function(e) {
            // Prevent # from appearing in URL.
            e.preventDefault();
            var userListPopupE = document.getElementById("user-list-popup");
            if (userListPopupE !== null) {
                userListPopupE.style.display = 'block';
            }
            var userListE = document.getElementById("user-list");
            if (userListE !== null) {
                var users = userListE.getElementsByTagName("p");
                if (users.length == 1) users[0].click();
            }
            return false;
        };

		return btn;
    }
});

// Function for spawning the controls.
L.control.locate = function(opts) {
    return new L.control.Locate(opts);
}
L.control.radar = function(opts) {
    return new L.control.Radar(opts);
}

var map, circleLayer, markerLayer;

function initMap() {
    // Create a Leaflet map.
    map = L.map('map').setView([0, 0], DEFAULT_ZOOM);
    L.tileLayer(TILE_URI, {
        attribution: ATTRIBUTION,
        maxZoom: MAX_ZOOM
    }).addTo(map);

    // Add the geolocation control to the map if supported by the browser.
    if ("geolocation" in navigator && window.isSecureContext) {
        L.control.locate({ position: 'topleft' }).addTo(map);
    }
    L.control.radar({ position: 'topleft' }).addTo(map);

    circleLayer = L.layerGroup().addTo(map);
    markerLayer = L.layerGroup().addTo(map);

    // Unfollow the user when the map is panned.
    map.on('mousedown', function() {
        following = null;
    });
}

// The leaflet markers and associated data.
var shares = {};

// Retrieve the sharing link ID from the URL. E.g.
// https://example.com/?ABCD-1234 --> "ABCD-1234"
var id = location.href.substr(location.href.indexOf("?") + 1);
if (id.indexOf("&") !== -1) id = id.substr(0, id.indexOf("&"));

// Whether the "offline" popup has appeared when the browser is offline.
var knownOffline = false;

function getJSON(url, callback, invalid) {
    var xhr = new XMLHttpRequest();
    xhr.timeout = REQUEST_TIMEOUT * 1000;
    xhr.open('GET', url, true);
    xhr.onreadystatechange = function() {
        if (this.readyState == 4) {
            var offlineE = document.getElementById("offline");
            var notchE = document.getElementById("notch");
            if (this.status === 200) {
                // Request successful. Reset offline state and parse the JSON.
                knownOffline = false;
                if (offlineE !== null) {
                    document.getElementById("offline").style.display = "none";
                }
                if (notchE !== null) {
                    document.getElementById("notch").className = "";
                }
                try {
                    var json = JSON.parse(this.responseText);
                    callback(json);
                } catch (ex) {
                    console.log(ex);
                    invalid();
                }
            } else if (this.status === 404) {
                invalid();
            } else {
                // Requested failed; offline.
                if (!knownOffline) {
                    knownOffline = true;
                    if (offlineE !== null) {
                        document.getElementById("offline").style.display = "block";
                    }
                    if (notchE !== null) {
                        document.getElementById("notch").className = "offline";
                    }
                }
            }
        }
    }
    xhr.send();
}

// General message popup box. Reused for several popups.
var dismissMessageE = document.getElementById("dismiss-message");
if (dismissMessageE !== null) {
    dismissMessageE.addEventListener("click", function() {
        var messageE = document.getElementById("message-popup");
        if (messageE !== null) messageE.style.display = "none";
    });
}

// Shows a dialog box with a title and message.
function showMessage(title, message) {
    var messageE = document.getElementById("message-popup");
    var titleE = document.getElementById("message-title");
    var bodyE = document.getElementById("message-body");
    if (titleE !== null) titleE.textContent = title;
    if (bodyE !== null) bodyE.textContent = message;
    if (messageE !== null) messageE.style.display = "block";
}

var dismissOfflineE = document.getElementById("dismiss-offline");
if (dismissOfflineE !== null) {
    dismissOfflineE.addEventListener("click", function() {
        var offlineE = document.getElementById("offline");
        if (offlineE !== null) offlineE.style.display = "none";
    });
}

// End-to-end encryption password prompt handlers.
var passwordInputE = document.getElementById("e2e-password");
var passwordDecryptE = document.getElementById("decrypt-e2e-password");
if (passwordInputE !== null) {
    passwordInputE.addEventListener("keyup", function(e) {
        if (e.keyCode == 13) {
            if (passwordDecryptE !== null) {
                passwordDecryptE.click();
            }
        }
    });
}

var passwordCancelE = document.getElementById("cancel-e2e-password");
if (passwordCancelE !== null) {
    passwordCancelE.addEventListener("click", function() {
        var promptE = document.getElementById("e2e-prompt");
        if (promptE !== null) promptE.style.display = "none";
        if (passwordDecryptE !== null && acceptKeyFunc !== null) passwordDecryptE.removeEventListener("click", acceptKeyFunc);
    });
}

var userDetailsE = document.getElementById("user-details-popup");

var userDetailsFollowE = document.getElementById("user-details-follow");
if (userDetailsFollowE !== null) {
    userDetailsFollowE.addEventListener("click", function() {
        var user = userDetailsFollowE.dataset.user;
        follow(user);
        if (userDetailsE !== null) userDetailsE.style.display = "none";
    });
}

var userDetailsNavigateE = document.getElementById("user-details-navigate");
if (userDetailsNavigateE !== null) {
    userDetailsNavigateE.addEventListener("click", function() {
        var user = userDetailsNavigateE.dataset.user;
        var points = shares[user].points;
        if (points.length > 0) {
            var last = points[points.length - 1];
            window.open("geo:" + last.lat + "," + last.lon);
        }
        if (userDetailsE !== null) userDetailsE.style.display = "none";
    });
}

var closeUserListE = document.getElementById("close-user-list");
if (closeUserListE !== null) {
    closeUserListE.addEventListener("click", function() {
        var userListPopupE = document.getElementById("user-list-popup");
        if (userListPopupE !== null) {
            userListPopupE.style.display = 'none';
        }
    });
}

var showAllUsersE = document.getElementById("btn-show-all");
if (showAllUsersE !== null) {
    showAllUsersE.addEventListener("click", function() {
        autoCenter();
        var userListPopupE = document.getElementById("user-list-popup");
        if (userListPopupE !== null) {
            userListPopupE.style.display = 'none';
        }
    });
}

var closeUserDetailsE = document.getElementById("close-user-details");
if (closeUserDetailsE !== null) {
    closeUserDetailsE.addEventListener("click", function() {
        if (userDetailsE !== null) {
            userDetailsE.style.display = 'none';
        }
    });
}

var fetchIntv;
var countIntv;

function setNewInterval(expire, interval, serverTime) {
    var countdownE = document.getElementById("countdown");
    var timeOffset = Date.now() / 1000 - serverTime;

    // The data contains an expiration time. Create a countdown at the top of
    // the map screen that ends when the share is over.
    countIntv = setInterval(function() {
        var seconds = expire - Math.round((Date.now() - timeOffset) / 1000);
        if (seconds < 0) {
            clearInterval(countIntv);
            return;
        }

        var h = Math.floor(seconds / 3600);
        var m = Math.floor((seconds % 3600) / 60);
        var s = seconds % 60;

        var time = "";
        if (h > 0) time += h + ":";
        if (h > 0 && m < 10) time += "0";
        time += m + ":";
        if (s < 10) time += "0";
        time += s;

        if (countdownE !== null) countdownE.textContent = time;
    }, 1000);

    // The location data contains an interval. Schedule a task that fetches data
    // once per interval time.
    fetchIntv = setInterval(function() {
        // Stop the task if the share has expired.
        if ((Date.now() - timeOffset) / 1000 >= expire) {
            clearInterval(fetchIntv);
            clearInterval(countIntv);
            if (countdownE !== null) countdownE.textContent = LANG["status_expired"];
            showMessage(LANG["dialog_expired_head"], LANG["dialog_expired_body"]);
        }

        getJSON("./api/fetch.php?id=" + id, function(data) {
            // Recreate the interval timers if the interval or expiration
            // change.
            if (data.expire != expire || data.interval != interval) {
                clearInterval(fetchIntv);
                clearInterval(countIntv);
                setNewInterval(data.expire, data.interval, data.serverTime);
            }
            processUpdate(data, false);
        }, function() {
            // On failure to get new location data:
            clearInterval(fetchIntv);
            clearInterval(countIntv);
            if (countdownE !== null) countdownE.textContent = LANG["status_expired"];
            showMessage(LANG["dialog_expired_head"], LANG["dialog_expired_body"]);
        });
    }, interval * 1000);
}

var noGPS = document.getElementById("searching");

// Whether or not an initial location has been received.
var hasReceivedFirst = false;

// Whether the map has been initially centered.
var hasInitiated = false;

// The user being followed on the map.
var following = null;

// The decryption key for end-to-end encrypted shares.
var aesKey = null;

// Button handler for the "Decrypt" button on the E2E password prompt.
var acceptKeyFunc = null;

// Converts a base64-encoded string to a Uint8Array ArrayBuffer for use with
// WebCrypto.
function byteArray(base64) {
    var raw = atob(base64);
    var len = raw.length;
    var arr = new Uint8Array(new ArrayBuffer(len));
    for (var i = 0; i < len; i++) {
        arr[i] = raw.charCodeAt(i);
    }
    return arr;
}

// Follow a user on the map.
function follow(user) {
    following = shares[user].id;
    var last = shares[user].points[shares[user].points.length - 1];
    map.panTo([last.lat, last.lon]);
}

// Zoom and pan map to show all users.
function autoCenter() {
    var markers = [];
    for (var share in shares) {
        if (!shares.hasOwnProperty(share)) continue;
        if (shares[share].marker === null) continue;
        markers.push(shares[share].marker);
    }
    var fg = new L.featureGroup(markers);
    map.fitBounds(fg.getBounds().pad(0.5));

    // Do not exceed the default zoom level.
    if (map.getZoom() > DEFAULT_ZOOM) map.setZoom(DEFAULT_ZOOM);
}

// Parses the data returned from ./api/fetch.php and updates the map marker.
function processUpdate(data, init) {
    var users = {};
    var multiUser = false;
    if (data.type == SHARE_TYPE_ALONE) {
        users[""] = data.points;
        multiUser = false;
    } else if (data.type == SHARE_TYPE_GROUP) {
        users = data.points;
        multiUser = true;
    }

    // Check for crypto support if necessary.
    if (data.encrypted && !("crypto" in window)) {
        showMessage(LANG["e2e_title"], LANG["e2e_unsupported"]);
        return;
    } else if (data.encrypted && !("subtle" in window.crypto)) {
        if (!window.isSecureContext) {
            showMessage(LANG["e2e_title"], LANG["e2e_unavailable_secure"]);
        } else {
            showMessage(LANG["e2e_title"], LANG["e2e_unsupported"]);
        }
        return;
    }

    if (data.encrypted && aesKey == null) {
        // If using end-to-end encryption, we need to decrypt the data. We have
        // not obtained an AES key yet, so prompt the user for it.
        var promptE = document.getElementById("e2e-prompt");
        var labelE = document.getElementById("e2e-password-label");
        if (promptE !== null && passwordInputE !== null && passwordDecryptE !== null && labelE !== null) {
            acceptKeyFunc = function() {
                // Remove the event listener, hide the dialog and fetch the
                // password.
                passwordDecryptE.removeEventListener("click", acceptKeyFunc);
                promptE.style.display = "none";
                labelE.textContent = LANG["e2e_incorrect"];
                var password = passwordInputE.value;

                // Get the salt in binary format.
                var salt = byteArray(data.salt);

                // Derive the encryption key using PBKDF2 with SHA-1. SHA-1 was chosen
                // because of availability in Android.
                crypto.subtle
                    .importKey("raw", new TextEncoder("utf-8").encode(password), "PBKDF2", false, ["deriveKey"])
                    .then(key => crypto.subtle.deriveKey(
                        {name: "PBKDF2", salt: salt, iterations: 65536, hash: "SHA-1"},
                        key,
                        {name: "AES-CBC", length: 256},
                        false,
                        ["decrypt"]
                    ))
                    .then(key => {
                        // Store the crypto key and re-process the update.
                        aesKey = key;
                        processUpdate(data, init);
                    });
            };

            // Attach the listener to the dialog box and show it.
            passwordDecryptE.addEventListener("click", acceptKeyFunc);
            passwordInputE.value = "";
            promptE.style.display = "block";
            passwordInputE.focus();
        }

        return;

    } else if (data.encrypted) {
        // The data is encrypted, but now we have a key we can use to decrypt
        // it. Decrypt each point using the key.
        var algo = {name: "AES-CBC"};

        var pointPromises = [];
        for (var i = 0; i < data.points.length; i++) {
            algo.iv = byteArray(data.points[i][0]);
            var promises = [];
            for (var j = 1; j < data.points[i].length; j++) {
                // Check that the array entry is not null to prevent an
                // exception. If the entry is null, push a promise that returns
                // null to the array to maintain indexing in the decrypted
                // result.
                if (data.points[i][j] !== null) {
                    promises.push(crypto.subtle.decrypt(algo, aesKey, byteArray(data.points[i][j])));
                } else {
                    promises.push(new Promise(function(resolve, reject) {
                        resolve(null);
                    }));
                }
            }
            pointPromises.push(Promise.all(promises));
        }

        // Wait for all points to be decrypted.
        Promise
            .all(pointPromises)
            .then(function(values) {
                // Parse all points and conver them to floating point values
                // (all values in the array are currently numbers).
                var decoder = new TextDecoder("utf-8");
                for (var i = 0; i < values.length; i++) {
                    for (var j = 0; j < values[i].length; j++) {
                        // Check that the value isn't null to avoid exceptions.
                        if (values[i][j] !== null) {
                            data.points[i][j] = parseFloat(decoder.decode(values[i][j]));
                        } else {
                            data.points[i][j] = null;
                        }
                    }
                    // The IV was the first item in the array, so all items have
                    // been shifted up once. Pop the last array element off.
                    data.points[i].pop();
                }

                // Flag the data as unencrypted and re-process the update.
                data.encrypted = false;
                processUpdate(data, init);
            })
            .catch(function(error) {
                // Decryption error. Most likely incorrect password. Reset the
                // key and prompt the user for the password again.
                console.log(error);
                aesKey = null;
                if (!init) {
                    clearInterval(fetchIntv);
                    clearInterval(countIntv);
                }
                processUpdate(data, true);
            });

        return;
    }

    // If flagged to initialize, set up polling.
    if (init) setNewInterval(data.expire, data.interval, data.serverTime);
    var userListE = document.getElementById("user-list");

    for (var user in users) {
        if (!users.hasOwnProperty(user)) continue;
        var locData = users[user];

        if (!shares.hasOwnProperty(user)) {
            // Add an entry to the user list.
            var listE = document.createElement("p");
            listE.textContent = user;
            listE.dataset.user = user;
            listE.style.display = "none";
            listE.addEventListener("click", function() {
                var userListPopupE = document.getElementById("user-list-popup");
                if (userListPopupE !== null && userDetailsE !== null) {
                    userListPopupE.style.display = "none";
                    userDetailsE.style.display = "block";
                    var user = this.dataset.user;
                    var userDetailsHeaderE = document.getElementById("user-details-header");
                    if (userDetailsHeaderE !== null) userDetailsHeaderE.textContent = user;
                    if (userDetailsFollowE !== null) userDetailsFollowE.dataset.user = user;
                    if (userDetailsNavigateE !== null) userDetailsNavigateE.dataset.user = user;
                }
            });
            if (userListE !== null) userListE.appendChild(listE);

            shares[user] = {
                "marker": null,
                "circle": null,
                "icon": null,
                "points": [],
                "id": Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 5),
                "listEntry": listE,
                "state": "live"
            };
        }

        // Get the last location received.
        var lastPoint = shares[user].points.length > 0 ? shares[user].points[shares[user].points.length - 1] : null;

        for (var i = 0; i < users[user].length; i++) {
            var lat = users[user][i][0];
            var lon = users[user][i][1];
            var time = users[user][i][2];
            var prov = users[user][i][3];
            var acc = users[user][i][4];
            var spd = users[user][i][5];

            // Default to "Fine" provider for older clients.
            if (prov === null) prov = LOC_PROVIDER_FINE;
            // Determine the icon color to use depending on provider.
            shares[user].state = prov == LOC_PROVIDER_FINE ? "live" : "rough";
            var iconColor = STATE_LIVE_COLOR;
            if (shares[user].state == "rough") iconColor = STATE_ROUGH_COLOR;

            // Check if the location should be added. Only add new location points
            // if the point was not recorded before the last recorded point.
            if (lastPoint === null || time > lastPoint.time) {
                var line = null;
                if (shares[user].marker == null) {
                    // Add a marker to the map if it's not already there.
                    shares[user].icon = L.divIcon({
                        html:
                            '<div class="marker">' +
                                '<div class="arrow still-' + shares[user].state + '" id="arrow-' + shares[user].id + '"></div>' +
                                '<p class="' + shares[user].state + '" id="label-' + shares[user].id + '">' +
                                    '<span id="nickname-' + shares[user].id + '"></span>' +
                                    '<span class="velocity">' +
                                        '<span id="velocity-' + shares[user].id + '">0.0</span> ' +
                                        VELOCITY_UNIT.unit +
                                    '</span><span class="offline" id="last-seen-' + shares[user].id + '">' +
                                    '</span>' +
                                '</p>' +
                            '</div>',
                        iconAnchor: [33, 18]
                    });
                    shares[user].marker = L.marker([lat, lon], {icon: shares[user].icon}).on("click", function() {
                        follow(this.haukUser);
                    });
                    shares[user].marker.haukUser = user;
                    shares[user].marker.addTo(markerLayer);
                } else {
                    // If there is a marker, draw a line from its last location
                    // instead and move the marker.
                    line = L.polyline([shares[user].marker.getLatLng(), [lat, lon]], {color: TRAIL_COLOR}).addTo(markerLayer);
                    shares[user].marker.setLatLng([lat, lon]);
                }
                // Draw an accuracy circle if GPS accuracy was provided by the
                // client.
                if (acc !== null && shares[user].circle == null) {
                    shares[user].circle = L.circle([lat, lon], {radius: acc, fillColor: iconColor, fillOpacity: 0.25, color: iconColor, opacity: 0.5, interactive: false}).addTo(circleLayer);
                } else if (shares[user].circle !== null) {
                    shares[user].circle.setLatLng([lat, lon]);
                    if (acc !== null) shares[user].circle.setRadius(acc);
                }
                shares[user].points.push({lat: lat, lon: lon, line: line, time: time, spd: spd, acc: acc});
                lastPoint = shares[user].points[shares[user].points.length - 1];
            }
        }

        if (lastPoint !== null) shares[user].listEntry.style.display = "block";

        var eVelocity = document.getElementById("velocity-" + shares[user].id)
        var vel = 0;
        if (lastPoint !== null && lastPoint.spd !== null && eVelocity !== null) {
            // Prefer client-provided speed if possible.
            vel = lastPoint.spd * VELOCITY_UNIT.mpsMultiplier;
            eVelocity.textContent = vel.toFixed(1);
        } else if (eVelocity !== null) {
            // If the client didn't provide its speed, calculate it locally from its
            // list of locations.
            var dist = 0;
            var time = 0;
            var idx = shares[user].points.length;

            // Iterate over all locations backwards until we either reach our
            // required VELOCITY_DELTA_TIME, or we run out of points.
            while (idx > 2) {
                idx--;
                var pt1 = shares[user].points[idx - 1];
                var pt2 = shares[user].points[idx];
                var dTime = pt2.time - pt1.time;

                // If the new time does not exceed the VELOCITY_DELTA_TIME, add the
                // time and distance deltas to the appropriate sum for averaging;
                // otherwise, break the loop and proceed to calculate.
                if (time + dTime <= VELOCITY_DELTA_TIME) {
                    time += dTime;
                    dist += distance(pt1, pt2);
                } else {
                    break;
                }
            }

            // Update the UI with the velocity.
            vel = velocity(dist, time);
            eVelocity.textContent = vel.toFixed(1);;
        }

        // Flag that the first location has been received, for map centering.
        if (lastPoint !== null && !hasReceivedFirst) {
            hasReceivedFirst = true;
        }

        // Move the marker if needed.
        if (lastPoint !== null && !multiUser && following !== null) {
            map.panTo([lastPoint.lat, lastPoint.lon]);
        } else if (lastPoint !== null && multiUser && following == shares[user].id) {
            map.panTo([lastPoint.lat, lastPoint.lon]);
        }

        // Rotate the marker to the direction of movement.
        var eArrow = document.getElementById("arrow-" + shares[user].id);
        if (eArrow !== null && shares[user].points.length >= 2) {
            var last = shares[user].points.length - 1;
            eArrow.style.transform = "rotate(" + angle(shares[user].points[last - 1], shares[user].points[last]) + "deg)";
            if (vel.toFixed(1) == "0.0") {
                eArrow.className = "arrow still-" + shares[user].state;
            } else {
                eArrow.className = "arrow moving-" + shares[user].state;
            }
        }

        // Prune the array of locations so it does not exceed our MAX_POINTS defined
        // in the config.
        if (shares[user].points.length > MAX_POINTS) {
            var remove = shares[user].points.splice(0, shares[user].points.length - MAX_POINTS);
            for (var j = 0; j < remove.length; j++) if (remove[j].line !== null) map.removeLayer(remove[j].line);
        }

        // Add the user's nickname if this is a group share.
        var nameE = document.getElementById("nickname-" + shares[user].id);
        if (nameE !== null && multiUser) {
            nameE.textContent = user;
            nameE.innerHTML += "<br />";
            nameE.style.fontWeight = "bold";
        }
    }

    for (var user in shares) {
        if (!shares.hasOwnProperty(user)) continue;

        // Gray out the user's location if no data has been received for the
        // OFFLINE_TIMEOUT.
        var eArrow = document.getElementById("arrow-" + shares[user].id);
        if (eArrow !== null) {
            var last = shares[user].points.length - 1;
            var point = shares[user].points[last];
            var eLabel = document.getElementById("label-" + shares[user].id);
            var eLastSeen = document.getElementById("last-seen-" + shares[user].id);

            if (point.time < data.serverTime - OFFLINE_TIMEOUT) {
                eArrow.className = eArrow.className.split("live").join("dead").split("rough").join("dead");
                if (eLabel !== null) eLabel.className = 'dead';
                if (eLastSeen !== null) {
                    // Calculate time since last update and choose an
                    // appropriate unit.
                    var time = Math.round(data.serverTime - point.time);
                    var unit = LANG["last_update_seconds"];
                    if (time >= 60) {
                        time = Math.floor(time / 60);
                        unit = LANG["last_update_minutes"];
                        if (time >= 60) {
                            time = Math.floor(time / 60);
                            unit = LANG["last_update_hours"];
                            if (time >= 24) {
                                time = Math.floor(time / 24);
                                unit = LANG["last_update_days"];
                            }
                        }
                    }
                    eLastSeen.textContent = unit.split("{{time}}").join(time);
                }
                setAccuracyCircleColor(shares[user].circle, STATE_DEAD_COLOR);
            } else {
                eArrow.className = eArrow.className.split("dead").join(shares[user].state);
                if (eLabel !== null) eLabel.className = shares[user].state;
                var iconColor = STATE_LIVE_COLOR;
                if (shares[user].state == "rough") iconColor = STATE_ROUGH_COLOR;
                setAccuracyCircleColor(shares[user].circle, iconColor);
            }
        }
    }

    // On first location update, center the map so it shows all participants.
    if (hasReceivedFirst && !hasInitiated) {
        hasInitiated = true;
        noGPS.style.display = "none";
        var mapOuterE = document.getElementById("mapouter");
        if (mapOuterE !== null) {
            mapOuterE.style.visibility = "visible";
        }
        autoCenter();

        // Auto-follow single-user shares.
        if (!multiUser) {
            following = true;
        }
    }
}

function setAccuracyCircleColor(circle, color) {
    if( circle ) {
        circle.setStyle({
            fillColor: color,
            color: color
        });
    }
}

// Calculates the distance between two points on a sphere using the Haversine
// algorithm.
function distance(from, to) {
    var d2r = Math.PI / 180;
    var fla = from.lat * d2r, tla = to.lat * d2r;
    var flo = from.lon * d2r, tlo = to.lon * d2r;
    var havLat = Math.sin((tla - fla) / 2); havLat *= havLat;
    var havLon = Math.sin((tlo - flo) / 2); havLon *= havLon;
    var hav = havLat + Math.cos(fla) * Math.cos(tla) * havLon;
    var d = Math.asin(Math.sqrt(hav));
    return d;
}

// Calculates a velocity using the velocity unit from the config.
function velocity(distance, intv) {
    if (intv == 0) return 0.0;
    return distance * VELOCITY_UNIT.mpsMultiplier * HAV_MOD / intv;
}

// Calculates the bearing between two points on a sphere in degrees.
function angle(from, to) {
    var d2r = Math.PI / 180;
    var fromLat = from.lat * d2r, toLat = to.lat * d2r;
    var fromLon = from.lon * d2r, toLon = to.lon * d2r;
    /*
        Calculation code by krishnar from
        https://stackoverflow.com/a/52079217
    */
    var x = Math.cos(fromLat) * Math.sin(toLat)
          - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(toLon - fromLon);
    var y = Math.sin(toLon - fromLon) * Math.cos(toLat);
    var heading = Math.atan2(y, x) / d2r;

    return (heading + 360) % 360;
}
