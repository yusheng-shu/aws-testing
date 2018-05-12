//
//  CallBotMessageCell.swift
//  chatbot
//
//  Created by soknife on 19/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class CallBotMessageCell: MessageCell {
    private let PTV_PHONE = "1800800007"
    
    @IBOutlet weak var callButton: UIButton!
    
    override public func setContent(chatMessage: ChatMessage) {
        super.setContent(chatMessage: chatMessage)
        message.textColor = UIColor.black
        
    }
    
    @IBAction func call() {
        if let url = URL(string: "tel:\(PTV_PHONE)"), UIApplication.shared.canOpenURL(url) {
            if #available(iOS 10, *) {
                UIApplication.shared.open(url)
            } else {
                UIApplication.shared.openURL(url)
            }
        }
    }
}
