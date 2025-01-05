##########################################
#   SIMULATED DATA SET : 
##########################################
import numpy as np

# Generate 100k random 64-dimensional vectors.  Each dimsion is
# uniformly i.i.d over [0..1) interval.  Each row is a vector.
d = 64                           # dimension
nb = 100000                      # database size = 100k
np.random.seed(1234)             # make reproducible
xb = np.random.random((nb, d)).astype('float32')

# Generate random query vectors. 
nq = 10000                       # number of queries = 10k
xq = np.random.random((nq, d)).astype('float32')

# Add offset to the first dimension depending on the array index.
# This way, nearby vectors have similar array index numbers.
xb[:, 0] += np.arange(nb) / 1000. 
xq[:, 0] += np.arange(nq) / 1000.


# Print summary:
print ("========================================================")
print ("Data Summary:")
print ("========================================================")
print ("Data Dimension, d = ", d)
print ("Database Size, nb = ", nb)
print ("Query Size, nb = ", nq)
