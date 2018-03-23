const AWS = require('aws-sdk');

const ddb = new AWS.DynamoDB.DocumentClient();

const params = {
    TableName: 'yushengTable',
}

exports.handler = (event, context, callback) => {
    ddb.scan(params, function(err, data) {
        if (err) {
            callback(null, {
               statusCode: 201,
            body: err.stack,
            headers: {
                'Access-Control-Allow-Origin': '*',
            }
           });
        } else {
           callback(null, {
               statusCode: 201,
            body: JSON.stringify(data),
            headers: {
                'Access-Control-Allow-Origin': '*',
            }
           });
       } 
    });
};