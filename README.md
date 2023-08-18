# M300_Vuzix_WebRTC_Demo_Server_and_Application

## Local P2P and Web Server Install and Run

Install Nodejs on server or workstation
cd to server directory of source code

    npm install
    npm start

## Configuration paramerter

There are some configuration parameters on the config.json.

To have those configurations be used, make sure you modify them and save them before you start the server.

  p2p - plain: 8095, - Peer to Peer unencrypted port
  p2p - secured: 8096, - Peer to Peer TLS port

  web - plain: 8080, - Web client unencrypted port
  web - secured: 8433, - Web client TLS port

  ip_address - automatic: true,  - Enable automatic IP detection, 
  ip_address - address: "192.168.1.2", - String address used if automatic IP detection is disabled.


## Local P2P and Web Server Stop

To stop the P2P and web server just exit the NPM process.
Remeber to stop the NodeJS process running in the background since it has an known issues with locking the ports.

## Android client application
Install Android studio 3.0 or above
Open existing android project
Select VuzixHUD directory
Build and deploy application to Vuzix M300

A precompiled APK is provided in the bin directory which can be installed with ADB command line tools

    adb install vuzix-xrclient-debug.apk

## M300, M400, Blade Button Interface

Application navigation: The "menu" gesture will bring up a menu. The "back" gesture will exit application.


## Use case 

Start the signaling server.
Log into p2p signaling server with web portal (click login button).
Log into p2p signaling server with M300/Blade using by scanning the QR code displayed on the web portal.
Invite M300/Blade to connect via web portal (click invite button).
Parties can now share video/audio/text between each other using the peer-to-peer connection.

## User Interface

Main screen Video is shared video

Chat messages send from the Desktop client display briefly.

Known Issues: All devices must have unique names.
Work around: Use unique names for each device.


## Not Implemented:
Sharing desktop from PC to glasses requires plugin for Chrome and Signed Certificates.
Secured endpoints, all code exists for implementation.  Requires a signed certificate to secure the endpoints and changing the portal
	to use HTTPS and clients to use wss for websockets on port 8096.

Note: HTTPS required to access camera when not hosted locally

## Original Projects:
All project are deriviate work from Open WebRTC Toolkit
https://github.com/open-webrtc-toolkit

Server: https://github.com/open-webrtc-toolkit/owt-server-p2p
App: https://github.com/open-webrtc-toolkit/owt-client-android
