//
//  MessageCellView.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class UserMessageCell: MessageCell {
    
    override public func setContent(chatMessage: ChatMessage) {
        message.text = chatMessage.text
    }
}
