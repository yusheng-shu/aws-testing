//
//  ViewController.swift
//  chatbot
//
//  Created by soknife on 10/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import UIKit
import CoreLocation
import AWSMobileClient
import AWSAuthCore
import AWSLex

class ChatViewController: UIViewController, UITableViewDelegate, UITableViewDataSource, UITextFieldDelegate, SendMessageDelegate, CLLocationManagerDelegate {
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var textInput: UITextField!
    @IBOutlet weak var sendButton: UIButton!
    @IBOutlet weak var clearButton: UIButton!
    @IBOutlet weak var clearButtonHeight: NSLayoutConstraint!
    
    private var locationManager: CLLocationManager!
    
    private let botName = "AnsBot"
    private let botAlias = "$LATEST"
    private let userId = UUID().uuidString
    private let chatKey = "chatConfig"
    private let maxChatWait: Double = 10
    private let maxLocationWait: Double = 10
    private let errorMessage = "Sorry, I couldn't connect to the service. Please check your network connection. Or call 1800800007 for more assistance."
    
    internal var lexResponseReceived = false
    internal var locationReceived = false
    internal let lexDispatch = DispatchGroup()
    internal let locationDispatch = DispatchGroup()
    
    private var currentLocation: CLLocation!
    private var sessionAttributes: [String : String] = [:]
    private var messages: [ChatMessage] = []
    private var cellHeights: [IndexPath : CGFloat] = [:]
    private var clearButtonOGHeight: CGFloat = 24

    override func viewDidLoad() {
        super.viewDidLoad()
        disableInput()
        initInput()
        initChat()
        initLocation()
        initAWS(botName, botAlias)
        enableInput()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        listenForLocation()
    }
    
    private func initChat() {
        tableView.delegate = self
        tableView.dataSource = self
        tableView.rowHeight = UITableViewAutomaticDimension
        tableView.estimatedRowHeight = 44
        tableView.reloadData()
        fillFromBottom(unscroll: false)
        insertNewMessage(BotChatMessage(text: "Hi, how may I help you today?", card: nil, sendMessageDelegate: self))
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

    private func initLocation() {
        locationManager = CLLocationManager()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
    }
    
    private func initAWS(_ botName: String, _ botAlias: String) {
        // Set region and credential config
        let credentialsProvider = AWSMobileClient.sharedInstance().getCredentialsProvider()
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        AWSLex.register(with: configuration!, forKey: chatKey)
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
        request.sessionAttributes?["latitude"] = String(currentLocation.coordinate.latitude)
        request.sessionAttributes?["longitude"] = String(currentLocation.coordinate.longitude)
        request.inputText = chatMessage.text
        
        AWSLex(forKey: chatKey).postText(request, completionHandler: receiveMessage(response:error:))
        
        lexDispatch.enter()
        
        // Timeout for response
        DispatchQueue.main.async {
            let result = self.lexDispatch.wait(timeout: DispatchTime.now() + self.maxChatWait)
            if (result == DispatchTimeoutResult.timedOut) {
                self.insertNewMessage(BotChatMessage(text: self.errorMessage, card: nil, sendMessageDelegate: self))
            }
        }
        
    }
    
    // Receive message callback
    private func receiveMessage(response: AWSLexPostTextResponse?, error: Error?) {
        lexDispatch.leave()
        // Default response
        var responseMsg = "Sorry, I can't answer your question."
        var responseCard: AWSLexGenericAttachment?
        var escalate = false
        
        // Error response
        if let _err = error {
            responseMsg = errorMessage
            print("Lex response error: \(_err)")
        } else if (response == nil) {
            responseMsg = errorMessage
            print("Lex response error: There is no response!")
        }
            
        // Success response
        else {
            if let _responseMsg = response?.message {
                responseMsg = _responseMsg
            }
            
            // Get card
            if let _responseCard = response?.responseCard {
                if let _genericAttachments = _responseCard.genericAttachments {
                    responseCard = _genericAttachments.first
                }
            }
            
            // Get session attributes
            if (response?.sessionAttributes != nil) {
                sessionAttributes = (response?.sessionAttributes)!
                if (sessionAttributes["globalFail"] != nil) {
                    escalate = true
                    sessionAttributes.removeValue(forKey: "globalFail")
                }
            }
        }
        
        
        // Display received message
        DispatchQueue.main.asyncAfter(deadline: .now() + 1, execute: {
            if (escalate) {
                self.insertNewMessage(CallBotChatMessage(text: responseMsg, sendMessageDelegate: self))
            } else {
                self.insertNewMessage(BotChatMessage(text: responseMsg, card: responseCard, sendMessageDelegate: self))
            }
        })
        
    }
    
    // Clears the chat log
    @IBAction func clearChat() {
        messages.removeAll() // Remove all messages
        // Disable user input
        textInput.isUserInteractionEnabled = false
        clearButton.isUserInteractionEnabled = false
        sendButton.isUserInteractionEnabled = false
        tableView.isUserInteractionEnabled = false
        
        // Animate chat log fade out and scroll up
        UIView.animate(withDuration: 0.3, animations: {
            self.tableView.contentInset = UIEdgeInsetsMake(0, 0, self.tableView.frame.size.height, 0)
            self.tableView.alpha = 0
        }) { (complete) in
            self.tableView.contentInset = UIEdgeInsetsMake(0, 0, 0, 0)
            self.tableView.alpha = 1
            self.tableView.reloadData()
            self.fillFromBottom(unscroll: false)
            // Re-enable user input
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
            self.tableView.scrollToRow(at: IndexPath(row: self.messages.count - 1, section: 0), at: UITableViewScrollPosition.top, animated: true)
            
        }
        tableView.beginUpdates()
        tableView.insertRows(at: [indexPathToInsert], with: .top)
        tableView.endUpdates()
        
        CATransaction.commit()
 
    }
    
    // Show and hide the chat button when required
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
    
    private func disableInput() {
        textInput.isEnabled = false
    }
    
    private func enableInput() {
        textInput.isEnabled = true
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
            if (messages[indexPath.row] is CallBotChatMessage) {
                cell = tableView.dequeueReusableCell(withIdentifier: "callBotMessageCell") as! CallBotMessageCell
            } else {
                cell = tableView.dequeueReusableCell(withIdentifier: "botMessageCell") as! BotMessageCell
            }
        }
        cell.setContent(chatMessage: messages[indexPath.row])
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        cellHeights[indexPath] = cell.frame.size.height
    }
    
    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        guard let height = cellHeights[indexPath] else { return 44 }
        return height
    }
    
    // Create inset at the top of chat view content to push messages to the bottom
    // This function is only used when the chat log does not fill the entire window
    private func fillFromBottom(unscroll: Bool) {
        // Don't do anything if chat log fills the entire window
        if (tableView.contentSize.height > tableView.frame.size.height) {
            tableView.contentInset = UIEdgeInsetsMake(0, 0, 0, 0)
            //return
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
    // Scroll to specificed row
    private func scrollTo(row: Int) {
        if (row < 0) { return }
        
        CATransaction.begin()
        
        CATransaction.setCompletionBlock {
            self.tableView.scrollToRow(at: IndexPath(row: row, section: 0), at: UITableViewScrollPosition.top, animated: true)
        }
        // Do a "mini" scroll to force the top cell to unload
        // This avoids the chat log from "skipping" when scrolling
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

    private func listenForLocation() {
        if CLLocationManager.locationServicesEnabled() {
            locationManager.startUpdatingLocation()
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations[locations.count - 1] as CLLocation
        
        locationManager.stopUpdatingLocation()
        
        print("User location: = \(currentLocation.coordinate.latitude), \(currentLocation.coordinate.longitude)")
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Error \(error)")
    }
}

