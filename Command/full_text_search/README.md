# Full-text (documents) Search

The goal of full-text search is to retrive documents.

## Types of query supported
### Term search
  - Words are tokenized in to terms:  Stemming and Lemmatization.
  - Stop words are removed.  (the, i, at, as, etc...)
### Logical search
  - AND
  - OR
  - NOT
  - Groupings
  - example: "cat & (rat | mouse)"
### Proximity (phrase) search
  - example:  "before <-> after"
  - example:  "before <3> after"

## Apollo Integration

### To run unit test.
Assumptions:
    - Docker-Compose is installed.
    - The Apollo Command directory located at ../../..
    - There is a .env file in the Apollo Command directory.

```bash
tests/integration_test.sh
```
### Live debugging
```
# cd to Command dir.
docker-compose run -v $(pwd)/full_text_search/:/code --rm full-text-search bash
```


### To send a text file to the queue.
```bash
curl localhost:8080/jobs/full_text_search/ -H 'Content-Type:application/json' -X POST -d '{"path": "inputs/ner/test.txt"}'
```

### To rearch full-text.
```bash
curl localhost:8080/search/full_text?query=crisis
```

