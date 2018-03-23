
// This is the function that is called when this lambda is called
exports.handler = (event, context, callback) => {
    
    // Reads the json data sent by Lex
    // The format of the data sent by Lex can be found @ https://docs.aws.amazon.com/lex/latest/dg/lambda-input-response-format.html
    const currentIntent = event.currentIntent;
    const name = currentIntent.name;
    
    // If the json data sent is the one we want (i.e. sent by Lex with intent CurrentTime)
    if (name == "CurrentTime") {
        const currentDate = new Date();
        
        // The callback function for when we want to send data back to Lex
        callback(null, {
            // The data sent back should be in json with the following format
            // The format of the data sent to Lex can be found @ https://docs.aws.amazon.com/lex/latest/dg/lambda-input-response-format.html
            dialogAction : {
                type: 'Close',
                fulfillmentState: 'Fulfilled',
                message: {
                    contentType: 'PlainText',
                    content: 'The current time is ' + currentDate.toLocaleString("en-AU", {timeZone: "Australia/Melbourne"})
                },
            }
        });
    }
};