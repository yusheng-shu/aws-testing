//
//  CardButtonView.swift
//  chatbot
//
//  Created by soknife on 11/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class CardButtonView: UIView {
    @IBOutlet weak var button: UIButton!
    @IBOutlet weak var label: UILabel!
    
    private var sendMessageDelegate: SendMessageDelegate?
    private var display = "Unknown response"
    private var response = "Unknown response"
    
    @IBAction func reply() {
        if (sendMessageDelegate == nil) {
            return
        }
        sendMessageDelegate?.sendMessage(chatMessage: UserChatMessage(text: response))
    }
    
    public func setContent(display: String, response: String, sendMessageDelegate: SendMessageDelegate) {
        self.display = display
        self.response = response
        self.sendMessageDelegate = sendMessageDelegate
        
        //label.numberOfLines = 1;
        //label.adjustsFontSizeToFitWidth = true;
        //label.lineBreakMode = NSLineBreakMode.byClipping
        label.text = display
    }
}

