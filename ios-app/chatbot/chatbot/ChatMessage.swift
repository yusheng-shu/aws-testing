//
//  ChatMessage.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation

class ChatMessage {
    internal var text: String
    internal var sender: SenderType
    
    init(text: String, sender: SenderType) {
        self.text = text
        self.sender = sender
    }
    
}
