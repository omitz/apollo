# Local feature extractor and matching

A prototype for local image feature extraction and matching features stored in the data lake.

## Set Up Virtual Environment

```bash
$ cd apollo/reference/local-features
$ python3.6 -m venv --prompt features venv
$ . venv/bin/activate
(features) $ python -m pip install --upgrade pip wheel setuptools
(features) $ python -m pip install -r requirements.txt
```

## Build database of features

Features are stored in a local file called features.db. Run extractor.py on each image you want in the database.

### Extract features

Extractor takes in an image argument and extracts ORB features. These features and descriptors are saved to the SQL database.

```bash
(features) $ python extractor.py -i <image_path>
```

### Extractor arguments

```bash
(features) $ python extractor.py -h
usage: extractor.py [-h] -i INPUT [-o OUTPUT] [-v]

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        Image path
  -v, --verbose         Increase logging verbosity
```

### Viewing data in SQLITE3

```bash
$ sqlite3 features.db
SQLite version 3.11.0 2016-02-15 17:29:24
Enter ".help" for usage hints.
sqlite> .tables
image     
sqlite> .schema image
CREATE TABLE image (
        id INTEGER NOT NULL, 
        filepath VARCHAR, 
        keypoints BLOB, 
        descriptors BLOB, 
        PRIMARY KEY (id), 
        UNIQUE (filepath)
);
sqlite> select * from image;
1|plate.png|���X|���0
2|box.png|��q�|���l
3|box_in_scene.png|����|���}
sqlite> .quit
```

## Matching

Now that we have a database of features, we would like to query local features with another image. The matcher will extract the same ORB features and then query the database for descriptors. The descriptors from the query image will be matched with each set of descriptors in the database and then ranked by distance to each feature.

### Match with a query image

```bash
(features) $ python matcher.py -i <query_image_path>
```

To query with a subimage, use the bbox option. x1 < x2 and y1 < y2.

```bash
(features) $ python matcher.py -i <query_image_path> -b x1 y1 x2 y2
```

### Sorting/ranking

Query rank is determined by Euclidean distance between the query image features and the database image features. A transform is solved using the good feature correspondences. The query features are warped into the database image space. Distance is calculated between the warped query points and the corresponding image points using the 10 best features.

### Matcher arguments

```bash
(features) $ python matcher.py -h
usage: matcher.py [-h] -i INPUT [-b BBOX [BBOX ...]] [-v]

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        Image path
  -b BBOX [BBOX ...], --bbox BBOX [BBOX ...]
  -v, --verbose         Increase logging verbosity
```

## References

https://www.sqlitetutorial.net/

https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_feature2d/py_orb/py_orb.html

https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_feature2d/py_matcher/py_matcher.html
