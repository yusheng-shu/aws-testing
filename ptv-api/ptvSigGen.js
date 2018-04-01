/* 
Returns a the full API request URL with required signature

Expected input:
{
    request: [API request and parameters without base URL] REQUIRED
}
Example:
{
    request: "/v3/search/50%20Queen%20Street?include_outlets=true"
}

Expected output:
{
    url: [Full API call URL] REQUIRED
}
Example:
{
    url: "http://timetableapi.ptv.vic.gov.au/v3/search/50%20Queen%20Street?include_outlets=true&devid=3000569&signature=391E73D5B18BB165EBB439F020A11A5BA1D50FE9"
}

Error output:
{
    error: [Error message] REQUIRED
}
*/

const API_ENDPOINT = "http://timetableapi.ptv.vic.gov.au";
const PTV_ID = "3000569";
const PTV_KEY = "22216f43-d43e-4c85-b01f-ac5cab7bfaaf";

exports.handler = (event, context, callback) => {
    
    if (event.request == null) {
        callback(null, {
            error: "Error, you did not provide a request string"
        });
    }
    
    // Make sure the request starts with a '/'
    const head  = (event.request.charAt(0) == '/') ? '' : '/';
    // Make sure the request ends with a '&'
    const tail = (event.request.charAt(-1) == '&') ? '' : '&';
    // Full request with dev id
    const request = head + event.request + tail + "devid=" + PTV_ID;
    
    // Get HMACSHA1
    const crypto = require("crypto");
    const signature = crypto.createHmac("sha1", PTV_KEY).update(request).digest("hex");
    
    callback(null, {
        url: API_ENDPOINT + request + "&signature=" + signature
    });
    
};