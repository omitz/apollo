dictate.js
==========

__dictate.js__ is a small Javascript library for browser-based
real-time speech recognition.

It uses [Recorderjs](https://github.com/mattdiamond/Recorderjs) for
audio capture, and a WebSocket connection to the [Kaldi GStreamer
server](https://github.com/alumae/kaldi-gstreamer-server) for speech
recognition.

API
---

The API is modeled after [Android's
SpeechRecognizer](http://developer.android.com/reference/android/speech/SpeechRecognizer.html).
See the source code of [lib/dictate.js](lib/dictate.js) and the usage
in [demos/demo.js](demos/demo.js).

Running the demos
-----------------

The demos connect to the public services running on `wss://bark.phon.ioc.ee`
that offer Estonian and English speech recognition.

The demos are available [here](https://kaljurand.github.io/dictate.js/).
(Note that in order to use a wss-service the HTML-pages must be loaded over https.)

To run the demos on localhost, start a local HTTP server, e.g.:

    # Python 2
    python -m SimpleHTTPServer

    # Python 3
    python -m http.server

and then open e.g. <http://localhost:8000/demos/mob.html>.

```bash
firefox http://localhost:8000/demos/mob_local.html
```


Browser support
---------------

Known to work in:

- Google Chrome 72.0 on Ubuntu 18.04
- Mozilla Firefox 68.0.2 on Ubuntu 18.04
- Chrome 76 on Android 10
- Opera 53.0 on Android 10

See also
--------

- [Dikteeri](https://bark.phon.ioc.ee/dikteeri/) is an Estonian dictation demo by the [Laboratory of Phonetics and Speech Technology](https://phon.ioc.ee/dokuwiki/doku.php?id=start.en) (in Estonian). Uses `dictate.js`.
- [Kõnele](https://github.com/Kaljurand/K6nele) contains an Android front-end to the Kaldi GStreamer server
