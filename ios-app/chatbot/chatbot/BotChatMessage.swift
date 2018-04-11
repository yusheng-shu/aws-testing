//
//  BotChatMessage.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import AWSLex

class BotChatMessage: ChatMessage {
    var card: AWSLexGenericAttachment?
    var sendMessageDelegate: SendMessageDelegate?
    
    init(text: String, card: AWSLexGenericAttachment?, sendMessageDelegate: SendMessageDelegate) {
        super.init(text: text, sender: SenderType.bot)
        
        self.card = card
        self.sendMessageDelegate = sendMessageDelegate
    }
}
