jQuery(window).load(function () {

  "use strict";

  // Page Preloader
  jQuery('#preloader').delay(350).fadeOut(function () {
     jQuery('body').delay(350).css({ 'overflow': 'visible' });
  });
});
 

jQuery(document).ready(function () {

  "use strict";
  // Set some default values
  $('#devid').val("XRClient");
  $('#uid').val("XRApp");

  $('#target-video-publish').prop('disabled', true);
  $('#target-screen').prop('disabled', true);
  $('#target-snapshot').prop('disabled', true);
  $('#request-remote-video').prop('disabled', true);

  $('#data-send').prop('disabled', true);
  $('#imu-enable').prop('disabled', true);

  // this is the WebRTC Code
  $('#target-connect').click(function () {
    console.log("Connect Clicked to: " + getTargetId());
    p2p.allowedRemoteIds=[getTargetId()];
    $('#target-video-publish').prop('disabled', false);
    
  });

  $('#imu-enable').click(function () {
    let ele = $('#imu-enable > i');
    if (ele.hasClass('fa-toggle-off')) {
        console.log('turn on');            
        ele.removeClass('fa-toggle-off');
        ele.addClass('fa-toggle-on');
        fnSendImuRequest(true);
    }else{
        console.log('turn off')
        ele.removeClass('fa-toggle-on');
        ele.addClass('fa-toggle-off');
        fnSendImuRequest(false);
    }
  });

  $('#request-remote-video').click(function () {

    if ($('#target-uid').val() == "") {
      console.log('Not connected please connect first');
      fnGrowlWarning('Not connected to peer, please connect first.');
      return;
    }

    // send video request
    console.log('Sending video request to: ' + $('#target-uid').val());
    if (isViewingRemote) {
      fnSendViewRequest(false);
    } else {
      fnSendViewRequest(true);
    }
  });

  $('#target-snapshot').click(function () {

    if ($('#target-uid').val() == "") {
      console.log('Not connected please connect first');
      fnGrowlWarning('Not connected to peer, please connect first.');
      return;
    }

    // send video request
    console.log('Taking image and saving to disk of: ' + $('#target-uid').val());
    var canvas = document.createElement("canvas");
    var video = $('#remote video').get(0);
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);

    var a = $("<a>")
      .attr("href", canvas.toDataURL())
      .attr("download", "capture.png")
      .appendTo("body");

    a[0].click();

    a.remove();

  });

  $('#target-screen').click(function () {

    jQuery.gritter.add({
      title: 'Share Screen Chrome Addon Must be installed!',
      text: 'Add extension and modify this click funtion to enable',
      class_name: 'growl-danger',
      image: 'images/screen.png',
      sticky: false,
      time: ''
    });
    return false;
  });

  $('#target-video-publish').click(function () {

    console.log('target-video-publish::onClick');

    if ($('#target-uid').val() == "") {
      console.log('Not connected please connect first');
      fnGrowlWarning('Not connected to peer, please connect first.');
      return;
    }

    if ($('#target-video-publish').hasClass('btn-flat')) {
      
      console.log('has flat class');
      $('#target-video-publish').prop('disabled', false);
      $('#target-video-publish').removeClass('btn-flat');
      $('#target-video-publish').addClass('btn-inverse');

      if (localStream) {
        p2p.publish(getTargetId(), localStream).then(publication => {
           publicationForCamera = publication;
        }, error => {
           console.log('Failed to share video.');
        }); // Publish local stream to remote client
      } else {
        const audioConstraintsForMic = new Owt.Base.AudioTrackConstraints(Owt.Base.AudioSourceInfo.MIC);
        const videoConstraintsForCamera = new Owt.Base.VideoTrackConstraints(Owt.Base.VideoSourceInfo.CAMERA);
        let mediaStream;
        Owt.Base.MediaStreamFactory.createMediaStream(new Owt.Base.StreamConstraints(audioConstraintsForMic, videoConstraintsForCamera)).then(stream => {
           mediaStream = stream;
           localStream = new Owt.Base.LocalStream(mediaStream, new Owt.Base.StreamSourceInfo('mic', 'camera'));
           $('#local').children('video').get(0).srcObject = localStream.mediaStream;
           p2p.publish(getTargetId(), localStream).then(publication => {
              publicationForCamera = publication;
           }, error => {
              console.log('Failed to share video.');
           });
           $('#pip-container').show();
        }, err => {
           console.error('Failed to create MediaStream, ' + err);
        });
      }

    } else {
      console.log('does not have flat class');
      $('#target-video-publish').prop('disabled', false);
      $('#target-video-publish').removeClass('btn-inverse');
      $('#target-video-publish').addClass('btn-flat');
      publicationForCamera.stop();
      for (const track of localStream.mediaStream.getTracks()) {
         track.stop();
      }
      localStream = undefined;
      $("#local video").get(0).src = '';
      $('#pip-container').hide();

    }
  });

  $('#target-disconnect').click(function () {
    p2p.stop($('#target-uid').val()); // Stop chat
  });

  $('#login').click(function () {
    p2p.connect({
      host: serverAddress,
      token: $('#uid').val()
    }, function () {
      $('#uid').prop('disabled', true);
    }); // Connect to peer server
  });

  $('#logoff').click(function () {
    p2p.disconnect(); // Disconnected from peer server.
    $('#uid').prop('disabled', false);
  });

  $('#data-send').click(function () {
    fnSendMessageData($('#dataSent').val());
    fnChatSendHandler($('#dataSent').val());
    $('#dataSent').val('');
  });

  $("#btnPencil").click(function () {
    console.log("Button Line Selected");
    this.className="btn btn-inverse";
    document.getElementById('btnCircle').className="btn btn-flat";
    document.getElementById('btnSquare').className="btn btn-flat";
    drawingApp.setPenStyle('line');
  });

  $("#btnCircle").click(function () {
      console.log("Button Circle Selected");
      this.className = "btn btn-inverse";
      document.getElementById('btnPencil').className="btn btn-flat";
      document.getElementById('btnSquare').className="btn btn-flat";
      drawingApp.setPenStyle('circle');
  });

  $("#btnSquare").click(function () {
      console.log("Button Square Selected");
      this.className = "btn btn-inverse";
      document.getElementById('btnCircle').className="btn btn-flat";
      document.getElementById('btnPencil').className="btn btn-flat";
      drawingApp.setPenStyle('square');
  });

  $("#btnClear").click(function () {
      var mediaPlayer = document.getElementById('remote');

      drawingApp.clear();
  });

  // This is the QR code generator
  var options = {
    render: 'div',
    minVersion: 1,
    maxVersion: 40,
    ecLevel: 'L',

    background: '#FFF',
    // corner radius relative to module width: 0.0 .. 0.5
    radius: 0,

    // quiet zone in modules
    quiet: 0,

    // content
    text: '{"ipaddr":"' + getUrlParameter('ipaddr') + '","port":8095,"devid":"' + $('#devid').val() + '"}',
    size: 100,

    fill: '#000',

    // modes
    // 0: normal
    // 1: label strip
    // 2: label box
    // 3: image strip
    // 4: image box
    mode: 0,

    mSize: 0.1,
    mPosX: 0.5,
    mPosY: 0.5,

    label: 'no label',
    fontname: 'sans',
    fontcolor: '#000',

    image: null
  };


  $('#devname').click(function () {
    options.text = '{"ipaddr":"' + getUrlParameter('ipaddr') + '","port":8095,"devid":"' + $('#devid').val() + '"}';
    $('#m300qr').empty().qrcode(options);
  });

  $('#m300qr').qrcode(options);

  // login
  p2p.connect({
    host: serverAddress,
    token: $('#uid').val()
  }, function () {
    $('#uid').prop('disabled', true);
  }); // Connect to peer server

  initScene();
  render();
});