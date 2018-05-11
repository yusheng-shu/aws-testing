'use strict';

const aws = require('aws-sdk');
const fs = require('fs');
const JSZip = require('jszip');

exports.handler = (event, context, callback) => {
    if (event.RequestType == null) {
        console.log('No request type detected, are you calling from cloudformation custom resource?');
        cfnResponse(event, context, "FAILED", { Error: 'No request type detected, are you calling from cloudformation custom resource?' });
        return;
    }
    
    if (event.RequestType == 'Create') {
        createBot(event, context, callback);
        return;
    } else if (event.RequestType == 'Delete') {
        deleteBot(event, context, callback);
        return;
    } else {
        console.log('Unsupported request type of  ' + event.RequestType);
        cfnResponse(event, context, "FAILED", { Error: 'Unsupported request type of  ' + event.RequestType });
        return;
    }
};

function createBot(event, context, callback) {
    const lexmodelbuildingservice = new aws.LexModelBuildingService();
    
    // Get the lex bot json template lexbot.json from environment filesystem
    var lexbotjson;
    try {
        lexbotjson = fs.readFileSync(__dirname +'/lexbot.json', 'utf-8');
    } catch (err) {
        console.log(err);
        cfnResponse(event, context, "FAILED", { Error: 'File lexbot.json does not exist' });
        return;
    }
    
    // Set lex bot name to what cloudfront sent LexBotName
    var lexbot = JSON.parse(lexbotjson);
    lexbot.resource.name = event.ResourceProperties.LexBotName;
    
    // Create a zip file containing file lexbot.json with contents of lexbot (with newly set lex bot name)
    var zip = new JSZip();
    zip.file('lexbot.json', JSON.stringify(lexbot));
    
    // Start promise chain
    // Generate the zip file as a buffer
    zip.generateAsync({
        type: "nodebuffer"
    })
    
    // Import into Lex
    .then(function (zipBuffer) {
        const params = {
            mergeStrategy: 'FAIL_ON_CONFLICT',
            payload: zipBuffer,
            resourceType: 'BOT'
        };
        return lexmodelbuildingservice.startImport(params).promise();
    })
    
    .then(function (response) {
        console.log(response);
        cfnResponse(event, context, "SUCCESS", { FinalLexBotName: lexbot.resource.name });
    })
    
    .catch(function (err) {
        console.log(err);
        cfnResponse(event, context, "FAILED", { Error: 'Could not create Lex Bot ' + err });
    });
    // End promise chain
}

function deleteBot(event, context, callback) {
    const lexmodelbuildingservice = new aws.LexModelBuildingService();
    var lexbot;
    
    // Start promise chain
    Promise.resolve()
    // Get existing bot
    .then(function (data) {
        const params = {
            name: event.ResourceProperties.LexBotName, 
            versionOrAlias: "$LATEST"
        };
        return lexmodelbuildingservice.getBot(params).promise();
    })
    // Save existing bot data, then delete bot (alias for bot currently unsupported)
    .then(function (response) {
        lexbot = response;
        const params = {
            name: event.ResourceProperties.LexBotName
        };
        return lexmodelbuildingservice.deleteBot(params).promise();
    })
    // Delete all intents used by bot
    .then(function (response) {
        var deletePromises = [];
        
        for (var i = 0; i < lexbot.intents.length; i++) {
            var deleteFunction = () => createDeletePromise(lexbot.intents[i].intentName, i * 2000, lexmodelbuildingservice);
            deletePromises.push(deleteFunction);
        }
        
        Promise.all(deletePromises)
        .then(function (response) {
            console.log(response);
            cfnResponse(event, context, "SUCCESS", { FinalLexBotName: lexbot.resource.name });
            return;
        })
        .catch(function (err) {
            console.log(err);
            cfnResponse(event, context, "FAILED", { Error: 'Could not delete Lex Bot ' + err });
            return;
        });
    })
    
    .catch(function (err) {
        console.log(err);
        cfnResponse(event, context, "FAILED", { Error: 'Could not delete Lex Bot ' + err });
    });
    // End promise chain
}

function createDeletePromise(intentName, delay, lexmodelbuildingservice) {
    return new Promise(function (resolve, reject) {
      	setTimeout(function() {
            lexmodelbuildingservice.deleteIntent({ name: intentName }).promise()
            .then(function (response) {
                console.log(response);
                resolve();
            })
            .catch(function (err) {
                console.log(err);
                cfnResponse(event, context, "FAILED", { Error: 'Could not delete Lex Bot ' + err });
                resolve();
            });
          }, delay);
  });
}

function cfnResponse(event, context, responseStatus, responseData, physicalResourceId, noEcho) {

    var responseBody = JSON.stringify({
        Status: responseStatus,
        Reason: "See the details in CloudWatch Log Stream: " + context.logStreamName,
        PhysicalResourceId: physicalResourceId || context.logStreamName,
        StackId: event.StackId,
        RequestId: event.RequestId,
        LogicalResourceId: event.LogicalResourceId,
        NoEcho: noEcho || false,
        Data: responseData
    });

    console.log("Response body:\n", responseBody);

    var https = require("https");
    var url = require("url");

    var parsedUrl = url.parse(event.ResponseURL);
    var options = {
        hostname: parsedUrl.hostname,
        port: 443,
        path: parsedUrl.path,
        method: "PUT",
        headers: {
            "content-type": "",
            "content-length": responseBody.length
        }
    };

    var request = https.request(options, function(response) {
        console.log("Status code: " + response.statusCode);
        console.log("Status message: " + response.statusMessage);
        context.done();
    });

    request.on("error", function(error) {
        console.log("send(..) failed executing https.request(..): " + error);
        context.done();
    });

    request.write(responseBody);
    request.end();
}
