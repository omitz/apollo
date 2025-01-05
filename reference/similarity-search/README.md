# Three Top-Performing Similarity Search Libries -- (FAISS, NMSLib, NTG)

FAISS - Facebook AI Similarity Search
NMSLib - Non-Metric Space Library
NGT - Neighborhood Graph and Tree for Indexing High-dimensional Data 

## Set Up Virtual Environment

```bash
$ python3 -m venv --prompt simsearch venv
$ . venv/bin/activate
(simsearch) $ pip install --upgrade pip wheel setuptools
(simsearch) $ pip install faiss-gpu  ## also works without gpu
(simsearch) $ pip install --no-binary :all: nmslib ## takes a few minutes
(simsearch) $ pip install ngt
```


## Run simple examples
example1_faiss.py    --- Brute-force approach
example2_faiss.py    --- Inverted File Index
example3_faiss.py    --- Inverted File Index + Product Quantization
example_nmslib.py    --- Hierarchical Navigable Small World Graph
example_ngt.py       --- Neighborhood Graph Tree

