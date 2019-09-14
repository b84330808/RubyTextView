package me.weilunli.samples;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import me.weilunli.views.RubyTextView;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RubyTextView rtv1 = new RubyTextView(this);
        rtv1.setText("令和");
        rtv1.setRubyText("れい|わ");
        rtv1.setTextSize(30);
        rtv1.setRubyTextSize(14);
        rtv1.setRubyTextColor(getResources().getColor(R.color.red));
        rtv1.setSpacing(14);
        rtv1.setBackgroundColor(getResources().getColor(R.color.yellow));
        rtv1.setTextColor(getResources().getColor(R.color.blue));

        RubyTextView rtv2 = new RubyTextView(this);
        rtv2.setTextSize(30);
        rtv2.setRubyTextSize(14);
        rtv2.setRubyTextColor(getResources().getColor(R.color.red));
        rtv2.setSpacing(14);
        rtv2.setTextColor(getResources().getColor(R.color.blue));
        rtv2.setCombinedText("平|へい 成|せい");

        mainLayout = findViewById(R.id.linearLayout);
        mainLayout.addView(rtv1);
        mainLayout.addView(rtv2);


    }
}
