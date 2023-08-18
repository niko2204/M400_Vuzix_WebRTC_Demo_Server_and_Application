/*
 * Copyright © 2019 Edgespace LLC. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// Prepare for web server
var fs = require('fs');
var path = require('path');
var url = require('url');
var os = require('os');
var ip = require('ip');
var config = require('./config');
var account = require('./vendermodule');
const opn = require('opn');

var dirname = __dirname || path.dirname(fs.readlinkSync('/proc/self/exe'));
var httpsOptions = {
  key: fs.readFileSync(path.resolve(dirname, 'cert/key.pem')).toString(),
  cert: fs.readFileSync(path.resolve(dirname, 'cert/cert.pem')).toString()
};

const express = require('express');
const app = express();

// Change this to make it secure. Ensure you have the proper configuration for HTTPS.
var server = app.listen(config.p2p.plain);
var servers = require('https').createServer(httpsOptions, app).listen(config.p2p.secured);
var io = require('socket.io').listen(server);
var ios = require('socket.io').listen(servers);

var sessionMap = {};  // Key is uid, and value is session object.
var friendlyNameMap = {}; // friendly name mapping

// Check user's token from partner
function validateUser(token, successCallback, failureCallback){
  // TODO: Should check token first, replace this block when engagement with different partners.
  if(token){
    account.authentication(token,function(uid){
      successCallback(uid);
    },function(){
      console.log('Account system return false.');
      failureCallback(0);
    });
  }
  else
    failureCallback(0);
}

function disconnectClient(uid){
  if(sessionMap[uid]!==undefined){
    var session=sessionMap[uid];
    session.emit('server-disconnect');
    session.disconnect();
    console.log('Force disconnected '+uid);
  }
}

function createUuid(){
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
    return v.toString(16);
  });
}

function emitChatEvent(targetUid, eventName, message, successCallback, failureCallback){
  if(sessionMap[targetUid]){
    sessionMap[targetUid].emit(eventName,message);
    if(successCallback)
      successCallback();
  }
  else
    if(failureCallback)
      failureCallback(2201);
}

function authorization(socket, next){
  var query=url.parse(socket.request.url,true).query;
  var token=query.token;
  var clientVersion=query.clientVersion;
  var clientType=query.clientType;
  switch(clientVersion){
    case '4.2':
    case '4.2.1':
    case '4.3':
      // socket.user stores session related information.
      if(token){
        validateUser(token, function(uid){  // Validate user's token successfully.
          socket.user={id:uid};
          console.log(uid+' authentication passed.');
        },function(error){
            // Invalid login.
            console.log('Authentication failed.');
            next();
        });
      }else{
        socket.user=new Object();
        socket.user.id=createUuid()+'@anonymous';
        console.log('Anonymous user: '+socket.user.id);
      }
      next();
      break;
    default:
      next(new Error('2103'));
      console.log('Unsupported client. Client version: '+query.clientVersion);
      break;
  }
}

function onConnection(socket){
  // Disconnect previous session if this user already signed in.
  var uid=socket.user.id;
  var fn = socket.user.id;
  disconnectClient(uid);
  sessionMap[uid]=socket;
  friendlyNameMap[uid] = fn;
  socket.emit('server-authenticated',{uid:uid});  // Send current user's id to client.
  console.log('A new client has connected. Online user number: '+Object.keys(sessionMap).length);

  socket.on('disconnect',function(){
    if(socket.user){
      var uid=socket.user.id;
      // Delete session
      if(socket===sessionMap[socket.user.id]){
        delete sessionMap[socket.user.id];
        delete friendlyNameMap[socket.user.id];
      }
      console.log(uid+' has disconnected. Online user number: '+Object.keys(sessionMap).length);
      socket.broadcast.emit('server-list', JSON.stringify(friendlyNameMap)); // Send current user's id to client.
    }
  });

  socket.on('vuzix-message',function(msg){
    if(socket.user){
      let senderID = socket.user.id;
      msg.from = senderID;
      let receiverID = msg.to;
      console.log(senderID +' sending custom message to: ' + receiverID + ' message: ' + JSON.stringify(msg.data));
      emitChatEvent(receiverID, 'vuzix-message', msg)
    }
  });
  
  // Forward events
  var forwardEvents=['owt-message'];
  for (var i=0;i<forwardEvents.length;i++){
    socket.on(forwardEvents[i],(function(i){
      return function(data, ackCallback){
        console.log('Received '+forwardEvents[i]+': '+JSON.stringify(data));
        data.from=socket.user.id;
        var to=data.to;
        delete data.to;
        emitChatEvent(to,forwardEvents[i],data,function(){
          if(ackCallback){
            ackCallback();
          }
        },function(errorCode){
          console.log("error: " + errorCode);
          if(ackCallback)
            ackCallback(errorCode);
        });
      };
    })(i));
  }
  console.log("Sending server list to clients");
  socket.broadcast.emit('server-list', JSON.stringify(friendlyNameMap)); // Send current user's id to client.
}

function listen(io) {
  io.use(authorization);
  io.on('connection',onConnection);
}

listen(io);
listen(ios);
console.info ('Vuzix WebRTC Demo Server Version 4.3'); 

// Signaling server only allowed to be connected with Socket.io.
// If a client try to connect it with any other methods, server returns 405.
/*app.get('*', function(req, res, next) {
  res.send(405, 'WebRTC signaling server. Please connect it with Socket.IO.');
});*/
if (config.ip_address.automatic){
 console.info ('Local Machine IP: ' + ip.address() );  
}
else{
  console.info ('Using override IP address of: ' + config.ip_address.address ); 
}


/* server up static content */
app.use(express.static('p2p_web'))

/* listen on port 8080 */
// Change this to make it secure. Ensure you have the proper configuration for HTTPS.
var server = app.listen(config.web.plain);


console.info('P2P Signaling Server\r\n\tListening ports: ' + config.p2p.plain + '/' + config.p2p.secured);
console.info('Demonstration WEB Server\r\n\tListening ports \r\n\tListening ports: ' + config.web.plain);
console.info('\r\nPress Ctrl + C to stop.\r\n\r\n');

/* get IP address for QR generation */
var ip_address = "";

if (config.ip_address.automatic){
   ip_address = ip.address();
}
else
{
  ip_address = config.ip_address.address;
}

var open_addr = 'http://127.0.0.1:8080/index.html?ipaddr='+ ip_address;

opn(open_addr, function (err) {
  if (err) throw err;
  console.log('Error Opening the browser');
});
