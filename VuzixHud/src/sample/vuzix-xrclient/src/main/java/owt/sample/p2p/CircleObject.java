package owt.sample.p2p;


import android.graphics.Canvas;
import android.graphics.Paint;

public class CircleObject extends BaseObject {
    private float originX;
    private float originY;
    private float radius;

    public CircleObject(float originX, float originY, float radius, int color) {
        super(DrawType.CIRCLE, color);
        this.originX = originX;
        this.originY = originY;
        this.radius = radius;
    }

    public float getOriginX() {
        return originX;
    }

    public void setOriginX(float originX) {
        this.originX = originX;
    }

    public float getOriginY() {
        return originY;
    }

    public void setOriginY(float originY) {
        this.originY = originY;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void drawObject(Canvas canvas, Paint paint) {
        float xTranslateFactor = 1.0f;
        float yTranslateFactor = 1.0f;
        float rTranslateFactor = 1.0f;

        paint.setColor(this.getObjColor());
        if(this.hasCanvasDimensions() && this.hasSourceDimensions()){
            xTranslateFactor = (float) ((float) this.getCanvasWidth()/(float) this.getSrcWidth());
            yTranslateFactor = (float) ((float) this.getCanvasHeight()/(float) this.getSrcHeight());
            rTranslateFactor = (float)((xTranslateFactor + yTranslateFactor)/2);
        }

        canvas.drawCircle(originX * xTranslateFactor, originY * yTranslateFactor,
                radius * rTranslateFactor, paint);
    }
}
