/*
Returns a curated output from the search API

Important:  Search term should be focused on address, station/stop, or route.
            DO NOT include entire sentences such as "Flinders station myki outlet", 
            instead search for "Flinders" and set categories to "["outlet","station"]" (string array)
            
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
        sentence: [Short sentence summarizing the result]
        category: [String]
        routeType: [String]
        latitude: [Float]
        longitude: [Float]
        distsance: [Only applicable if request gave coordinates]
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
const SEARCH_DIST = "&max_distance="
const CATEGORIES = ["stops", "routes", "outlets"];
const ROUTE_TYPES = ["train", "tram", "bus", "vline", "nightbus"];

exports.handler = (event, context, callback) => {
    if (!validateInput(event, callback)) {
        return;
    }
    
    var request = SEARCH_ENDPOINT + event.searchTerm + '?';
    if (event.latitude != null) {
        request +=  SEARCH_LAT + event.latitude + SEARCH_LONG + event.longitude + SEARCH_DIST + event.maxDistance;
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
        json.stops.sort(function(a, b) {
            return a.stop_distance - b.stop_distance;
        });
        json.outlets.sort(function(a, b) {
            return a.outlet_distance = b.outlet_distance;
        });
    }
    
    var other = {}; // All applicable results (for now)
    
    // Add specified categories to results
    for (var i = 0; i < event.categories.length; i++) {
        other[event.categories[i]] = json[event.categories[i]];
    }
    
    // Remove unspecified route types from results
    // Checks the route_type id against the index of ROUTE_TYPE (e.g. "train" in ROUTE_TYPE is 0, thus route_type: 0 == "train")
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
    
    console.log(other);
    callback(null, null);
}

function errorCallback(callback, msg) {
    callback(null, {
        error: msg
    });
}