package owt.sample.p2p;


import android.graphics.Canvas;
import android.graphics.Paint;

public class LineObject extends BaseObject{

    private float x1;
    private float y1;
    private float x2;
    private float y2;

    public LineObject(float x1, float y1, float x2, float y2, int color) {
        super(DrawType.LINE, color);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void drawObject(Canvas canvas, Paint paint) {
        float xTranslateFactor = 1.0f;
        float yTranslateFactor = 1.0f;

        paint.setColor(this.getObjColor());

        if(this.hasCanvasDimensions() && this.hasSourceDimensions()){
            // draw translated
            xTranslateFactor = (float) ((float) this.getCanvasWidth()/(float) this.getSrcWidth());
            yTranslateFactor = (float) ((float) this.getCanvasHeight()/(float) this.getSrcHeight());
/*
            Log.d("DrawLine","Source Width: " + this.getSrcWidth() + " Height: " + this.getSrcHeight() + "" );
            Log.d("DrawLine","Canvas Width: " + this.getCanvasWidth() + " Height: " + this.getCanvasHeight() + "" );
            Log.d("DrawLine","Translate x: " +  xTranslateFactor + " y: " + yTranslateFactor + "" );
*/
        }

        canvas.drawLine(x1 * xTranslateFactor, y1 * yTranslateFactor,
                x2 * xTranslateFactor, y2 * yTranslateFactor, paint);

    }

    public float getX1() {
        return x1;
    }

    public void setX1(float x1) {
        this.x1 = x1;
    }

    public float getY1() {
        return y1;
    }

    public void setY1(float y1) {
        this.y1 = y1;
    }

    public float getX2() {
        return x2;
    }

    public void setX2(float x2) {
        this.x2 = x2;
    }

    public float getY2() {
        return y2;
    }

    public void setY2(float y2) {
        this.y2 = y2;
    }
}
