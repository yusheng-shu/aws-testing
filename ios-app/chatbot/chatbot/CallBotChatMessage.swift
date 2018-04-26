//
//  CallBotChatMessage.swift
//  chatbot
//
//  Created by soknife on 19/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import AWSLex

class CallBotChatMessage: BotChatMessage {
    init(text: String, sendMessageDelegate: SendMessageDelegate) {
        super.init(text: text, card: nil, sendMessageDelegate: sendMessageDelegate)
    }
}
