package me.weilunli.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;


public class RubyTextView extends AppCompatTextView {

    private Bitmap bitmapToSaveCanvas;
    private Canvas tempCanvas;
    private TypedArray ta;
    private Paint textPaint;
    private Paint rubyTextPaint;
    private Rect mRect;
    private String rubyText;
    private String combinedText;
    private float rubyTextSize;
    private int rubyTextColor;
    private float spacing;
    String tempText;
    String tempRubyText;


    public RubyTextView(Context context) {
        super(context);
        initialize();
    }

    public RubyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();

        ta = getContext().obtainStyledAttributes(attrs, R.styleable.RubyTextView);
        try {
            combinedText = ta.getString(R.styleable.RubyTextView_combinedText);
            rubyText = ta.getString(R.styleable.RubyTextView_rubyText);
            rubyTextSize = ta.getDimension(R.styleable.RubyTextView_rubyTextSize, 50f);
            rubyTextColor = ta.getColor(R.styleable.RubyTextView_rubyTextColor,
                    ContextCompat.getColor(getContext(), R.color.black));
            spacing = ta.getDimension(R.styleable.RubyTextView_spacing, 10f);
            if(null != combinedText) setCombinedText(combinedText);

        } finally {
            ta.recycle();
        }

    }

    private void initialize() {
        rubyTextSize = 50f;
        rubyTextColor = ContextCompat.getColor(getContext(), R.color.black);
        spacing = 10f;
        textPaint = new Paint();
        rubyTextPaint = new Paint();
        mRect = new Rect();
        tempCanvas = new Canvas();
        tempText = "";
        tempRubyText = "";
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
        final int width = getMySize(widthMeasureSpec,
                (int) Math.max((getText().length() * getTextSize()),
                        getRubyText().length()*getRubyTextSize()));
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
        rubyTextPaint.setColor(rubyTextColor);
        String[] split = getRubyText().split("\\|");
        for (int i = 0; i < split.length; i++) {
            rubyTextPaint.getTextBounds(split[i], 0, split[i].length(), mRect);
            float rubyTextXpos = getTextSize() * (i + (1 / 2f)) - (mRect.width() * (1 / 2f));
            float rubyTextYpos = canvas.getHeight() - getTextSize() - getSpacing();
            canvas.drawText(split[i], rubyTextXpos, rubyTextYpos, rubyTextPaint);
        }

    }





    public String getRubyText() {
        return null == rubyText?"":rubyText;
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

    public String getCombinedText() {
        return null == combinedText?"":combinedText;
    }

    public void setRubyTextColor(int color) {
        rubyTextColor = color;
        invalidate();
    }

    public void setCombinedText(String text) {
        combinedText = text;
        tempText = "";
        tempRubyText ="";

        String[] split = text.split(" ");
        for(int i = 0; i < split.length; i++){
            String[] t = split[i].split("\\|");
            tempText+=(t[0]);
            if(t.length>=2)tempRubyText += ("|"+t[1]);
            else for(int j = 0;j<t[0].length();j++) tempRubyText += "|";
        }
        setText(tempText);
        setRubyText(tempRubyText.substring(1));
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
