//
//  MessageCell.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class MessageCell: UITableViewCell, UITextViewDelegate {
    @IBOutlet weak var message: UITextView!
    @IBOutlet weak var box: UIView!
    
    let URL_PATTERN = ".*\\s(https:\\/\\/www\\.google\\.com\\S*)"
    let MAP_TEXT = "Click here to open map"
    
    
    internal func getHyperlinkMessage(message: String) -> NSMutableAttributedString {
        let attributedString = NSMutableAttributedString(string: message)
        
        do {
            let regex = try NSRegularExpression(pattern: URL_PATTERN, options: NSRegularExpression.Options.caseInsensitive)
            let matches = regex.matches(in: message, options: [], range: NSRange(location: 0, length: message.characters.count))
            
            var subStringRange: [String : NSRange] = [:]
            for match in matches {
                let urlRange = match.rangeAt(1)
                let urlString = NSString(string: message).substring(with: urlRange)
                attributedString.replaceCharacters(in: urlRange, with: MAP_TEXT)
                var mapTextRange = NSRange()
                mapTextRange.location = urlRange.location
                mapTextRange.length = MAP_TEXT.characters.count
                subStringRange.updateValue(mapTextRange, forKey: urlString)
                print(urlString)
            }
            
            for set in subStringRange {
                let linkAttributes = [
                    NSLinkAttributeName: NSURL(string: set.key)!,
                    NSForegroundColorAttributeName: UIColor(red: 0.5, green: 0, blue: 0, alpha: 1)
                    ] as [String : Any]
                
                attributedString.setAttributes(linkAttributes, range: set.value)
                
            }
            
        } catch {
            return attributedString
        }
        return attributedString
    }
    
    func textView(_ textView: UITextView, shouldInteractWith URL: URL, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool {
        return true
    }
    
    public func setContent(chatMessage: ChatMessage) {
        message.delegate = self
        message.attributedText = getHyperlinkMessage(message: chatMessage.text)
        message.isUserInteractionEnabled = true
        message.isEditable = false
    }
}
