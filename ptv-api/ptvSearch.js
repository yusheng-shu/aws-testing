/*
Returns a curated output from the search API

Important:  Search term should be focused on address, station/stop, or route.
            DO NOT include entire sentences such as "Flinders station myki outlet", 
            instead search for "Flinders" and set categories to "["outlet","station"]" (string array)
            
The following are applicable categories and route types, these are the default order if order is not specified
Categories: stops, routes, outlets
Route types: train, tram, bus, vline, nightbus

Expected input:
{
    searchTerm: [Search string, if numeric or less than 3 characters, only routes are returned] REQUIRED
    categories: [Array of strings indicating which categories to include, order sensitive, default is all] OPTIONAL
    routeType: [Array of strings indicating which routes to include, order sensitive, default is all] OPTIONAL
}

{
    searchTerm: [Search string, if numeric or less than 3 characters, only routes are returned] REQUIRED
    latitude: [Float] REQUIRED
    longitude: [Float] REQUIRED
    maxDistance: [Float max radius from coordinate] REQUIRED
    categories: [Array of strings indicating which categories to include, order sensitive, default is all] OPTIONAL
    routeType: [Array of strings indicating which routes to include, order sensitive, default is all] OPTIONAL
}

Example:
{
    searchTerm: "Flinders",
    latitude: -37.81831
    longitude: 144.966965
    maxDistance: 100
    categories: [
        "stops"
    ]
}

Expected output:
{
    best: { [Best fitting result]
        sentence: [Short sentence summarizing the result] REQUIRED
        category: [String] REQUIRED
        routeType: [String only applicable if category is stop or route] OPTIONAL
        latitude: [Float only applicable if request gave coordinates] OPTIONAL
        longitude: [Float only applicable if request gave coordinates] OPTIONAL
        distsance: [Float only applicable if request gave coordinates] OPTIONAL
    },
    other: { [Other results that fit the criteria]
        stops: [
            {
                stop_distance: 
                stop_name: 
                stop_id:
                route_type: 
                stop_latitude: 
                stop_longtitude: 
            }
        ],
        routes: [
            {
                route_name:
                route_number:
                route_type:
                route_id:
            }
        ],
        outlets: [
            {
                outlet_distance: 
                outlet_name: 
                outlet_buisness: 
                outlet_latitude: 
                outlet_longtitude: 
                outlet_suburb:
            }
        ]
    }
}

Example:
{
    best: {
        sentence: "Were you looking for Flinders Street Station about 3 metres away?",
        category: "stops",
        routeType: "train",
        latitude: -37.81831,
        longitude: 144.966965,
        distance: 2.68296123
    },
    other: {
        stops: [
            {
                stop_distance: 2.68296123,
                stop_name: "Flinders Street Railway Station",
                stop_id: 1071,
                route_type: 3,
                stop_latitude: -37.81831,
                stop_longtitude: 144.966965
            },
            {
                stop_distance: 82.5356,
                stop_name: "Swanston St/Flinders St #5",
                stop_id: 2096,
                route_type: 1,
                stop_latitude: -37.81757,
                stop_longtitude: 144.966934
            },
        ]
    }
}

Error output:
{
    error: [Error message] REQUIRED
}
*/

const SIG_GEN_FUNC = "ptvSigGen";

const SEARCH_ENDPOINT = "/v3/search/";
const SEARCH_LAT = "&latitude=";
const SEARCH_LONG = "&longitude=";
const SEARCH_DIST = "&max_distance=";
const SEARCH_ROUTE_TYPE = "&route_types=";
const CATEGORIES = ["stops", "routes", "outlets"];
const ROUTE_TYPES = ["train", "tram", "bus", "vline", "nightbus"];

var isGateway = false;

exports.handler = (event, context, callback) => {
    if (event.body != null) {
        event = JSON.parse(event.body);
        isGateway = true;
    }
    
    if (!validateInput(event, callback)) {
        return;
    }
    
    var request = SEARCH_ENDPOINT + event.searchTerm + '?';
    if (event.latitude != null) {
        request +=  SEARCH_LAT + event.latitude + SEARCH_LONG + event.longitude + SEARCH_DIST + event.maxDistance;
    }
    if (event.routeType != null) {
        for (var i = 0; i < event.routeType.length; i++) {
            request += SEARCH_ROUTE_TYPE + ROUTE_TYPES.indexOf(event.routeType[i]);
        }
    }
    
    console.log("PTV request url: " + request);
    getRequestUrl(request, event, callback);
};

// Validates request input
function validateInput(event, callback) {
    // Must have search term
    if (event.searchTerm == null) {
        errorCallback(callback, "You did not provide a search term string");
        return false;
    }
    
    event.searchTerm = event.searchTerm.replace(/\s/g, "%20");
    
    // All coordinates must be present, unless none is present
    const hasLat = event.longitude != null;
    const hasLong = event.latitude != null;
    const hasDist = event.maxDistance != null;
    if (hasLat || hasLong || hasDist) {
        if (!hasLat || !hasLong || !hasDist) {
            errorCallback(callback, "You must provide longitude, latitude, and maxDistance together");
            return false;
        }
        
        if (hasDist && event.maxDistance <= 0) {
            errorCallback(callback, "You must provide a postive non zero maxDistance");
            return false;
        }
    }
    
    // Valid categories
    if (event.categories != null) {
        if (!Array.isArray(event.categories)) {
            errorCallback(callback, "You provided an categories object, but it is not an array");
            return false;
        }
        
        for (var i = 0; i < event.categories.length; i++) {
            if (!CATEGORIES.includes(event.categories[i])) {
                errorCallback(callback, "You provided a category of " + event.categories[i] + " that does not exist");
                return false;
            }
        }
    } 
    // Default include all categories
    else if (event.categories == null || event.categories.length == 0) {
        event.categories = CATEGORIES;
    }
    
    // Valide route types
    if (event.routeType != null) {
        if (!Array.isArray(event.routeType)) {
            errorCallback(callback, "You provided an route type object, but it is not an array");
            return false;
        }
        
        for (var i = 0; i < event.routeType.length; i++) {
            if (!ROUTE_TYPES.includes(event.routeType[i])) {
                errorCallback(callback, "You provided a route type of " + event.routeType[i] + " that does not exist");
                return false;
            }
        }
    } 
    // Default include all route types
    else if (event.routeType == null || event.routeType.length == 0) {
        event.routeType = ROUTE_TYPES;
    }
    
    return true;
}

// Gets ptv api url from sig gen lambda
function getRequestUrl(request, event, callback) {
    const aws = require("aws-sdk");
    const lambda = new aws.Lambda({
       region: "us-east-1" 
    });
    lambda.invoke({
        FunctionName: SIG_GEN_FUNC,
        Payload: JSON.stringify({
            request: request
        })
    },
    function (err, data) {
        if (err) {
            errorCallback(callback, "Error generating URL for request, error response from invoking " + SIG_GEN_FUNC);
        }
        const url = JSON.parse(data.Payload).url;
        if (url == null) {
            errorCallback(callback, "Error generating URL for request, no url returned from " + SIG_GEN_FUNC);
        }
        searchAPI(url, event, callback);
    });
}

// Calls the ptv api
function searchAPI(requestUrl, event, callback) {
    const http = require("http");
    http.get(requestUrl,
    function (res) {
        
        if (res.statusCode != "200") {
            errorCallback(callback, "Error response from PTV, status code: " + res.statusCode);
            return;
        }
        
        var body = '';
        // Callback as the data is being read
        res.on("data", function (data) {
           body += data; 
        });
        
        // Callback when data read is complete
        res.on("end", function () {
            console.log("Response from PTV: " + body);
            parseResponse(JSON.parse(body), event, callback);
        });
        
    }).on("error", 
    function (e) {
        console.log("Failed to retreive data from url, error: " + e.message);
    });
}

// Parses response from ptv
function parseResponse(json, event, callback) {
    // Sorts results by distance if applicable
    if (event.latitude != null) {
        
        // Make sure sorting order respects specified route type order
        var routeTypeIds = [];
        for (var i = 0; i < event.routeType.length; i++) {
            routeTypeIds.push(ROUTE_TYPES.indexOf(event.routeType[i]));
        }
        json.stops.sort(function(a, b) {
           if (a.route_type != b.route_type) {
               return routeTypeIds.indexOf(a.route_type) - routeTypeIds.indexOf(b.route_type);
           } else {
               return a.stop_distance - b.stop_distance;
           }
        });
        
        // Sorting outlets
        json.outlets.sort(function(a, b) {
            return a.outlet_distance - b.outlet_distance;
        });
    }
    
    var other = {}; // All applicable results (for now)
    
    // Add specified categories to results
    for (var i = 0; i < event.categories.length; i++) {
        other[event.categories[i]] = json[event.categories[i]];
    }
    
    // Remove unspecified route types from results
    // Checks the route_type id against the index of ROUTE_TYPE (e.g. "train" in ROUTE_TYPE is 0, thus route_type: 0 == "train")
    // NOT IN USE, USING PTV API FILTERING INSTEAD
    /*
    if (other.stops != null) {
        other.stops = other.stops.filter(function (stop) {
            return event.routeType.indexOf(ROUTE_TYPES[stop.route_type]) != -1;
        });
    }
    if (other.routes != null) {
        other.routes = other.routes.filter(function (route) {
            return event.routeType.indexOf(ROUTE_TYPES[route.route_type]) != -1;
        });
    }
    */
    
    var best = null; // Candidate for best result
    
    // Get the best result
    for (var i = 0; i < event.categories.length; i++) {
        if (other[event.categories[i]] == null || other[event.categories[i]].length == 0) {
            continue;
        }
        const candidate = other[event.categories[i]][0];
        best = parseBestResult(event, candidate, event.categories[i]);
        
        // Remove best result from other results
        other[event.categories[i]].shift();
        break;
    }
    
    if (best == null) {
        errorCallback(callback, "There were no applicable results from PTV.");
    }
    
    // Return results
    if (isGateway) {
        callback(null, {
            statusCode: 200,
            body: JSON.stringify({
                best: best,
                other: other,
            }),
            headers: {
                'Access-Control-Allow-Origin': '*',
            }
        });
    } else {
        callback(null, {
            best: best,
            other: other
        });
    }
}

// Formats best result
function parseBestResult(event, candidate, category) {
    var best = null;
    if (category == "stops") {
        var sentence = "Were you looking for the " + ROUTE_TYPES[candidate.route_type] + " stop: " + candidate.stop_name.trim() + '?';
        if (event.latitude != null) {
            sentence += " It's about " + candidate.stop_distance.toFixed(0) + "m " 
            + getSimpleBearing(event.latitude, event.longitude, candidate.stop_latitude, candidate.stop_longitude)
            + " of here.";
        }
        
        best = {
            sentence: sentence,
            category: category,
            routeType: ROUTE_TYPES[candidate.route_type],
            latitude: candidate.stop_latitude,
            longitude: candidate.stop_longitude,
            distsance: candidate.stop_distance
        }
    } else if (category == "routes") {
        best = {
            sentence: "Were you looking for the " + ROUTE_TYPES[candidate.route_type] + " route:  " + candidate.route_name.trim() + '?',
            category: category,
            routeType: ROUTE_TYPES[candidate.route_type]
        }
    } else if (category == "outlets") {
        var sentence = "Were you looking for the outlet at: " + candidate.outlet_name.trim() + '?';
        if (event.latitude != null) {
            sentence += " It's about " + candidate.outlet_distance.toFixed(0) + "m " 
            + getSimpleBearing(event.latitude, event.longitude, candidate.outlet_latitude, candidate.outlet_longitude)
            + " of here.";
        }
        
        best = {
            sentence: sentence,
            category: category,
            latitude: candidate.outlet_latitude,
            longitude: candidate.outlet_longitude,
            distsance: candidate.outlet_distance
        }
    }
    return best;
    
}

// Returns simple compass bearing (e.g North, North East, East, South East ... etc.)
function getSimpleBearing(userLat, userLong, destLat, destLong) {
    var ult = toRadians(userLat);
    var uln = toRadians(userLong);
    var dlt = toRadians(destLat);
    var dln = toRadians(destLong);
    var x = Math.cos(dlt) * Math.sin(dln - uln);
    var y = Math.cos(ult) * Math.sin(dlt) - Math.sin(ult) * Math.cos(dlt) * Math.cos(dln - uln);
    var bearing = (Math.atan2(x, y) * (180/Math.PI) + 360) % 360;
    
    const compass = [ "North East", "East", "South East", "South", "South West", "West", "North West", "North" ];
    const step = 45;
    
    for (var i = 0, c = 22.5; i < compass.length; i++, c+=step) {
        if (bearing >= c && bearing < c + step) {
            return compass[i];
        }
    }
    return compass[7];
}

function toRadians (angle) {
  return angle * (Math.PI / 180);
}

function errorCallback(callback, msg) {
    callback(null, {
        error: msg
    });
}