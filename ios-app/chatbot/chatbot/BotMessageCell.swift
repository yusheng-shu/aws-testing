//
//  BotMessageCell.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class BotMessageCell: MessageCell {
    @IBOutlet weak var cardContainer: UIView!
    @IBOutlet weak var cardContainerBottomMargin: NSLayoutConstraint!
    
    override public func setContent(chatMessage: ChatMessage) {
        message.text = chatMessage.text
        
        if (chatMessage is BotChatMessage) {
            let botMessage = chatMessage as! BotChatMessage
            guard let card = botMessage.card else { return }
            guard let buttons = card.buttons else { return }
            
            var lastView: UIView = cardContainer
            
            cardContainerBottomMargin.constant = 8
            
            for i in 0..<buttons.count {
                // Setup button view
                let cardButtonView = Bundle.main.loadNibNamed("CardButton", owner: self, options: nil)?.first as? CardButtonView
                cardButtonView?.setContent(display: buttons[i].text!, response: buttons[i].value!, sendMessageDelegate: botMessage.sendMessageDelegate!)
                cardButtonView?.translatesAutoresizingMaskIntoConstraints = false
                cardButtonView?.heightAnchor.constraint(equalToConstant: (cardButtonView?.frame.size.height)!)
                
                // Add to container
                cardContainer.addSubview(cardButtonView!)
                
                if (i == 0) {
                    NSLayoutConstraint(item: cardButtonView!, attribute: NSLayoutAttribute.top, relatedBy: NSLayoutRelation.equal, toItem: cardContainer, attribute: NSLayoutAttribute.top, multiplier: 1, constant: 4).isActive = true
                } else {
                    NSLayoutConstraint(item: cardButtonView!, attribute: NSLayoutAttribute.top, relatedBy: NSLayoutRelation.equal, toItem: lastView, attribute: NSLayoutAttribute.bottom, multiplier: 1, constant: 4).isActive = true
                }
                
                cardButtonView?.leadingAnchor.constraint(equalTo: cardContainer.layoutMarginsGuide.leadingAnchor).isActive = true
                cardButtonView?.trailingAnchor.constraint(equalTo: cardContainer.layoutMarginsGuide.trailingAnchor).isActive = true
                
                if (i == buttons.count - 1) {
                    NSLayoutConstraint(item: cardButtonView!, attribute: NSLayoutAttribute.bottom, relatedBy: NSLayoutRelation.equal, toItem: cardContainer, attribute: NSLayoutAttribute.bottom, multiplier: 1, constant: 4).isActive = true
                }
                
                lastView = cardButtonView!
            }
        }
    }
}
