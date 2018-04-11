//
//  UserChatMessage.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation

class UserChatMessage: ChatMessage {
    init(text: String) {
        super.init(text: text, sender: SenderType.user)
    }
}
