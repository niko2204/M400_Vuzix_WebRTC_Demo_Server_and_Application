var drawingApp = (function () {

    "use strict";

    var canvas,
        tempcanvas,
		context,
		tempcontext,
		mousePressed = false,
        drawHandler,
		drawType = 'line',
		width,
		height,
		offX,
		offY,
		x1,
		y1,
		lastX,
		lastY,
		bDrawEnable = false,
		bReset = false,

		draw = function (ctx, x, y, isDown) {
		    if (document.getElementById('overlayCanvas').getContext('2d').canvas.width !=
				$('#overlayCanvas').width()) {
		        width = document.getElementById('overlayCanvas').getContext('2d').canvas.width = $('#overlayCanvas').width();
		        height = document.getElementById('overlayCanvas').getContext('2d').canvas.height = $('#overlayCanvas').height();
		    }


		    if (document.getElementById('tempCanvas').getContext('2d').canvas.width !=
				$('#tempCanvas').width()) {
		        width = document.getElementById('tempCanvas').getContext('2d').canvas.width = $('#tempCanvas').width();
		        height = document.getElementById('tempCanvas').getContext('2d').canvas.height = $('#tempCanvas').height();
		    }

		    lastX = x;
		    lastY = y;

		    // clear only the temp drawing surface
		    if (ctx == tempcontext) {
		        ctx.clearRect(0, 0, $('#tempCanvas').width(), $('#tempCanvas').height());
		    }

		    switch (drawType) {
		        case 'line':
		            drawLine(ctx, x1, y1, x, y);
		            break;
		        case 'circle':
		            drawCircle(ctx, x1, y1, x, y);
		            break;
		        case 'square':
		            drawRectangle(ctx, x1, y1, x, y);
		            break;
		        default:
		            break;
		    }
		},

        drawFinal = function (ctx) {
            switch (drawType) {
                case 'line':
                    drawLine(ctx, x1, y1, lastX, lastY);
                    break;
                case 'circle':
                    drawCircle(ctx, x1, y1, lastX, lastY);
                    break;
                case 'square':
                    drawRectangle(ctx, x1, y1, lastX, lastY);
                    break;
                default:
                    break;
            }
        },

		sendMessageToHud = function () {
		    if (drawHandler == undefined) {
		        console.log("no draw handler registered");
		        return;
		    }

		    // console.log("Send Message to HUD");
		    switch (drawType) {
		        case 'line':
		            drawHandler("line", "#00FF00", x1, y1, lastX, lastY, width, height);
		            break;
		        case 'circle':
		            drawHandler("circle", "#FFFFFF", x1, y1, lastX, lastY, width, height);
		            break;
		        case 'square':
		            drawHandler("square", "#FF0000", x1, y1, lastX, lastY, width, height);
		            break;
		        default:
		            break;
		    }
		},

		clearCanvas = function () {
		    // console.log("Clear Canvas");
		    // Use the identity matrix while clearing the canvas
		    context.setTransform(1, 0, 0, 1, 0, 0);
		    context.clearRect(0, 0, $('#overlayCanvas').width(), $('#overlayCanvas').height());
		    tempcontext.setTransform(1, 0, 0, 1, 0, 0);
		    tempcontext.clearRect(0, 0, $('#tempCanvas').width(), $('#tempCanvas').height());
		    bReset = true;

		    if (drawHandler != undefined) {
		        drawHandler("clear", 0, 0, 0, 0, 0);
		    }
		},

        stopDrawingSurface = function () {
            if (canvas != null) {
                console.log('<==== Stop Drawing ====>');
                var v = document.getElementById("remote");
                context.clearRect(0, 0, canvas.width, canvas.height);
                tempcontext.clearRect(0, 0, tempcanvas.width, tempcanvas.height);

                v.removeChild(tempcanvas);
                v.removeChild(canvas);

                tempcanvas = null;
                canvas = null;
            }
        },

		// Creates a canvas element, loads images, adds events, and draws the canvas for the first time.
		initDrawingSurface = function () {
		    console.log('<==== Init Drawing ====>');
		    // Create the canvas (Neccessary for IE because it doesn't know what a canvas element is)
		    tempcanvas = document.createElement('canvas');
		    tempcanvas.setAttribute('id', 'tempCanvas');
		    tempcanvas.className = 'sxtmpoverlaycanvas';

		    document.getElementById("remote").appendChild(tempcanvas);

		    tempcanvas.addEventListener("mousedown", overlayOnMouseDown, false);
		    tempcanvas.addEventListener("mousemove", overlayOnDrag, false);
		    tempcanvas.addEventListener("mouseup", overlayOnMouseUp);
		    tempcanvas.addEventListener("mouseout", overlayOnCancel, false);
		    tempcontext = tempcanvas.getContext("2d"); // Grab the 2d canvas context

		    canvas = document.createElement('canvas');
		    canvas.setAttribute('id', 'overlayCanvas');
		    canvas.className = 'sxoverlaycanvas';
		    //canvas.style.border = '1px solid red';
		    document.getElementById("remote").appendChild(canvas);

		    canvas.addEventListener("mousedown", handleOnMouseDown, false);
		    /*
            canvas.addEventListener("mousemove", overlayOnDrag, false);
            canvas.addEventListener("mouseup", overlayOnMouseUp);
            canvas.addEventListener("mouseout", overlayOnCancel, false);
            */
		    context = canvas.getContext("2d"); // Grab the 2d canvas context
		    getPos(canvas);
		};

    function drawLine(ctx, x1, y1, x2, y2) {
        // console.log("Draw Line");
        ctx.beginPath();
        ctx.strokeStyle = "#00FF00";
        ctx.lineWidth = 4;
        ctx.lineJoin = "round";
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.closePath();
        ctx.stroke();
    }

    function drawCircle(ctx, x1, y1, x2, y2) {
        // console.log("Draw Circle");
        var a = x2 - x1,
			b = y2 - y1,
			radius = Math.sqrt(a * a + b * b);

        ctx.beginPath();
        ctx.arc(x1, y1, radius, 0, 2 * Math.PI, false);
        ctx.lineWidth = 4;
        ctx.strokeStyle = '#FFFFFF';
        ctx.stroke();
    }

    function drawRectangle(ctx, x1, y1, x2, y2) {
        var w = x2 - x1,
			h = y2 - y1;

        ctx.beginPath();
        ctx.rect(x1, y1, w, h);
        ctx.fillStyle = 'rgba(255, 0, 0, 0.4)';
        ctx.fill();
        ctx.lineWidth = 4;        
        ctx.strokeStyle = '#FF0000';
        ctx.stroke();
        // console.log("Draw Rectangle");

    }

    function drawEllipse(ctx, x1, y1, x2, y2) {
        // console.log("Draw Ellipse");
        var radiusX = (x2 - x1) * 0.5,
			radiusY = (y2 - y1) * 0.5,
			centerX = x1 + radiusX,
			centerY = y1 + radiusY,
			step = 0.01,
			a = step,
			pi2 = Math.PI * 2 - step;

        ctx.beginPath();
        ctx.moveTo(centerX + radiusX * Math.cos(0),
				centerY + radiusY * Math.sin(0));

        for (; a < pi2; a += step) {
            ctx.lineTo(centerX + radiusX * Math.cos(a),
					centerY + radiusY * Math.sin(a));
        }

        ctx.closePath();
        ctx.lineWidth = 4;
        ctx.strokeStyle = "#FFFFFF";
        ctx.stroke();
    }

    function handleOnMouseDown(e) {
        // console.log("Handle OnMouseDown");
        if (!mousePressed) {
            // new start

            if (document.getElementById('overlayCanvas').getContext('2d').canvas.width !=
				$('#overlayCanvas').width()) {
                width = document.getElementById('overlayCanvas').getContext('2d').canvas.width = $('#overlayCanvas').width();
                height = document.getElementById('overlayCanvas').getContext('2d').canvas.height = $('#overlayCanvas').height();
            }

            if (document.getElementById('tempCanvas').getContext('2d').canvas.width !=
				$('#tempCanvas').width()) {
                width = document.getElementById('tempCanvas').getContext('2d').canvas.width = $('#tempCanvas').width();
                height = document.getElementById('tempCanvas').getContext('2d').canvas.height = $('#tempCanvas').height();
            }

            var rect = canvas.getBoundingClientRect();
            x1 = e.clientX - offX;
            y1 = e.clientY - offY;
            lastX = x1;
            lastY = y1;

            tempcontext.clearRect(0, 0, rect.width, rect.height);
            $("#tempCanvas").css({ left: 0, top: 0 });
            mousePressed = true;
        }
    }

    function overlayOnMouseDown(e) {
        // console.log("Overlay OnMouseDown");
        tempcanvas.style.cursor = "crosshair";
        if (!bDrawEnable)
            return;

        if (!mousePressed) {
            // new start
            var rect = tempcanvas.getBoundingClientRect();
            x1 = e.clientX - offX;
            y1 = e.clientY - offY;
            lastX = x1;
            lastY = y1;

            mousePressed = true;
        }
    }

    function overlayOnDrag(e) {
        if (mousePressed) {
            tempcanvas.style.cursor = "crosshair";
            if (!bDrawEnable)
                return;
            // console.log(e);
            draw(tempcontext, e.clientX - offX, e.clientY - offY, true);
        }

    }

    function overlayOnMouseUp(e) {
        // console.log("Overlay MouseUp");
        tempcanvas.style.cursor = "default";
        if (mousePressed && bDrawEnable) {
            // finished drawing send to hud draw to main canvas
            drawFinal(context);
            sendMessageToHud();
        }
        mousePressed = false;
    }

    function overlayOnCancel(e) {
        tempcanvas.style.cursor = "default";
        if (mousePressed && bDrawEnable) {
            // finished drawing send to hud draw to main canvas
            drawFinal(context);
            sendMessageToHud();
        }
        mousePressed = false;
    }

    function getPos(el) {
        for (var lx = 0, ly = 0;
			el != null;
			lx += el.offsetLeft, ly += el.offsetTop, el = el.offsetParent);

        offX = lx;
        offY = ly;
        return { x: lx, y: ly };
    }

    return {
        init: initDrawingSurface,
        stop: stopDrawingSurface,
        clear: clearCanvas,
        setDrawHandler: function (handler) {
            drawHandler = handler;
        },
        setDrawEnable: function (bEnable) {

            bDrawEnable = bEnable;
        },
        setPenStyle: function (penStyle) {

            drawType = penStyle;
        }
    };
}());
