from apollo import create_app
from face.facenet_analytic import facenetanalytic

face_analytic = facenetanalytic.FacenetAnalytic('facenet')
app = create_app(analytic=face_analytic)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port='82')