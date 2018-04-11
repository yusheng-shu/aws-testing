//
//  ViewController.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import UIKit
import AWSMobileClient
import AWSAuthCore
import AWSLex

class ChatViewController: UIViewController, UITableViewDelegate, UITableViewDataSource, UITextFieldDelegate, SendMessageDelegate {
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var textInput: UITextField!
    @IBOutlet weak var sendButton: UIButton!
    @IBOutlet weak var clearButton: UIButton!
    @IBOutlet weak var clearButtonHeight: NSLayoutConstraint!
    
    let botName = "AnsBot"
    let botAlias = "$LATEST"
    let userId = UUID().uuidString
    let sessionAttributes: [String : String] = [:]
    let chatKey = "chatConfig"
    
    //private var interactionKit: AWSLexInteractionKit?
    // var textModeSwitchingCompletion: AWSTaskCompletionSource<NSString>?
    
    private var messages: [ChatMessage] = []
    private var clearButtonOGHeight: CGFloat = 24

    override func viewDidLoad() {
        super.viewDidLoad()
        initInput()
        initChat()
        initAWS(botName, botAlias)
    }
    
    private func initChat() {
        tableView.delegate = self
        tableView.dataSource = self
        tableView.rowHeight = UITableViewAutomaticDimension
        tableView.estimatedRowHeight = 64
        tableView.reloadData()
        
        //tableView.contentInset = UIEdgeInsetsMake(CGFloat(tableView.frame.size.height), 0, 0, 0)
        fillFromBottom(unscroll: false)
        //scrollTo(y: ((tableView.contentSize.height) - (tableView.frame.size.height)), animated: false)
    }
    
    private func initInput() {
        // Register listner for keyboard show and hide events
        NotificationCenter.default.addObserver(self, selector: #selector(ChatViewController.keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(ChatViewController.keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        
        let tap: UITapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(ChatViewController.dismissKeyboard))
        view.addGestureRecognizer(tap)
        
        textInput.delegate = self
        clearButtonOGHeight = clearButtonHeight.constant
    }
    
    private func initAWS(_ botName: String, _ botAlias: String) {
        // Set region and credential config
        let credentialsProvider = AWSMobileClient.sharedInstance().getCredentialsProvider()
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        AWSLex.register(with: configuration!, forKey: chatKey)
        
        /* UNUSED: INTERACTION KIT NOT SUITABLE FOR RESPONSE CARDS
        // Register listener for lex events
        let chatConfig = AWSLexInteractionKitConfig.defaultInteractionKitConfig(withBotName: botName, botAlias: botAlias)
        AWSLexInteractionKit.register(with: configuration!, interactionKitConfiguration: chatConfig, forKey: "chatConfig")
        self.interactionKit = AWSLexInteractionKit.init(forKey: "chatConfig")
        self.interactionKit?.interactionDelegate = self
         */
    }
    
    // Send current text from input to lex
    @IBAction func sendMessage() {
        guard let input = textInput.text else { return }
        if (input == "") { return }
        
        textInput.text = ""
        sendMessage(chatMessage: UserChatMessage(text: input))
    }
    
    // Send message as user
    public func sendMessage(chatMessage: UserChatMessage) {
        insertNewMessage(chatMessage)
        
        guard let request = AWSLexPostTextRequest() else { return }
        request.botName = botName
        request.botAlias = botAlias
        request.userId = userId
        request.sessionAttributes = sessionAttributes
        request.inputText = chatMessage.text
        
        AWSLex(forKey: chatKey).postText(request) { (response, error) in
            // Default response
            var responseMsg = "Sorry, I can't answer your question."
            // Error response
            if let _err = error { responseMsg = "Sorry, something went wrong. Error reason: \(_err)" }
            else if (response == nil) { responseMsg = "Sorry, something went wrong. Error reason: No response." }
                // Success response
            else if let _responseMsg = response?.message { responseMsg = _responseMsg }
            
            //print("Response:\n" + String(describing: response))
            
            var responseCard: AWSLexGenericAttachment? = nil
            if let _responseCard = response?.responseCard {
                if let _genericAttachments = _responseCard.genericAttachments {
                    responseCard = _genericAttachments.first
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1, execute: {
                self.insertNewMessage(BotChatMessage(text: responseMsg, card: responseCard, sendMessageDelegate: self))
            })
        }
    }
    
    // Clears the chat log
    @IBAction func clearChat() {
        messages.removeAll()
        textInput.isUserInteractionEnabled = false
        clearButton.isUserInteractionEnabled = false
        sendButton.isUserInteractionEnabled = false
        tableView.isUserInteractionEnabled = false
        
        UIView.animate(withDuration: 0.2, animations: {
            self.tableView.contentInset = UIEdgeInsetsMake(0, 0, self.tableView.frame.size.height, 0)
            self.tableView.alpha = 0
        }) { (complete) in
            self.tableView.contentInset = UIEdgeInsetsMake(0, 0, 0, 0)
            self.tableView.alpha = 1
            self.tableView.reloadData()
            self.fillFromBottom(unscroll: false)
            self.clearButton.isUserInteractionEnabled = true
            self.sendButton.isUserInteractionEnabled = true
            self.tableView.isUserInteractionEnabled = true
            self.textInput.isUserInteractionEnabled = true
        }
    }
    
    
    // Display new message
    private func insertNewMessage(_ message: ChatMessage) {
        messages.append(message)
        let indexPathToInsert = IndexPath(row: messages.count-1, section: 0)
        CATransaction.begin()
        CATransaction.setCompletionBlock {
            self.fillFromBottom(unscroll: true)
            self.scrollTo(row: self.messages.count - 1)
        }
        tableView.beginUpdates()
        tableView.insertRows(at: [indexPathToInsert], with: .bottom)
        tableView.endUpdates()
        CATransaction.commit()
        
        
        
        
        //tableView.layoutIfNeeded()
        //scrollTo(y: ((tableView.contentSize.height) - (tableView.frame.size.height)), animated: true)
        
    }
    
    private func updateClearButton() {
        if (messages.count > 0) {
            UIView.animate(withDuration: 0.2, animations: {
                self.clearButtonHeight.constant = 24
                self.view.layoutIfNeeded()
            })
            
            
        } else {
            UIView.animate(withDuration: 0.2, animations: {
                self.clearButtonHeight.constant = 0
                self.view.layoutIfNeeded()
            })
        }
    }
    
    // Return message count
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        updateClearButton()
        return messages.count
    }
    
    // Assign message content
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        var cell = MessageCell()
        if (messages[indexPath.row].sender == SenderType.user) {
            cell = tableView.dequeueReusableCell(withIdentifier: "userMessageCell") as! UserMessageCell
        } else {
            cell = tableView.dequeueReusableCell(withIdentifier: "botMessageCell") as! BotMessageCell
        }
        cell.setContent(chatMessage: messages[indexPath.row])
        return cell
    }
    
    // Create inset at the top of chat view content to push messages to the bottom
    private func fillFromBottom(unscroll: Bool) {
        if (tableView.contentSize.height > tableView.frame.size.height) {
            tableView.contentInset = UIEdgeInsetsMake(0, 0, 0, 0)
            return
        }
        let rowCount = tableView(tableView!, numberOfRowsInSection: 0)
        var contentInsetTop = Float((tableView.bounds.size.height))
        var lastRowHeight = CGFloat(0)
        for i in 0..<rowCount {
            let rowRect = tableView.rectForRow(at: IndexPath(item: i, section: 0))
            contentInsetTop -= Float((rowRect.size.height))
            if contentInsetTop <= 0 {
                contentInsetTop = 0
            }
            
            if (i == rowCount - 1) {
                lastRowHeight = (rowRect.size.height)
            }
        }
        tableView.contentInset = UIEdgeInsetsMake(CGFloat(contentInsetTop), 0, 0, 0)
        
        // Keep scroll position at last message
        if (unscroll && contentInsetTop > 0) {
            scrollTo(y: ((tableView.contentSize.height) - (tableView.frame.size.height) - lastRowHeight), animated: false)
        }
    }
    
    // Scroll to specificed y-delta
    private func scrollTo(y: CGFloat, animated: Bool) {
        tableView.setContentOffset(CGPoint(x: 0, y: y), animated: animated)
    }
    
    private func scrollTo(row: Int) {
        if (row < 0) { return }
        
        CATransaction.begin()
        CATransaction.setCompletionBlock {
            self.tableView.scrollToRow(at: IndexPath(row: row, section: 0), at: UITableViewScrollPosition.bottom, animated: true)
        }
        
        
        
        let contentOffset = tableView.contentOffset.y
        scrollTo(y: contentOffset + 1, animated: true)
        
        CATransaction.commit()
    }
 
    // Dismiss any editing on view
    func dismissKeyboard() {
        view.endEditing(true)
    }
    
    // Keyboard return event
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if textField == textInput {
            sendMessage()
            return false
        }
        return true
    }
    
    // Keyboard show event
    @objc func keyboardWillShow(notification: NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.cgRectValue {
            self.view.frame.origin.y -= keyboardSize.height
        }
    }
    
    // Keyboard hide event
    @objc func keyboardWillHide(notification: NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.cgRectValue {
            if (self.view.frame.origin.y == 64) { return }
            self.view.frame.origin.y += keyboardSize.height
        }
    }

}

