# The Apollo Python package

The Apollo python package is meant to replace our utils packaging and provide some formalisms to help with code design and consistency across analytics. This python package will also assist outside developers who want to incorporate capabilities into the Apollo ingest pipeline.

## Developer installation

Use the -e option so that changes are reflected in the environment.

```bash
$ python -m pip install -e setup.py
```

## Package Usage
Usage is meant to be namespaced as in the following. Move away from using 'utils'.

```python
import apollo

apollo.Analytic
apollo.Message
apollo.MessageQueue
apollo.SQL or apollo.RDMS
apollo.Graph
apollo.Feature
```

## Example Apollo Analytic

```python
import apollo

class TextLocalization(apollo.Analytic):
    def __init__(self):
        # load models, create db connections, etc

    def run(self):
        # must implement run method from base class
        # this keeps the analytics consistent and expected
```
