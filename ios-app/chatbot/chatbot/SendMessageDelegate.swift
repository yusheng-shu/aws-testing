//
//  SendMessageDelegate.swift
//  chatbot
//
//  Created by soknife on 11/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation

protocol SendMessageDelegate {
    func sendMessage(chatMessage: UserChatMessage)
}
