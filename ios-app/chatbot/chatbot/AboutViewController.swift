//
//  AboutView.swift
//  chatbot
//
//  Created by soknife on 11/4/18.
//  Copyright © 2018 awsptv. All rights reserved.
//

import Foundation
import UIKit

class AboutViewController: UIViewController {
    @IBAction func ptvWebsite() {
        if let link = URL(string: "https://www.ptv.vic.gov.au/") {
            UIApplication.shared.open(link)
        }
    }
}
