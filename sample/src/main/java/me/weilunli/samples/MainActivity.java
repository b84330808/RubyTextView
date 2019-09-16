package me.weilunli.samples;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import me.weilunli.views.RubyTextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RubyTextView rtv = new RubyTextView(this);
        rtv.setCombinedText("全|すべ ての 瞬間|しゅんかん に 価値|かち がある。あなたの 心|こころ に 従|したが いなさい。");
        rtv.setTextSize(24);
        rtv.setRubyTextSize(12);
        rtv.setRubyTextColor(getResources().getColor(R.color.green)); //OK
        rtv.setTextColor(getResources().getColor(R.color.blue)); //OK
        rtv.setBackgroundColor(getResources().getColor(R.color.bg));
        rtv.setSpacing(2);
        rtv.setLetterSpacing(0.1f);

        LinearLayout mainLayout = findViewById(R.id.linearLayout);
        mainLayout.addView(rtv);

    }

}
