# Encoding video for the app

How to prepare the mp4 files the app plays from `res/raw` through media3 (ExoPlayer) into a SurfaceView: the Welcome
background, story slides and anything similar. Derived from [REDACTED_TASK_KEY] and [REDACTED_TASK_KEY].

## The main rule

By default the x264 `veryslow` preset sets 16 reference frames and up to 8 consecutive B-frames, and puts keyframes at
scene cuts and otherwise only every 250 frames (the default `keyint`), which is over ten seconds at 24 fps. The file
comes out compact and valid, but heavy for a phone's hardware decoder. That is why every recipe sets the reference
frames, the B-frames and the keyframe interval explicitly.

## Requirements and why

| Parameter | Value | Why |
|---|---|---|
| Codec and format | H.264 High, 8-bit, `yuv420p` | Hardware decoders exist everywhere. HEVC/VP9/AV1 gave no gain on our clips ([REDACTED_TASK_KEY]). |
| Level | set by x264 from the resolution and fps | 1080×1920@24 fits 4.1. 1080×2340@60 needs 5.1, but the danger is the number of reference frames, not the level. |
| Frame rate | the master's own: no `-r`, no `fps` filter | A higher rate only duplicates or interpolates frames; a smoother clip needs a new master from design. 24 fps judders a little on 60 and 90 Hz screens, while 30 divides 60, 90 and 120 Hz evenly. 60 fps live footage means 2.5× the frames for the decoder to push through (see [REDACTED_TASK_KEY]). |
| Reference frames | `ref=3`, `max_dec_frame_buffering` ≤ 4 in the header | The decoder holds that many surface buffers plus a margin. With 16 on an S22 Ultra (Android 16) the SurfaceView queue jammed and the video froze right after start ([REDACTED_TASK_KEY]). |
| B-frames | `bframes=2:b-pyramid=none` | Lower output latency and fewer buffers in flight. |
| Keyframes | `keyint=<fps>:min-keyint=<fps/2>` | After any decoder restart (back from stories, a surface change, a seek) it decodes from the last keyframe. Without frequent ones the picture stands still while it catches up, then a burst of frames is dropped. |
| Scene cuts | leave `scenecut` alone | x264 puts I-frames on cuts by itself. `-sc_threshold 0` removes them. |
| Audio and timecode | `-map 0:v:0 -an -write_tmcd 0` | DaVinci masters carry a PCM audio track and a timecode track; the app needs neither. |
| Container | `-movflags +faststart` | `moov` before `mdat`. |
| Colour | bt709 tags | Same as the masters. |
| Size | even sides | An odd master height (1080×2341) is cut with `crop`, not scaled. |
| Quality | live footage: `-tune film -crf 22`, CG: `-tune animation -crf 24` | `-tune animation` smears hair and texture in live footage (checked at 3× zoom). |

## Recipes

Live footage, Welcome background, 24 fps:

```bash
ffmpeg -i "Onboarfing FINAL (1).mov" -map 0:v:0 -vf format=yuv420p \
  -c:v libx264 -profile:v high -level:v 4.1 -preset veryslow -tune film -crf 22 \
  -x264-params "ref=3:bframes=2:b-pyramid=none:keyint=24:min-keyint=12" \
  -color_primaries bt709 -color_trc bt709 -colorspace bt709 \
  -an -movflags +faststart -write_tmcd 0 introduction_background.mp4
```

CG story slides, 60 fps, 1080×2341 master:

```bash
ffmpeg -i "stories MOV/1.mov" -map 0:v:0 -vf "crop=1080:2340:0:0,format=yuv420p" \
  -c:v libx264 -profile:v high -preset veryslow -tune animation -crf 24 \
  -x264-params "ref=3:bframes=2:b-pyramid=none:keyint=60:min-keyint=30" \
  -color_primaries bt709 -color_trc bt709 -colorspace bt709 \
  -an -movflags +faststart -write_tmcd 0 stories_onboarding_slide_1.mp4
```

Poster: the first frame of the finished clip (not of the master, so the crop matches), webp, the same size, placed in
`drawable-nodpi`:

```bash
ffmpeg -i introduction_background.mp4 -frames:v 1 -c:v libwebp -quality 80 -compression_level 6 poster.webp
```

## Checks before committing

```bash
F=introduction_background.mp4

# 1. Codec, profile, level, size, fps, frame count, duration
ffprobe -v error -select_streams v:0 \
  -show_entries stream=codec_name,profile,level,width,height,r_frame_rate,nb_frames,pix_fmt,has_b_frames \
  -show_entries format=duration -of compact "$F"

# 2. Reference frames and decoder buffer from the SPS: expect ≤ 4 / ≤ 4 / ≤ 2
ffmpeg -loglevel trace -i "$F" -c:v copy -bsf:v trace_headers -frames:v 1 -f null - 2>&1 \
  | grep -E "max_num_ref_frames|max_dec_frame_buffering|max_num_reorder_frames"

# 3. Keyframes: the largest gap is at most one second
ffprobe -v error -select_streams v:0 -show_entries frame=key_frame,best_effort_timestamp_time -of csv=p=0 "$F" \
  | awk -F, '$1==1{if(n++){g=$2-p; if(g>m)m=g} p=$2} END{printf "keyframes=%d max_gap=%.3fs\n", n, m}'

# 4. A video track only, and moov comes before mdat
ffprobe -v error -show_entries stream=codec_type -of csv=p=0 "$F"
ffprobe -v trace "$F" 2>&1 | grep -oE "type:'(moov|mdat)'" | head -2

# 5. Decodes without errors: the output must be empty
ffmpeg -v error -i "$F" -f null -

# 6. Quality against the master: minimum, 5th percentile and median of per-frame SSIM.
#    PRE is the recipe's -vf: the ssim filter needs equal frame sizes, so the master gets the same crop as the clip.
PRE="format=yuv420p"  # story slides: PRE="crop=1080:2340:0:0,format=yuv420p"
ffmpeg -v error -i "$F" -i "<master>.mov" \
  -lavfi "[0:v]format=yuv420p[a];[1:v]${PRE}[b];[a][b]ssim=stats_file=ssim.txt" -f null -
python3 -c "
import re; v=sorted(float(re.search(r'All:([0-9.]+)',l).group(1)) for l in open('ssim.txt'))
print(f'min={v[0]:.4f} p5={v[len(v)//20]:.4f} median={v[len(v)//2]:.4f}')"
```

For live footage the target is SSIM no worse than the previous version of the file: a minimum around 0.985 and a median
around 0.99.

## Replacing a clip

- Re-encode from the master, not from the previous mp4. Masters are not in the repository; design sends them.
- Regenerate the poster from the new clip.
- Welcome background: if the resolution changes, update `IntroductionModel.BACKGROUND_VIDEO_SIZE`. The surface gets
  that size before the first frame; a mismatch logs an error, and a surface resized under the playing decoder is what
  froze the video on Samsung ([REDACTED_TASK_KEY]).