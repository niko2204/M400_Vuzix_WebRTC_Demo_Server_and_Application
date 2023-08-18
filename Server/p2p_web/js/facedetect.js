
var FaceDetect = {

    drawToCanvas : function() {
        
        var ctx = FaceDetect.ctx,
            canvas = FaceDetect.canvas,
            destCanvas = FaceDetect.toCanvas,
            i;

        var destCtx = destCanvas.getContext('2d');
        var faces = ccv.detect_objects({ "canvas" : canvas,
                                "cascade" : cascade,
                                "interval" : 5,
                                "min_neighbors" : 1,
                                "async" : false,
                                "worker" : 1 });

        console.log("found: " + faces.length);
    },

    start : function() {
        /*
        console.log("<==== FACEDETECT STARTED ====>");
        if(FaceDetect.playing) { clearInterval(FaceDetect.playing); }
        FaceDetect.playing = setInterval(function() {
            FaceDetect.drawToCanvas();
        },100);
        */
    },

    stop : function() {
        /*
        console.log("<==== FACEDETECT STOPPED ====>");
        if(FaceDetect.playing) { clearInterval(FaceDetect.playing); }
        */
    }
};

FaceDetect.init = function(d) {

    console.log("<==== FACEDETECT INIT ====>");
    FaceDetect.canvas = document.getElementById('remoteVideo');
    FaceDetect.toCanvas = d;
    //FaceDetect.ctx = this.canvas.getContext("2d");

};
