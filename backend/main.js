// This is the main script file for Hauk's web view client.

// Create a Leaflet map.
var map = L.map('map').setView([0, 0], DEFAULT_ZOOM);
L.tileLayer(TILE_URI, {
    attribution: ATTRIBUTION,
    maxZoom: MAX_ZOOM
}).addTo(map);

var circleLayer = L.layerGroup().addTo(map);
var markerLayer = L.layerGroup().addTo(map);

// A list of points received from the server.
var points = [];

// The leaflet marker.
var marker = null;
var icon = null;
var circle = null;

// Retrieve the sharing link ID from the URL. E.g.
// https://example.com/?ABCD-1234 --> "ABCD-1234"
var id = location.href.substr(location.href.indexOf("?") + 1);
if (id.indexOf("&") !== -1) id = id.substr(0, id.indexOf("&"));

function getJSON(url, callback, invalid) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.onreadystatechange = function() {
        if (this.readyState == 4 && this.status === 200) {
            try {
                var json = JSON.parse(this.responseText);
                callback(json);
            } catch (ex) {
                console.log(ex);
                invalid();
            }
        }
    }
    xhr.send();
}

document.getElementById("dismiss").addEventListener("click", function() {
    document.getElementById("expired").style.display = "none";
});

// Attempt to fetch location data from the server once.
getJSON("./api/fetch.php?id=" + id, function(data) {
    document.getElementById("mapouter").style.visibility = "visible";

    // The location data contains an interval. Schedule a task that fetches data
    // once per interval time.
    var interval = setInterval(function() {
        // Stop the task if the share has expired.
        if ((Date.now() / 1000) >= data.x) clearInterval(interval);

        getJSON("./api/fetch.php?id=" + id, function(data) {
            processUpdate(data);
        }, function() {
            clearInterval(interval);
            document.getElementById("expired").style.display = "block";
        });
    }, data.i * 1000);
    processUpdate(data);
}, function() {
    document.getElementById("notfound").style.display = "block";
});

// Parses the data returned from ./api/fetch.php and updates the map marker.
function processUpdate(data) {
    // Get the last location received.
    var lastPoint = points.length > 0 ? points[points.length - 1] : null;

    for (var i = 0; i < data.l.length; i++) {
        var lat = data.l[i][0];
        var lon = data.l[i][1];
        var time = data.l[i][2];
        var acc = data.l[i][3];
        var spd = data.l[i][4];

        // Check if the location should be added. Only add new location points
        // if the point was not recorded before the last recorded point.
        if (lastPoint === null || time > lastPoint.time) {
            var line = null;
            if (marker == null) {
                // Add a marker to the map if it's not already there.
                icon = L.divIcon({
                    html: '<div id="marker"><div id="arrow"></div><p><span id="velocity">0.0</span> ' + VELOCITY_UNIT.unit + '</p></div>',
                    iconAnchor: [33, 18]
                });
                marker = L.marker([lat, lon], {icon: icon, interactive: false}).addTo(markerLayer);
            } else {
                // If there is a marker, draw a line from its last location
                // instead and move the marker.
                line = L.polyline([marker.getLatLng(), [lat, lon]], {color: TRAIL_COLOR}).addTo(markerLayer);
                marker.setLatLng([lat, lon]);
            }
            // Draw an accuracy circle if GPS accuracy was provided by the
            // client.
            if (acc !== null && circle == null) {
                circle = L.circle([lat, lon], {radius: acc, fillColor: '#d80037', fillOpacity: 0.25, color: '#d80037', opacity: 0.5, interactive: false}).addTo(circleLayer);
            } else if (circle !== null) {
                circle.setLatLng([lat, lon]);
                if (acc !== null) circle.setRadius(acc);
            }
            points.push({lat: lat, lon: lon, line: line, time: time, spd: spd, acc: acc});
            lastPoint = points[points.length - 1];
        }
    }

    var eVelocity = document.getElementById("velocity")
    if (lastPoint !== null && lastPoint.spd !== null && eVelocity !== null) {
        // Prefer client-provided speed if possible.
        eVelocity.textContent = (lastPoint.spd * VELOCITY_UNIT.mpsMod).toFixed(1);
    } else if (eVelocity !== null) {
        // If the client didn't provide its speed, calculate it locally from its
        // list of locations.
        var dist = 0;
        var time = 0;
        var idx = points.length;

        // Iterate over all locations backwards until we either reach our
        // required VELOCITY_DELTA_TIME, or we run out of points.
        while (idx > 2) {
            idx--;
            var pt1 = points[idx - 1];
            var pt2 = points[idx];
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
        eVelocity.textContent = velocity(dist, time);
    }


    // Follow the marker.
    if (lastPoint !== null) map.panTo([lastPoint.lat, lastPoint.lon]);

    // Rotate the marker to the direction of movement.
    var eArrow = document.getElementById("arrow");
    if (eArrow !== null && points.length >= 2) {
        var last = points.length - 1;
        eArrow.style.transform = "rotate(" + angle(points[last - 1], points[last]) + "deg)";
    }

    // Prune the array of locations so it does not exceed our MAX_POINTS defined
    // in the config.
    if (points.length > MAX_POINTS) {
        var remove = points.splice(0, points.length - MAX_POINTS);
        for (var j = 0; j < remove.length; j++) if (remove[j].line !== null) map.removeLayer(remove[j].line);
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
    if (intv == 0) return "0.0";
    return (distance * VELOCITY_UNIT.havMod / intv).toFixed(1);
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
