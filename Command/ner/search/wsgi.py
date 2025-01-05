from api import create_app
import nltk

app = create_app()

if __name__ == '__main__':
    nltk.download('punkt')
    print("Main: Starting rest service for NER search (return snippets)", flush=True)
    app.run(debug=True, host='0.0.0.0', port='84')