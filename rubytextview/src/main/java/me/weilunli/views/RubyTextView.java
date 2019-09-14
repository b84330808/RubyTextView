package me.weilunli.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;


public class RubyTextView extends AppCompatTextView {

    private Bitmap bitmapToSaveCanvas;
    private Canvas tempCanvas;
    private TypedArray ta;
    private Paint textPaint;
    private Paint rubyTextPaint;
    private Rect mRect;
    private String rubyText;
    private float rubyTextSize;
    private float spacing;


    public RubyTextView(Context context) {
        super(context);
        initialize();
    }

    public RubyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();

        ta = getContext().obtainStyledAttributes(attrs, R.styleable.RubyTextView);
        try {
            rubyText = ta.getString(R.styleable.RubyTextView_rubyText);
            rubyTextSize = ta.getDimension(R.styleable.RubyTextView_rubyTextSize, 50f);
            spacing = ta.getDimension(R.styleable.RubyTextView_spacing, 10f);
        } finally {
            ta.recycle();
        }
    }

    private void initialize() {
        rubyText = "";
        rubyTextSize = 50f;
        spacing = 10f;
        textPaint = new Paint();
        rubyTextPaint = new Paint();
        mRect = new Rect();
        tempCanvas = new Canvas();
    }


    private int getMySize(int measureSpec, int mBoundLength) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec); //get px
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(mBoundLength, specSize);
        } else {
            result = mBoundLength;
        }
        return result;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        textPaint.setTextSize(getTextSize()); //set px
        final int width = getMySize(widthMeasureSpec, (int) (getText().length() * getTextSize()));
        final int height = getMySize(heightMeasureSpec, (int) (getTextSize() + rubyTextSize + spacing));

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // A trade off to draw canvas on canvas
        float textXpos = 0;
        float textYpos = canvas.getHeight() - getTextSize() - textPaint.descent();
        bitmapToSaveCanvas = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        tempCanvas.setBitmap(bitmapToSaveCanvas);
        super.onDraw(tempCanvas);
        canvas.drawBitmap(bitmapToSaveCanvas, textXpos, textYpos, null);

        // Draw ruby text
        rubyTextPaint.setTextSize((getRubyTextSize()));
        String[] splited = getRubyText().split(" ");
        for (int i = 0; i < splited.length; i++) {
            rubyTextPaint.getTextBounds(splited[i], 0, splited[i].length(), mRect);
            float rubyTextXpos = getTextSize() * (i + (1 / 2f)) - (mRect.width() * (1 / 2f));
            float rubyTextYpos = canvas.getHeight() - getTextSize() - getSpacing();
            canvas.drawText(splited[i], rubyTextXpos, rubyTextYpos, rubyTextPaint);
        }

    }


    public String getRubyText() {
        return rubyText;
    }

    public float getRubyTextSize() {
        return rubyTextSize;
    }

    public float getSpacing() {
        return spacing;
    }


    public void setRubyText(String text) {
        rubyText = text;
        invalidate();
        requestLayout();
    }

    public void setRubyTextSize(float rubyTextSize) {
        this.rubyTextSize = sp2px(rubyTextSize);
        invalidate();
        requestLayout();
    }

    public void setSpacing(float spacing) {
        this.spacing = dp2px(spacing);
        invalidate();
        requestLayout();
    }


    /**
     * convert dp to its equivalent px
     */
    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    /**
     * convert sp to its equivalent px
     */
    private float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }


}
