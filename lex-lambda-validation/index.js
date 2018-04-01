const INTENT_NAME = "AskNickname";
const TYPE_CLOSE = 'Close';
const STATE_FAIL = 'Failed';
const STATE_FULFILLED = 'Fulfilled';

const SLOT_NICKNAME = "nickname";
const EXISTING_NICKNAME = "Nicky";
const EXIT_CMD = "Goodbye";

// Response format for fulfillment
class CloseResponse {
    constructor(type, state, messege) {
        this.type = type;
        this.state = state;
        this.messege = messege;
    }
    
    get json() {
        return {
            dialogAction : {
                type: this.type,
                fulfillmentState: this.state,
                message: {
                    contentType: 'PlainText',
                    content: this.messege
                }
            }
        };
    }
}

// Response format for elicit slot request
class ElicitSlotResponse {
    constructor(intentName, elicitSlot, slots) {
        this.intentName = intentName;
        this.elicitSlot = elicitSlot;
        this.slots = slots;
    }
    
    get json() {
        return {
            dialogAction : {
                type: 'ElicitSlot',
                intentName: this.intentName,
                slots: this.slots,
                slotToElicit: this.elicitSlot
            }
        }
    }
}

// Response format for confirmation request
class ConfirmResponse {
    constructor(intentName, slots, message) {
        this.intentName = intentName;
        this.slots = slots;
        this.message = message;
    }
    
    get json() {
        return {
            dialogAction : {
                type: 'ConfirmIntent',
                intentName: this.intentName,
                slots: this.slots,
                message: {
                    contentType: 'PlainText',
                    content: this.message
                }
            }
        }
    }
}

exports.handler = (event, context, callback) => {
    const currentIntent = event.currentIntent;
    const intentName = currentIntent.name;
    
    const defaultResponse = {
        dialogAction : {
            type: 'Close',
            fulfillmentState: 'Failed',
            message: {
                contentType: 'PlainText',
                content: 'Lambda error. Goodbye'
            }
        }
    };
    
    if (INTENT_NAME != intentName) {
        callback(null, defaultResponse);
    }
    
    // Check if there is a slot value
    const slots = currentIntent.slots;
    const slotsExist = checkSlotExist(slots);
    
    // Check if it is existing value
    const valueExisting = checkExistingValue(slots.nickname);
    
    // Check if confirmation value exists
    const confirmExist = checkConfirmation(currentIntent.confirmationStatus);
    // Check what is the confirmation value
    const confirmed = confirmExist && (currentIntent.confirmationStatus == 'Confirmed');
    const denied = confirmExist && (currentIntent.confirmationStatus == 'Denied');
    
    // If there's no slot value or user denied previous input, prompt for slot input
    if (!slotsExist || denied) {
        const elicitSlotResponse = new ElicitSlotResponse(INTENT_NAME, SLOT_NICKNAME, { nickname: "" });
        callback(null ,elicitSlotResponse.json);
        return;
    }
    
    // If confirmed or existing value, close the converstation
    if (confirmed || valueExisting) {
        const closeResponse = new CloseResponse(TYPE_CLOSE, STATE_FULFILLED, slots.nickname + " has been added. Goodbye.");
        callback(null, closeResponse.json);
        return;
    }
    
    // If value not existing ask for confirmation to add
    if (!valueExisting) {
        const confirmResponse = new ConfirmResponse(INTENT_NAME, { nickname: slots.nickname }, 
            "The nickname " + slots.nickname + " doesn't exist, add as new nickname?");
        callback(null, confirmResponse.json);
        return;
    }
    
    // Fallback
    callback(null, defaultResponse);
};

// Check if the nickname slot exists and has a value
function checkSlotExist(slots) {
    if (slots == null) {
        return false;
    }
    const slotNickname = slots.nickname;
    if (slotNickname == null) {
        return false;
    }
    return true;
}

// Check if the value is an existing value
function checkExistingValue(value) {
    if (value == null) {
        return false;
    }
    return value.toUpperCase() == EXISTING_NICKNAME.toUpperCase();
}

// Check if confirmation exists
function checkConfirmation(status) {
    return status != null;
}