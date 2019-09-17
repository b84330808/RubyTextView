[ ![Download](https://api.bintray.com/packages/b84330808/maven/rubytextview/images/download.svg) ](https://bintray.com/b84330808/maven/rubytextview/_latestVersion)

# RubyTextView
RubyTextView is an android view that can create TextView with ruby text, like furigana in Japanese or pinyin in Chinese.

## Preview
<img src="https://github.com/b84330808/RubyTextView/blob/master/screen_shot/preview.jpg" alt="preview" width="300"/>
<img src="https://github.com/b84330808/RubyTextView/blob/master/screen_shot/note.jpg" alt="preview" width="700"/>


## Gradle
```java
compile 'me.weilunli.views:rubytextview:1.3.1'
```
## How to use
### XML Layout 
####  Set `app:combinedText`

The `app:combinedText` format is like this `令|れい 和|わ`. 
1. Put the `|` between the text and ruby text 
2. put one ` ` (white space) before text and one ` ` after ruby text.
```java

// example 1
<me.weilunli.views.RubyTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="30sp"
        app:combinedText="令|れい 和|わ"
        app:rubyTextSize="16sp"
        app:spacing="1sp"/>

// example 2
<me.weilunli.views.RubyTextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="10dp"
    android:textSize="30sp"
    app:combinedText="天|tiān 气|qì 很|hěn 好|hǎo 、 去|qù 散|sàn 步|bù 吧|ba"
    app:rubyTextSize="14sp"
    android:textColor="@color/green"
    android:background="@color/bg"
    app:rubyTextColor="@color/blue"
    app:spacing="5sp"/>

```

###  Java
```java
RubyTextView rtv = (RubyTextView) findViewById(R.id.test);

rtv.setCombinedText("全|すべ ての 瞬間|しゅんかん に 価値|かち がある。");
    setTextSize(24);
    setRubyTextSize(12);
    setRubyTextColor(getResources().getColor(R.color.green));
    setTextColor(getResources().getColor(R.color.blue));
    setBackgroundColor(getResources().getColor(R.color.bg));
    setSpacing(2);
    setLetterSpacing(0.1f);
```
## Attributes
- `app:combinedText`: The text and ruby text. e.g.`你|nǐ  好|hǎo` 
- `app:rubyTextSize`: The size of ruby text.
- `app:rubyTextColor`: The color of ruby text.
- `app:spacing`: The spacing between text and ruby text.

## Known Issue
- If there are some English sentences in `combinedText`, some bugs will occur. 

<!-- ## TODO
- Make the processing of adding text and rubyText easily.  -->

# License
```
Copyright 2019 WEI-LUN LI

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```