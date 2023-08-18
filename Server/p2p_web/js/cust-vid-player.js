// Wait for the DOM to be loaded before initialising the media player
document.addEventListener("DOMContentLoaded", function () { initialiseMediaPlayer(); }, false);

// Variables to store handles to various required elements
var mediaPlayer;
var playPauseBtn;
var muteBtn;

function initialiseMediaPlayer() {
    console.log("<----------- Initialize Player -------------------->");
    // Get a handle to the player
    mediaPlayer = document.getElementById('remoteVideo');

    // Get handles to each of the buttons and required elements
    playPauseBtn = document.getElementById('play-pause-button');
    muteBtn = document.getElementById('mute-button');

    // Hide the browser's default controls
    mediaPlayer.controls = false;


    // Add a listener for the play and pause events so the buttons state can be updated
    mediaPlayer.addEventListener('play', function () {
        if (mediaPlayer == undefined) {
            console.log('video undefined');
            return;
        }
        changeButtonType(playPauseBtn, 'pause');
    }, false);

    mediaPlayer.addEventListener('pause', function () {
        if (mediaPlayer == undefined) {
            console.log('video undefined');
            return;
        }
        changeButtonType(playPauseBtn, 'play');
    }, false);

    // need to work on this one more...how to know it's muted?
    mediaPlayer.addEventListener('volumechange', function (e) {
        if (mediaPlayer == undefined) {
            console.log('video undefined');
            return;
        }

        // Update the button to be mute/unmute
        if (mediaPlayer.muted) changeButtonType(muteBtn, 'unmute');
        else changeButtonType(muteBtn, 'mute');
    }, false);
    mediaPlayer.addEventListener('ended', function () { this.pause(); }, false);
}

function togglePlayPause() {
    if (mediaPlayer == undefined) {
        console.log('video undefined');
        return;
    }

    // If the mediaPlayer is currently paused or has ended
    if (mediaPlayer.paused || mediaPlayer.ended) {
        // Change the button to be a pause button
        changeButtonType(playPauseBtn, 'pause');
        // Play the media
        mediaPlayer.play();
    }
        // Otherwise it must currently be playing
    else {
        // Change the button to be a play button
        changeButtonType(playPauseBtn, 'play');
        // Pause the media
        mediaPlayer.pause();
    }
}

// Stop the current media from playing, and return it to the start position
function stopPlayer() {
    mediaPlayer.pause();
    mediaPlayer.currentTime = 0;
}

// Changes the volume on the media player
function changeVolume(direction) {
    if (mediaPlayer == undefined) {
        console.log('video undefined');
        return;
    }

    if (direction === '+') mediaPlayer.volume += mediaPlayer.volume == 1 ? 0 : 0.1;
    else mediaPlayer.volume -= (mediaPlayer.volume == 0 ? 0 : 0.1);
    mediaPlayer.volume = parseFloat(mediaPlayer.volume).toFixed(1);
}

// Changes the volume on the media player
function toggleFullScreen() {
    if (mediaPlayer == undefined) {
        console.log('video undefined');
        return;
    }

    if (mediaPlayer.requestFullscreen) {
        mediaPlayer.requestFullscreen();
    } else if (mediaPlayer.mozRequestFullScreen) {
        mediaPlayer.mozRequestFullScreen(); // Firefox
    } else if (video.webkitRequestFullscreen) {
        mediaPlayer.webkitRequestFullscreen(); // Chrome and Safari
    }
}

// Toggles the media player's mute and unmute status
function toggleMute() {
    if (mediaPlayer == undefined) {
        console.log('video undefined');
        return;
    }

    if (mediaPlayer.muted) {
        // Change the cutton to be a mute button
        changeButtonType(muteBtn, 'mute');
        // Unmute the media player
        mediaPlayer.muted = false;
    }
    else {
        // Change the button to be an unmute button
        changeButtonType(muteBtn, 'unmute');
        // Mute the media player
        mediaPlayer.muted = true;
    }
}

// Updates a button's title, innerHTML and CSS class to a certain value
function changeButtonType(btn, value) {
    btn.title = value;
    switch (value) {
        case 'unmute':
            btn.innerHTML = '<i class=\'fa fa-volume-off\' aria-hidden=\'true\'></i>';
            break;
        case 'mute':
            btn.innerHTML = '<i class=\'fa fa-volume-up\' aria-hidden=\'true\'></i>';;
            break;
        case 'play':
            btn.innerHTML = '<i class=\'fa fa-play\' aria-hidden=\'true\'></i>';;
            break;
        case 'pause':
            btn.innerHTML = '<i class=\'fa fa-pause\' aria-hidden=\'true\'></i>';;
            break;
        default:
            console.log('Unknown button type switch');
            break;
    }
}

// Loads a video item into the media player
function loadVideo() {
    console.log("<----------- LOAD VIDEO -------------------->");
    for (var i = 0; i < arguments.length; i++) {
        var file = arguments[i].split('.');
        var ext = file[file.length - 1];
        // Check if this media can be played
        if (canPlayVideo(ext)) {
            // Reset the player, change the source file and load it
            resetPlayer();
            mediaPlayer.src = arguments[i];
            mediaPlayer.load();
            break;
        }
    }
}

// Checks if the browser can play this particular type of file or not
function canPlayVideo(ext) {
    var ableToPlay = mediaPlayer.canPlayType('video/' + ext);
    if (ableToPlay == '') return false;
    else return true;
}

// Resets the media player
function resetPlayer() {
    console.log("<----------- RESET PLAYER CALLED -------------------->");
    // Ensure that the play pause button is set as 'play'
    changeButtonType(playPauseBtn, 'play');
    mediaPlayer.src = '';
    mediaPlayer.removeAttribute('src');
    mediaPlayer.posterImage = '~/images/image-placeholder-alt.jpg';
    mediaPlayer.load();

    drawingApp.setDrawEnable(false);
}
