# Shiraki(白木)

## Goal

- Show image files in the directory one by one
- Show comment on each image as well
- Can be operated by keyboard

## Usage

```
$ java -jar shiraki.jar [DIR] [INTERVAL(msec)]
```

Shows all jpeg files on the current directory (or DIR) in order of timestamp. Default interval is 4000msec.

## Operation with keyboard

- `SPACE` key ...Pause(toggle)
- Right arrow key ...Next image
- Left arrow key ...Previous image
- `I` key ...Show Exif information
- `ESCAPE` or `Q` key ...Exit

## メモ

- [xfce]フルスクリーンを解除したとき、ウィンドウサイズが大きいまま
- [mac]`setFullScreenWindow`でフルスクリーン化すると、Exif情報のメッセージボックスが隠れてしまう
- `GridBagLayout`のペインのサイズを知る方法は?
