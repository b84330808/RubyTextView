package me.weilunli.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class RubyTextView extends AppCompatTextView {

    private Paint textPaint;
    private Paint rubyTextPaint;
    private String combinedText = "";
    private float rubyTextSize= 28f;
    private int rubyTextColor ;
    private float spacing = 0f;

    // Need to address first line because it don't need extra spacing.
    private float lineheight = 0;
    private float firstLineheight = 0;
    StringBuilder originalText;
    List<String[]> combinedTextArray;


    public RubyTextView(Context context) {
        super(context);
        initialize();
        setValue();
    }

    public RubyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.RubyTextView);
        try {
            combinedText = ta.getString(R.styleable.RubyTextView_combinedText);
            rubyTextSize = ta.getDimension(R.styleable.RubyTextView_rubyTextSize, 28f);
            rubyTextColor = ta.getColor(R.styleable.RubyTextView_rubyTextColor,
                    ContextCompat.getColor(getContext(), R.color.black));
            spacing = ta.getDimension(R.styleable.RubyTextView_spacing, 0);

        } finally {
            ta.recycle();
        }

        setValue();
    }


    private void initialize() {
        textPaint = getPaint();
        rubyTextPaint = new Paint();
        originalText = new StringBuilder();
        rubyTextColor = getCurrentTextColor();
        combinedTextArray = new ArrayList<>();
    }


    private void setValue() {
        textPaint.setColor(getCurrentTextColor());
        rubyTextPaint.setTextSize((getRubyTextSize()));
        rubyTextPaint.setColor(getRubyTextColor());
        lineheight = getTextSize() + getRubyTextSize() + getLineSpacingExtra() + getSpacing();
        firstLineheight = lineheight - getLineSpacingExtra();
        splitCombinedText();
        setLineHeight((int) lineheight);
    }


    private int getMySize(int measureSpec, int mBoundLength) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
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

        int width = MeasureSpec.getSize(widthMeasureSpec);
        float cur_x = 0;
        int lineCount = 1;

        for(String[] t : combinedTextArray) {
            float textWidth = textPaint.measureText(t[0]);

            // if t[0] == '\n'
            if(t[0].equals(System.getProperty("line.separator"))){
                cur_x = 0;
                lineCount++;
                continue;
            }

            if (cur_x + textWidth > width){
                cur_x = 0;
                lineCount++;
            }

            cur_x += textWidth;
        }

        // total height
        int height = getMySize(heightMeasureSpec,
                (int) (firstLineheight + lineheight * (lineCount-1)) + getLastBaselineToBottomHeight());
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean isFirstLine = true;
        float cur_x = 0;
        float cur_y = firstLineheight;
        for(String[] t : combinedTextArray) {
            /* **********
             * Draw text *
             * ***********/
            float textWidth = textPaint.measureText(t[0]);

            if(t[0].equals(System.getProperty("line.separator"))){
                cur_x = 0;
                if(isFirstLine) isFirstLine = false;
                cur_y += lineheight;
                continue;
            }

            if (cur_x + textWidth > getWidth()) {
                cur_x = 0;
                if(isFirstLine) isFirstLine = false;
                cur_y += lineheight;
            }
            canvas.drawText(t[0], cur_x, cur_y, textPaint);

            /* ****************
             * Draw ruby text *
             * ****************/
            float rubyText_posX = cur_x +
                    (1 / 2f) * (textWidth - rubyTextPaint.measureText(t[1]));
            canvas.drawText(t[1], rubyText_posX, cur_y - getTextSize() - getSpacing(), rubyTextPaint);

            // update cur_x position
            cur_x += textWidth;
        }
    }

    public String getCombinedText() {
        return combinedText;
    }
    public float getRubyTextSize() {
        return rubyTextSize;
    }
    public float getSpacing() {
        return spacing;
    }
    public int getRubyTextColor() {
        return rubyTextColor;
    }

    private void updateLineheight(){
        lineheight = getTextSize() + getRubyTextSize() + getLineSpacingExtra() + getSpacing();
        firstLineheight = lineheight - getLineSpacingExtra();
    }

    public void setCombinedText(String text) {
        combinedText = text;
        splitCombinedText();
        requestLayout();
        invalidate();
    }
    public void setRubyTextSize(float textSize) {
        rubyTextSize = sp2px(textSize);
        rubyTextPaint.setTextSize(rubyTextSize);
        updateLineheight();
        invalidate();
        requestLayout();
    }
    public void setRubyTextColor(int color) {
        rubyTextColor = color;
        rubyTextPaint.setColor(rubyTextColor);
        invalidate();
    }

    @Override
    public void setLetterSpacing(float letterSpacing) {
        super.setLetterSpacing(letterSpacing);
        invalidate();
        requestLayout();
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        updateLineheight();
        requestLayout();
        invalidate();
    }

    public void setSpacing(float spacing) {
        this.spacing = dp2px(spacing);
        updateLineheight();
        invalidate();
        requestLayout();
    }

    @Override
    public void setTextColor(int color) {
        textPaint.setColor(color);
        super.setTextColor(color);
    }

    public void splitCombinedText() {
        combinedTextArray.clear();
        originalText.setLength(0);
        String[] split = getCombinedText().split(" ");
        for (String value : split) {
            String[] t = value.split("\\|");
            if (t.length == 2) {
                if ((t[1].equals("-"))) {
                    combinedTextArray.add(new String[]{t[0], ""});
                } else {
                    combinedTextArray.add(new String[]{t[0], t[1]});
                }
            } else {
                for (int j = 0; j < t[0].length(); j++) {
                    String s = String.valueOf(t[0].charAt(j));
                    combinedTextArray.add(new String[]{s, ""});
                }
            }
            originalText.append(t[0]);
        }
        setText(originalText);
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
