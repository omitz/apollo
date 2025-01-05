#!/usr/bin/env python3
#
# https://github.com/facebookresearch/faiss/wiki/Getting-started
#
# This example demonstrates brue-force method: FLAT
#
# Maximum resident set size (kbytes): 119828
#
import faiss                   # make FAISS available
import time

print ("========================================================")
print ("  FAISS FLAT L2 (Brute-force Approach)")
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
#   IndexFlatL2 (brute force) Indexing: 
##########################################
print("Inexing data...")
start = time.time()

index = faiss.IndexFlatL2(d)   # build the empty index
index.add(xb)                  # add vectors to the index

index_time = time.time() - start
print("Indexing time = ", index_time)

# Test Saving and Loading:
# faiss.write_index (index, "savedIndex.faiss")  # save the index to disk
# index = faiss.read_index ("savedIndex.faiss")  # load the index

##########################################
#  Search K-NN
##########################################
print("Searching data...")
start = time.time()

# Batch search = query a bunch of vecoters at once
k = 4                          # we want to see 4 nearest neighbors
D, I = index.search(xq, k)     # actual search
query_time = time.time() - start

print("Search time = ", query_time)

##########################################
#  Print Result
##########################################
from print_search_result import *
printResult (I,D)
