[ ![Download](https://api.bintray.com/packages/b84330808/maven/rubytextview/images/download.svg) ](https://bintray.com/b84330808/maven/rubytextview/_latestVersion)

# RubyTextView
RubyTextView is an android view that can create TextView with ruby text, like furigana in Japanese or pinyin in Chinese.

## Preview
<img src="https://github.com/b84330808/RubyTextView/blob/master/screen_shot/preview.jpg" alt="preview" width="300"/>


## Gradle
```java
compile 'me.weilunli.views:rubytextview:1.1.0'
```
## How to use
### XML Layout (Two methods)
#### Method1: Set `app:combinedText` (Recommend)

The `app:combinedText` format is like this `令|れい 和|わ`. 
1. Put the `|` between the text and ruby text 
2. put one ` ` (white space) before text and one ` ` after ruby text.
```java
<me.weilunli.views.RubyTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="30sp"
        app:rubyTextSize="16sp"
        app:combinedText="天|てん 気|き が 良|い い  "/>

<me.weilunli.views.RubyTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="32sp"
        app:rubyTextSize="16sp"
        app:combinedText="我|wǒ 喜|xǐ 歡|huān 吃香 蕉|jiāo"/>
```
##### Method2: Set `android:text` and `app:rubyText` seperately.
***In this methond, you have to count the characters skipped by yourself.***

The `app:rubyText` format is like this `れい|わ`. 

```java
<me.weilunli.views.RubyTextView  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:text="平成"  
  android:textSize="26sp"
  app:rubyText="へい|せい"  
  app:rubyTextSize="10sp"
  app:spacing="5sp"/> 


<!------>

<me.weilunli.views.RubyTextView  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:text="天気が良いから、散歩しましょう"  
  android:textSize="20sp"  
  app:rubyText="てん|き||い|||||さん|ぽ"  
  app:rubyTextSize="10sp"
  app:rubyTextColor="@color/black"/>

```
###  Java
```java
RubyTextView rtv = (RubyTextView) findViewById(R.id.test);

rtv.setText("令和")
   .setRubyText("れい|わ");
   .setTextSize(30);
   .setRubyTextSize(14)
   .setRubyTextColor(getResources().getColor(R.color.red)
   .setSpacing(5) 
   .setCombinedText("平|へい 成|せい") 
```
## Attributes
`app:rubyText `: The ruby text onto the text, split by whitespace character.
`app:rubyTextSize `: The size of ruby text.
`app:rubyTextColor`: The color of ruby text.
`app:spacing`: The spacing between text and ruby text.
`app:setCombinedText`: Set the text and ruby text simultaneously by specific format mentioned above.



## Known Issue
- Only single line is supported (multiline is not supported yet).
- `rubyText` string is split by `|` character. So if there are some word you do not want to ruby it, you have to add extra `|` by yourself. For example,  The ruby text of `"委員"` is `"い|いん"`, but in case of  the ruby text of `"良い点"` is `い||てん`.

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