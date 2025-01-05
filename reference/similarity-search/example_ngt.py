#!/usr/bin/env python3
#
# https://github.com/yahoojapan/NGT/blob/master/python/README.md
# https://github.com/yahoojapan/NGT/blob/master/python/sample/sample.py
#
# This example demonstrates Neighborhood Graph and Tree (NGT)
#
# Maximum resident set size (kbytes): 172328

import ngtpy                   # make NGT available
import time

print ("========================================================")
print ("  NGT (Neighborhood Graph and Tree)")
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
#   NTG EXAMPLE 
##########################################
print("Inexing data...")
start = time.time()

indexPath = 'savedIndex.ntg'    # index directory to save to / load from
ngtpy.create (path=indexPath, dimension=d, distance_type="L2")
index = ngtpy.Index ("savedIndex.ntg")
num_threads = 1                 # using a thread pools to load data
index.batch_insert (xb, num_threads = num_threads)

index_time = time.time() - start
print("Indexing time = ", index_time)

# Test Saving and Loading:
# index.save()
# index.close()                   # close the index.
# index = ngtpy.Index(indexPath)   # open the index.


##########################################
#  Search K-NN
##########################################
print("Searching data...")
start = time.time()

## k nearest neighbors.  
k = 4                           # we want to see 4 nearest neighbors
results = [None] * len(xq)      # NGT Library does not provide batch query so we have to use loop..
for idx in range(len(xq)):
    result = index.search (xq[idx], k)
    results[idx] = result
query_time = time.time() - start
I = np.array (results)[:,:,0]
D = np.array (results)[:,:,1]

print("Search time = ", query_time)

##########################################
#  Print Result
##########################################
from print_search_result import *
printResult (I,D)
