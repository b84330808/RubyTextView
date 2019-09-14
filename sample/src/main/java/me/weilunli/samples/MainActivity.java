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

        RubyTextView rubyTextView = new RubyTextView(this);
        rubyTextView.setText("令和");
        rubyTextView.setRubyText("れい|わ");
        rubyTextView.setTextSize(30);
        rubyTextView.setRubyTextSize(14);
        rubyTextView.setRubyTextColor(getResources().getColor(R.color.red));
        rubyTextView.setSpacing(14);
        rubyTextView.setBackgroundColor(getResources().getColor(R.color.yellow));
        rubyTextView.setTextColor(getResources().getColor(R.color.blue));

        mainLayout = findViewById(R.id.linearLayout);
        mainLayout.addView(rubyTextView);


    }
}
