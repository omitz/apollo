#!/usr/bin/env python3
#
# https://github.com/nmslib/nmslib/tree/master/python_bindings
#
# This example demonstrates Hierarchical Navigable Small World (HNSW)
#
# Maximum resident set size (kbytes): 186196
#

import numpy as np
import time
import nmslib                  # make NMSLIB available

print ("========================================================")
print ("  NMSLIB HNSW (approximated nearest-neighbor graph)")
print ("========================================================")


##########################################
#   SIMULATED DATA SET : 
##########################################
from simulate_dataset import *
# d = number of data dimensions
# xb = row-major data vectors
# nb = number of data
# xq = row-major query vectors
# nq = number of queries


##########################################
#   NMSLIB  EXAMPLE (HNSW)
##########################################
print("Inexing data...")
start = time.time()

# Initialize a new index, using a HNSW index on Cosine Similarity
index = nmslib.init (method="hnsw", space='l2') # space can also be 'cosinesimil'
index.addDataPointBatch (xb)
index.createIndex({'post': 0})  # post=2 takes twice long indexing time but higher recall
                                # post=0 has short indexing time but lower recall

index_time = time.time() - start
print("Indexing time = ", index_time)

# Test Saving and Loading:
# index.saveIndex ("savedIndex.nmslib", save_data=True)
# index.loadIndex("savedIndex.nmslib", load_data=True)

##########################################
#  Search K-NN
##########################################
print("Searching data...")
start = time.time()
                                
#  Batch search = query a bunch of vecoters at once
k = 4                           # we want to see 4 nearest neighbors
num_threads = 1                 # using a thread pool to do batch query
neighbors = index.knnQueryBatch (xq, k, num_threads)
query_time = time.time() - start
I = np.asarray ([neighbors[idx][0] for idx in range(len(neighbors))]) # neighbor indecies
D = np.asarray ([neighbors[idx][1] for idx in range(len(neighbors))]) # neighbor distances

print("Search time = ", query_time)

##########################################
#  Print Result
##########################################
from print_search_result import *
printResult (I,D)


