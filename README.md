[ ![Download](https://api.bintray.com/packages/b84330808/maven/rubytextview/images/download.svg) ](https://bintray.com/b84330808/maven/rubytextview/_latestVersion)


# RubyTextView
RubyTextView is an android view that can create TextView with ruby text, like furigana in Japanese or pinyin in Chinese.

## Preview
![preview](https://github.com/b84330808/RubyTextView/blob/master/screen_shot/preview.png)

## Gradle
```java
compile 'me.weilunli.views:rubytextview:1.0.0'
```
## XML Layout
```java
<me.weilunli.views.RubyTextView  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:text="天気が良いから、散歩しましょう"  
  android:textSize="20sp"  
  app:rubyText="てん き  い     さん ぽ"  
  app:rubyTextSize="10sp"  
  app:spacing="0sp"/>
  
<!------------------------------------>

<me.weilunli.views.RubyTextView  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:text="今天天氣很好，去散步吧"  
  android:textSize="26sp"
  app:rubyText="jīn tiān tiān qì hěn hǎo  qù sàn bù ba"  
  app:rubyTextSize="10sp"  
  app:spacing="5sp"/> 
```
## Java
```java
    RubyTextView rtv = (RubyTextView) findViewById(R.id.test);

    rtv.setText("令和")
            .setRubyText("れい わ");
            .setTextSize(30);
            .setRubyTextSize(14)
            .setSpacing(5)
            
```
## Attributes
`app:spacing="5sp"`: The spacing between text and ruby text.

## Known Issue
- Only single line is supported (multiline is not supported yet).
- `rubyText` is separated by the `space` in value. So if there are some word you do not want to ruby it, you have to add extra `space` by yourself. For example,  The ruby text of `"委員"` is `"い いん"`(one space in い and いん), but in case of  the ruby text of `"良い点"`, you have to put two space in  い and てん.

## TODO
- `setRubyTextColor`
- Make the processing of adding text and rubyText easily. 
- DEMO
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