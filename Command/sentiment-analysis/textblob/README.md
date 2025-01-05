# TextBlob

## Apollo Integration:
Assumptions:
    - Docker-Compose is installed.
    - The Apollo Command directory located at ../../..
    - There is a .env file in the Apollo Command directory.
### Runing Unittest
```bash
./tests/integration_test.sh
```

### Run as stand-alone
```
cd APOLLO/Command/
source .env
aws ecr get-login-password --region us-east-1 | docker login --password-stdin --username AWS 604877064041.dkr.ecr.us-east-1.amazonaws.com

cd APOLLO/Command/sentiment-analysis/textblob/
docker-compose up -d sentiment-textblob
 
docker-compose exec sentiment-textblob bash
./textblob_main.py in.txt /dev/stdout

```



# OLD reference
## Installation
```
pip install -r requirements.txt
```

## Build Dockerfile

```bash
docker build -t apollo/textblob .
```

## Example run

```bash
docker run --rm -it apollo/textblob textblob_main.py in.txt out.txt
```
