from api import create_app
from dotenv import load_dotenv

load_dotenv('.env')

app = create_app()

if __name__ == '__main__':
    #app.run(debug=True, host='0.0.0.0', port='80')

    # prevent watchdog from restarting the server when detected
    # changes in the following files.
    exclude_patterns=['/code/\.#*',
                      '/code/api/\.#*',
                      '/code/tests/*',
                      '/code/api/SpeakerIdTraining/\.#*',
                      '/code/api/FaceIdTraining/\.#*',
                      '/code/api/SpeakerIdTraining/tmp_*/*',
                      '/code/api/FaceIdTraining/tmp_*/*',
                      '/code/api/*/classifier_models/*/*']
    print (f"exclude_patterns = {exclude_patterns}", flush=True)
    
    app.run(debug=True, host='0.0.0.0', port='80', exclude_patterns=exclude_patterns)
