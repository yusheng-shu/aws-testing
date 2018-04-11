//
//  MessageCell.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class MessageCell: UITableViewCell {
    @IBOutlet weak var message: UILabel!
    @IBOutlet weak var box: UIView!
    
    public func setContent(chatMessage: ChatMessage) {}
}
