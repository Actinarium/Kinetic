# Kinetic

Kinetic is a small proof of concept app that records device motion and translates it into Java code (interpolators for Android SDK).

It’s a very experimental app in an early stage developed for the [Android Experiments I/O challenge][challenge].
So far it records rotation somewhat precisely, and offset recording is a subject to very strong interference (jitter, drift etc).

**How to use:**

1. Press Record button
2. Move the device for up to 10 seconds or stop earlier
3. Preview and adjust the results. If you’re lucky enough, it may produce something usable.
4. Switch off the recordings that you’re not interested in
5. Press Export to generate and save Java code

## License

The library is licensed under Apache 2.0 License, meaning that you can freely use it in any of your projects.

The full license text is [here][license].

[challenge]: https://www.androidexperiments.com/challenge
[license]: https://raw.githubusercontent.com/Actinarium/Kinetic/master/LICENSE