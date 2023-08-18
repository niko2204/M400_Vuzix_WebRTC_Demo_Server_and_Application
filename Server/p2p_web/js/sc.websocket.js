// Copyright (C) <2018> Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

'use strict';
/**
 * @class SignalingChannel
 * @classDesc Signaling module for Open WebRTC Toolkit P2P chat
 */
function SignalingChannel() {

  this.onMessage = null;
  this.onVuzixMessage = null;
  this.onServerDisconnected = null;

  var clientType = 'Web';
  var clientVersion = '4.3';
  var myUUID = null;

  var wsServer = null;

  var self = this;

  let connectPromise=null;

  var MAX_TRIALS = 10;
  var reconnectTimes = 0;

  /* TODO: Do remember to trigger onMessage when new message is received.
     if(this.onMessage)
       this.onMessage(from, message);
   */

  // message should a string.
  this.send = function(targetId, message) {
    var data = {
      data: message,
      to: targetId
    };
    return new Promise((resolve, reject) => {
      wsServer.emit('owt-message', data, function(err) {
        if (err)
          reject(err);
        else
          resolve();
      });
    });
  };

  this.sendVuzix = function(targetId, message) {
    console.log("inside sendVuzix");
    var data = {
      data: message,
      to: targetId
    };
    return new Promise((resolve, reject) => {
      wsServer.emit('vuzix-message', data, function(err) {
        if (err)
          reject(err);
        else
          resolve();
      });
    });
  };

  this.connect = function(loginInfo) {
    var serverAddress = loginInfo.host;
    var token = loginInfo.token;
    var paramters = [];
    var queryString = null;
    paramters.push('clientType=' + clientType);
    paramters.push('clientVersion=' + clientVersion);
    if (token)
      paramters.push('token=' + encodeURIComponent(token));
    if (paramters)
      queryString = paramters.join('&');
    console.log('Query string: ' + queryString);
    var opts = {
      query: queryString,
      'reconnection': true,
      'reconnectionAttempts': MAX_TRIALS,
      'force new connection': true
    };
    wsServer = io(serverAddress, opts);

    // refresh server list here
    wsServer.on('server-list', function (data) {
      console.log('server-list');
      console.log(data);

      // use a passed in function here
      var ul = document.getElementById('hud_device_list');
      var cd = document.getElementById('ftConnectedDevices');
      var cuid = document.getElementById('target-uid');
      console.log('CUID: ' + JSON.stringify(cuid));
      var count = 0;
      ul.innerHTML = '';
      if (ul != undefined) {
        var fmap = JSON.parse(data);
        for (var key in fmap) {
          console.log('myUUID: ' + myUUID);
          if (key != myUUID) {
            count++;
            console.log(key + ' <==> ' + fmap[key]);
            var li = document.createElement('li');
            var div_med_rt = document.createElement('div');
            var div_content = document.createElement('div');
            var para = document.createElement('p');
            li.setAttribute('id', key);
            li.className += 'list-group-item media v-middle';

            if (cuid != undefined && cuid.value == key) {
              // highlight user as connected
              li.className += ' connect_highlight';
            }

            var div_med_bod = document.createElement('div');
            div_med_bod.className += 'media-body';
            para.className += 'text-subhead';
            para.innerText = fmap[key];
            div_med_bod.appendChild(para);
            div_med_rt.className += 'media-right';
            div_content.className += 'width-30 margin-none';
            div_content.innerHTML = '<button onclick=\'fnButtonInvite("' + key + '")\'><i class="fa fa-plug" aria-hidden="true"></i></button>';
            div_med_rt.appendChild(div_content);
            li.appendChild(div_med_bod);
            li.appendChild(div_med_rt);
            ul.appendChild(li);
          }

        }
        cd.innerText = 'Total Devices: ' + count;
      }


    });

    wsServer.on('connect', function() {
      reconnectTimes = 0;
      console.info('Connected to websocket server.');
    });

    wsServer.on('server-authenticated', function(data) {
      console.log('Authentication passed. User ID: ' + data.uid);
      myUUID = data.uid;
      if(connectPromise){
        connectPromise.resolve(data.uid);
      }
      connectPromise=null;
    });

    wsServer.on('reconnecting', function(){
      reconnectTimes++;
    });

    wsServer.on('reconnect_failed', function(){
      if (self.onServerDisconnected)
        self.onServerDisconnected();
    })

    wsServer.on('server-disconnect', function(){
      reconnectTimes = MAX_TRIALS;
    })

    wsServer.on('disconnect', function() {
      console.info('Disconnected from websocket server.');
      if (reconnectTimes >= MAX_TRIALS && self.onServerDisconnected)
        self.onServerDisconnected();
    });

    wsServer.on('connect_failed', function(errorCode) {
      console.error('Connect to websocket server failed, error:' +
        errorCode + '.');
      if (connectPromise) {
        connectPromise.reject(parseInt(errorCode))
      }
      connectPromise = null;
    });

    wsServer.on('error', function(err) {
      console.error('Socket.IO error:' + err);
      if (err == '2103' && connectPromise) {
        connectPromise.reject(err)
        connectPromise=null;
      }
    });

    wsServer.on('owt-message', function(data) {
      console.info('Received owt message.');
      if (self.onMessage)
        self.onMessage(data.from, data.data);
    });

    
    wsServer.on('vuzix-message', function(data) {
      console.info('Received Vuzix message.');
      console.log(JSON.stringify(data));
      if (self.onVuzixMessage)
        self.onVuzixMessage(data.from, data.data);
    });

    return new Promise((resolve, reject) => {
      connectPromise = {
        resolve: resolve,
        reject: reject
      };
    });
  };

  this.disconnect = function() {
    reconnectTimes = MAX_TRIALS;
    if (wsServer)
      wsServer.close();
    return Promise.resolve();
  };

}
