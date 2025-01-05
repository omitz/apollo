from api import create_app

app = create_app()

if __name__ == '__main__':
    print("Main: Starting rest service for landmark search", flush=True)
    app.run(debug=True, host='0.0.0.0', port='83', use_reloader=False)