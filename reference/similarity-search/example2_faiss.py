#!/usr/bin/env python3
#
# https://github.com/facebookresearch/faiss/wiki/Getting-started
#
# This example demonstrates Inverted File Index 
#
# Maximum resident set size (kbytes): 119548
#
import faiss                   # make FAISS available
import time


print ("========================================================")
print ("  FAISS IVF + FLAT (clustering method)")
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
#   IVF (clustering) + FLAT (quantization) Indexing: 
##########################################
print("Inexing data...")
start = time.time()

nlist = 50                       # number of clusters
nprobe = 1                       # number of nearby cluster to search
quantizer = faiss.IndexFlatL2(d) # coarse quantizer
index = faiss.IndexIVFFlat (quantizer, d, nlist, faiss.METRIC_L2)
index.nprobe = nprobe

index.train(xb)                 # training phase, to analyze the
                                # distribution of the vectors.
index.add(xb)                   # index the data

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
