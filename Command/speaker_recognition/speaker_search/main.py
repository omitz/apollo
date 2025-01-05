from apollo import create_app
from speaker_recognition.vgg_speaker_recognition import analytic

analytic = analytic.SpeakerRecogAnalytic('speaker_recognition')
app = create_app(analytic=analytic)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port='85')