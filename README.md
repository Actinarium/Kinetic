# Kinetic

Kinetic is a small proof of concept app that records device motion and translates it into Java code (interpolators for Android SDK).

[Video demo](https://www.youtube.com/watch?v=OT7uTqNy30M) • Pending publication on [Google Play Store](https://play.google.com/store/apps/details?id=com.actinarium.kinetic)

It’s a very experimental app in an early stage developed for the [Android Experiments I/O challenge][challenge].
So far it records rotation somewhat precisely, and offset recording is a subject to very strong interference (jitter, drift etc).

## How to use

1. Press Record button
2. Move the device for up to 10 seconds or stop earlier
3. Preview and adjust the results. If you’re lucky enough, it may produce something usable.
4. Switch off the recordings that you’re not interested in
5. Press Export to generate and save Java code

## Tips

* Before pressing Record let the device rest idle for a bit: it attempts performing calibration in the background.
* If you want to get a usable offset recording, do not even try capturing multiple directions at once.
The safest bet would be to place the device on flat table and move horizontally only.

## Goal

This project was created to try out whether it was possible to utilize device sensors such as accelerometer and gyroscope
to record natural motion that would be accurate enough to use in real world apps. The idea was there for a while since I
needed this for some of my own apps, and the I/O challenge motivated me to finally try and implement this.

**Conclusions:** unfortunately, sensors in general market devices (as opposed to Tango Project etc) are not suitable
for capturing motion with sufficient precision. One of my requirements was double-integrating acceleration data to
obtain linear offset — but errors from jitter and gravity bias just added up. Multiple attempts were made to reduce
interference, but all hit dead end. Gravity impact could not be effectively eliminated (the framework’s high pass filter
was unusable because it also smoothened out actual acceleration).

The gyroscope, however, yielded adequate recordings good enough to record rotational animation.

## License

```
Copyright (C) 2016 Actinarium

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

The full license text is [here][license].

[challenge]: https://www.androidexperiments.com/challenge
[license]: https://raw.githubusercontent.com/Actinarium/Kinetic/master/LICENSE