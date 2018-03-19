const randomBytes = require('crypto').randomBytes;

const AWS = require('aws-sdk');

const ddb = new AWS.DynamoDB.DocumentClient();

exports.handler = (event, context, callback) => {
    
    if (event.hasOwnProperty('currentIntent')) {
        lexEvent(event, callback);
    } else {
        webAPIEvent(event, callback);
    }
};

function webAPIEvent(event, callback) {
    const requestBody = JSON.parse(event.body);
    const requestName = requestBody.Name;
    if (requestName == null) {
        requestName = 'null'
    }
    
    const id = toUrlString(randomBytes(4));
    
    addDdbItem(id, requestName).then(() => {
        callback(null, {
            statusCode: 201,
            body: JSON.stringify({
                yushengId: id,
                requestName: requestName
            }),
            headers: {
                'Access-Control-Allow-Origin': '*',
            },
        });
    });
}

function lexEvent(event, callback) {
    const currentIntent = event.currentIntent;
    const slots = currentIntent.slots;
    const name = slots.Name;
    
    const id = toUrlString(randomBytes(4));
    
    addDdbItem(id, name).then(() => {
        callback(null, {
            dialogAction : {
                type: 'Close',
                fulfillmentState: 'Fulfilled',
                message: {
                    contentType: 'PlainText',
                    content: 'Thanks, a request has been made under the name ' + name
                },
            }
        });
    });
    
}

function addDdbItem(id, name) {
    
    
    return ddb.put({
        TableName: 'yushengTable',
        Item: {
            yushengID: id,
            requestName: name
        },
    }).promise();
}

function toUrlString(buffer) {
    return buffer.toString('base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}