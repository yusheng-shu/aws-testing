const CLOSE = 'Close';
const FAILED = 'Failed';
const FULLFILLED = 'Fulfilled';

// Intents
const INTENT_GREET = "Greet";
const INTENT_HOW_TO_TRAVEL = 'HowToTravel';
const INTENT_ABOUT_MYKI = 'AboutMyki';
const INTENT_BUY_MYKI = 'BuyMyki';
const INTENT_TOPUP_MYKI = 'TopUpMyki';
const INTENT_TOUCH_ONOFF_MYKI = 'TouchOnOffMyki';

// Response messeges
const NO_RESPONSE = "Sorry, but I can't help you with that. [No Lambda Case]";
const RESPONSE_GREET = "Hi, I'm PTV Bot, ask my anything about myki. (e.g. What is myki?).";
const RESPONSE_HOW_TO_TRAVEL = "Melbourne¡¯s trains, trams and buses are an easy way find your way around. " + 
    "All you need is a myki card and you¡¯ll be ready to travel around the city.";
const RESPONSE_ABOUT_MYKI = "myki is Melbourne¡¯s ticket to travel on the city¡¯s trains, trams and buses. " +
    "Remeber to top up before you travel, and touch on and touch off as you enter the paid area of a train station or board a tram or bus.";
const RESPONSE_BUY_MYKI = "You can buy a myki at an authorized retail outlet or PTV Hub; " + 
    "at any myki machines situated near some stations; at a premium station ticket office; " + 
    "on board a bus; or via online or via the call centre.";
const RESPONSE_TOPUP_MYKI = "Once you've bought your myki card, you need to top it up to travel. " + 
    "You can either top up with myki money, which is pay as you travel. " + 
    "Or, you can top up with myki pass, which gives you unlimited travel over a period of time. " +
    "You can top up anywhere you can buy a myki from.";
const RESPONSE_TOUCH_ONOFF_MYKI = "Touch on and off at a myki reader every time you use public transport. " +
    "You will have to touch on/off when you enter/leave a train station, bus, or tram.";
    
// Urls
const MYKI_IMG = "https://static.ptv.vic.gov.au/Images/Ticketing/1509066806/myki-page.gif";
const VISITING_URL = "https://www.ptv.vic.gov.au/getting-around/visiting-melbourne/";
const MYKI_URL = "https://www.ptv.vic.gov.au/tickets/myki/";
const BUY_MYKI_URL = "https://www.ptv.vic.gov.au/tickets/myki/buy-a-myki/";
const USE_MYKI_URL = "https://www.ptv.vic.gov.au/tickets/myki/how-to-use-myki/";
const TOPUP_URL = "https://www.ptv.vic.gov.au/tickets/myki/top-up-a-myki/";

// Button class
class Button {
    constructor(text, value) {
        this.text = text; // Text displayed to user
        this.value = value; // Text automaticly sent to Lex when button is pressed
    }
    
    get json() {
        /* 
        Button json format
        {
            text:
            value:
        }
        */
        return {
            text: this.text,
            value: this.value
        };
    }
}

// Card class
class Card {
    constructor(title, subTitle, imageUrl, attachmentLinkUrl, buttons) {
        this.title = title;
        this.subTitle = subTitle;
        this.imageUrl = imageUrl;
        this.attachmentLinkUrl = attachmentLinkUrl;
        this.buttons = buttons; // Array of buttons
    }
    
    get json() {
        /*
        Card json format
        {
            title: 
            subTitle: 
            imageUrl: 
            attachementLinkUrl: 
            buttons: [
                {
                    text: 
                    value: 
                }
            ]
        }
        */
        return {
            title: this.title,
            subTitle: this.subTitle,
            imageUrl: this.imageUrl,
            attachmentLinkUrl: this.attachmentLinkUrl,
            buttons: this.buttons
        };
    }
}

// Response class
class Response {
    
    constructor(intent, type, state, messege, card) {
        this.intent = intent; // Intent in Lex
        this.type = type;
        this.state = state;
        this.messege = messege;
        this.card = card;
        
    }
    
    get json() {
        /*
        Response json format
        {
            dialogAction : {
                type:
                fulfillmentState: 
                message: {
                    contentType: 'PlainText',
                    content: 
                },
                responseCard: { // Don't include this if there is no card! (set to null is fine)
                    version: 1,
                    contentType: "application/vnd.amazonaws.card.generic",
                    genericAttachments: [
                        {
                            title: 
                            subTitle: 
                            imageUrl: 
                            attachementLinkUrl: 
                            buttons: [
                                {
                                    text: 
                                    value: 
                                }
                            ]
                        }
                    ] 
                }
            }
        }
        */
        var responseCard = null;
        if (this.card != null)
            responseCard = {
                version: 1,
                contentType: "application/vnd.amazonaws.card.generic",
                genericAttachments: [
                    this.card 
                ] 
            };
        
        return {
            dialogAction : {
                type: this.type,
                fulfillmentState: this.state,
                message: {
                    contentType: 'PlainText',
                    content: this.messege
                },
                responseCard: responseCard
                
            }
        };
    }
}

const greeting = new Response(INTENT_GREET, CLOSE, FULLFILLED, RESPONSE_GREET, null);

const howToTravel = new Response(INTENT_HOW_TO_TRAVEL, CLOSE, FULLFILLED, RESPONSE_HOW_TO_TRAVEL, 
    new Card("Traveling in Melbourne", "Additional information", MYKI_IMG, VISITING_URL, 
        [ 
            new Button("What is myki?", "What is Myki.") 
        ]
    )
);

const aboutMyki = new Response(INTENT_ABOUT_MYKI, CLOSE, FULLFILLED, RESPONSE_ABOUT_MYKI, 
    new Card("About myki", "Using myki", MYKI_IMG, MYKI_URL, 
        [ 
            new Button("Where to buy myki?", "How do I get a myki card."),
            new Button("How to recharge myki?", "How do I recharge myki."),
            new Button("How to use myki?", "How do I use myki.")
        ]
    )
);

const buyMyki = new Response(INTENT_BUY_MYKI, CLOSE, FULLFILLED, RESPONSE_BUY_MYKI,
    new Card("Getting myki", "How to get your myki", MYKI_IMG, BUY_MYKI_URL, 
        [
            new Button("How to recharge myki?", "How do I recharge myki."),
            new Button("How to use myki?", "How do I use myki.")
        ]
    )
);

const useMyki = new Response(INTENT_TOUCH_ONOFF_MYKI, CLOSE, FULLFILLED, RESPONSE_TOUCH_ONOFF_MYKI,
    new Card("Using myki", "How to use your myki", MYKI_IMG, USE_MYKI_URL, null)
);

const rechargeMyki = new Response(INTENT_TOPUP_MYKI, CLOSE, FULLFILLED, RESPONSE_TOPUP_MYKI, 
    new Card("Recharge myki", "How to recharge your myki", MYKI_IMG, TOPUP_URL, null)
);

// Array of responses
const responseArray = [ greeting, howToTravel, aboutMyki, buyMyki, useMyki, rechargeMyki ];

exports.handler = (event, context, callback) => {
    
    const currentIntent = event.currentIntent;
    const name = currentIntent.name;
    
    var finalResponse;
    
    // Check if we have the response for the intent from Lex
    for (var i = 0; i < responseArray.length; i++)
        if (responseArray[i].intent == name)
            finalResponse = responseArray[i];
    
    // Default fail state
    if (finalResponse == null)
        finalResponse = new Response("", CLOSE, FAILED, NO_RESPONSE, null);
    
    callback(null, finalResponse.json);
};