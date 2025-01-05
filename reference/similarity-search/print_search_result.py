def printResult (I, D):
    print ("========================================================")
    print ("k nearest neighbors of first 5 queries")
    print ("========================================================")
    print(I[:5])

    ## distances of the k nearest neighbors for the first 5 queries
    print ("========================================================")
    print ("distance to the k nearest neighbors of first 5 queries")
    print ("========================================================")
    print(D[:5])

    ## k nearest neighbors (identified by their array index) of the last 5
    ## queries. Note that the neighbors all have large array indices.
    print ("========================================================")
    print ("k nearest neighbors of last 5 queries")
    print ("========================================================")
    print(I[-5:])
