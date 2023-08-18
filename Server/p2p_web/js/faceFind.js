canvas = null;
run = false;
running = null;
oldComp = null;


function initFaceFind(run){
    //this.canvas = canvas;
    this.run=run;
    faceFind();
}

function faceFind(){
    console.log('faceFind() called');
    if(this.run)
    {
        this.running = requestAnimationFrame(faceFind);
    } else {
        cancelAnimationFrame(running);
        tempCan = document.getElementById('faceFindCanvas');
        tempCtx = tempCan.getContext("2d");
        tempCtx.clearRect(0,0,tempCan.width,tempCan.height);
    }
    //var image = document.getElementById('remoteVideo');
    this.canvas = document.getElementById('tempCanvas');

    if(this.canvas != null){

        var ctx = this.canvas.getContext("2d");

        scale=1;

        var testCanvas = document.getElementById('faceFindCanvas');

        //var testCanvas = document.createElement('canvas');
        
        //testCanvas.width=this.canvas.width;
        //testCanvas.height=this.canvas.height;
        //testCanvas.style.visibility="hidden";
        if (testCanvas != null)
        {
            //testCanvas.style.visibility="hidden";
            var displayCanvas = document.getElementById("overlayCanvas");
            var displayCtx = displayCanvas.getContext("2d");
            var testCtx = testCanvas.getContext("2d");
            //displayCtx.clearRect(0,0,displayCanvas.width,displayCanvas.height);

            //testCtx.save();

            clearOld();
            //

            testCtx.drawImage(document.getElementById("remoteVideo"),0,0,this.canvas.width,this.canvas.height);

            //testCtx.putImageData(ctx.getImageData(0,0,this.canvas.width,this.canvas.height),0,0);

            //getImage();

            //faceapi.drawDetection(canvas, fd.detection, { withScore: false })

            function post(comp) {
                //document.getElementById("num-faces").innerHTML = comp.length.toString();
                //document.getElementById("detection-time").innerHTML = Math.round((new Date()).getTime() - elapsed_time).toString() + "ms";
                
                displayCtx.globalCompositeOperation="source-over";
                displayCtx.lineWidth = 2;
                displayCtx.strokeStyle = 'rgba(230,87,0,1)';
                for (var i = 0; i < comp.length; i++) {
                    drawHandler("face", "#FFA500", (comp[i].x + comp[i].width * 0.5) * scale, (comp[i].y + comp[i].height * 0.5), (comp[i].x + comp[i].width * 0.5) * scale + (comp[i].width + comp[i].height) * 0.25 * scale * 1.2, (comp[i].y + comp[i].height * 0.5) * scale,  document.getElementById('overlayCanvas').width,  document.getElementById('overlayCanvas').height); //.arc((comp[i].x + comp[i].width * 0.5) * scale, (comp[i].y + comp[i].height * 0.5) * scale, (comp[i].width + comp[i].height) * 0.25 * scale * 1.2, 0, Math.PI * 2);                    
                    displayCtx.beginPath();
                    displayCtx.arc((comp[i].x + comp[i].width * 0.5) * scale, (comp[i].y + comp[i].height * 0.5) * scale, (comp[i].width + comp[i].height) * 0.25 * scale * 1.2, 0, Math.PI * 2);
                    displayCtx.stroke();
                }
            }

            function clearOld(){
                displayCtx.lineWidth = 4;
                displayCtx.globalCompositeOperation="source-in";
                displayCtx.strokeStyle = 'rgba(230,87,0,0)';
                /* draw detected area */
                if(this.oldComp != null)
                {
                    for (var i = 0; i < this.oldComp.length; i++) {
                        displayCtx.beginPath();
                        displayCtx.arc((this.oldComp[i].x + this.oldComp[i].width * 0.5) * scale, (this.oldComp[i].y + this.oldComp[i].height * 0.5) * scale,
                                (this.oldComp[i].width + this.oldComp[i].height) * 0.25 * scale * 1.2, 0, Math.PI * 2);
                                displayCtx.stroke();
                    }
                }
            }

            /* call main detect_objects function */
            if (false) {
                ccv.detect_objects({ "canvas" : testCanvas,
                                    "cascade" : cascade,
                                    "interval" : 5,
                                    "min_neighbors" : 1,
                                    "async" : true,
                                    "worker" : 1 })(post);
            } else {
                var comp = ccv.detect_objects({ "canvas" : ccv.grayscale(testCanvas),
                                                "cascade" : cascade,
                                                "interval" : 5,
                                                "min_neighbors" : 1 });

                //testCtx.restore();
                drawHandler("clface", 0, 0, 0, 0, 0);
                testCtx.clearRect(0,0,testCanvas.width,testCanvas.height);
                post(comp);
                oldComp=comp;
            }
        }
    }
}