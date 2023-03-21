# Shiraki(白木)

## Goal

- Show image files in the directory one by one
- Show comment on each image as well
  - comments are read from `UserComment` entry of Exif meta data
  - or the comment file
- Can be operated by keyboard

## Usage

```
$ java -jar shiraki.jar [DIR] [INTERVAL(msec)]
```

Shows all jpeg files on the current directory (or DIR) in order of timestamp. Default interval is 4000msec.

## Comment file

The comment file is written in edn format and placed in the same directory as image files. It consists of a hash map, whose key is file name and value is comment.

```
{
 "1.JPG" "Cool!"
 :2.JPG "ナイス!"
 }
 ```

## Operation with keyboard

- `SPACE` key ...Pause(toggle) slideshow
- Right arrow key ...Show next image
- Left arrow key ...Show previous image
- `I` key ...Show Exif meta data
- `ESCAPE` or `Q` key ...Quit the application

## メモ

- [xfce]フルスクリーンを解除したとき、ウィンドウサイズが大きいまま
- [mac]`setFullScreenWindow`でフルスクリーン化すると、Exif情報のメッセージボックスが隠れてしまう
- [mac]OS Xネイティブのフルスクリーンモードを使う方法は?
- `GridBagLayout`のペインのサイズを知る方法は?
