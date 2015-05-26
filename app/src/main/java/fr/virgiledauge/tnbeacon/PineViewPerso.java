package fr.virgiledauge.tnbeacon;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
/**
 * Created by virgile on 25/05/15.
 */
public class PineViewPerso extends SubsamplingScaleImageView{
    private PointF sPin;
    private Bitmap pin;
    public PineViewPerso(Context context) {
        this(context, null);
    }
    public PineViewPerso(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }
    public void setPin(PointF sPin) {
        this.sPin = sPin;
        initialise();
        invalidate();
    }
    public PointF getPin() {
        return sPin;
    }
    private void initialise() {
        /*
        float density = getResources().getDisplayMetrics().densityDpi;
        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.map0);
        float w = (density/420f) * pin.getWidth();
        float h = (density/420f) * pin.getHeight();
        pin = Bitmap.createScaledBitmap(pin, (int)w, (int)h, true);
        */
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        if (sPin != null && pin != null) {
            PointF vPin = sourceToViewCoord(sPin);
            float vX = vPin.x - (pin.getWidth()/2);
            float vY = vPin.y - pin.getHeight();
            canvas.drawBitmap(pin, vX, vY, paint);
        }
    }
}