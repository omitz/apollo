#! /usr/bin/env python3
#
#
# TC 2021-05-11 (Tue) 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import pickle
from sklearn.svm import SVC


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------

def SaveAsLibSVM (clf, filename, newFormat=1):
    """libsvm format is realtive simple.  

    Use newFormat=0 if sparse matrix index starts at 0.  This is the
    case for some libsvm running on the phone.

    Example, clf created with:
    SVC(kernel='linear', class_weight='balanced', gamma='auto',
            decision_function_shape='ovo', probability=True)

    Example Fromat:
    svm_type        --- "c_svc"   type(clf) == "sklearn.svm._classes.SVC"
    kernel_type     --- "linear"  clf.kernel
    nr_class        --- "10"      len (clf.classes_)
    total_sv        --- "92"      clf.n_support_.sum()
    rho             --- <45 nubmers>  -clf._intercept_
    label           --- <10 numbers>  clf.classes_
    probA           --- <45 numbers> clf.probA_
    probB           --- <45 numbers> clf.probB_
    nr_sv           --- <10 numbers> clf.n_support_
    SV
    <9 numbers> <support vector in sparse dictionary>
    ...
    <9 numbers> <support vector in sparse dictionary>

    Where:
       <9 numbers> = clf.dual_coef_[:,i].toarray()   -- ith SV
       <support vector in dictionary as sparse matrix> = sparse (clf.support_vectors_[i,:])

    from scipy.sparse import csr_matrix
    dict((sum(key)+1, value) for (key, value) in B.items())

    ref: https://www.csie.ntu.edu.tw/~cjlin/libsvm/faq.html#f402

    """

    content = []
    ## output the header
    # print ("type (clf) == ", type (clf))
    assert (type (clf) == SVC)
    content.append ("svm_type c_svc")
    content.append ("kernel_type %s" % clf.kernel)
    content.append ("nr_class %d" % len (clf.classes_))
    content.append ("total_sv %d" % clf.n_support_.sum())
    content.append ("rho " + " ".join (['%.17g' % -elm for elm in clf._intercept_.tolist()]))
    content.append ("label " + " ".join (['%d' % elm for elm in clf.classes_.tolist()]))
    content.append ("probA " + " ".join (['%.17g' % elm for elm in clf.probA_.tolist()]))
    content.append ("probB " + " ".join (['%.17g' % elm for elm in clf.probB_.tolist()]))
    content.append ("nr_sv " + " ".join (['%d' % elm for elm in clf.n_support_.tolist()]))
    content.append ("SV")

    for idx in range (clf.n_support_.sum()):

        ## For sparse matrix, as read from:
        ##      trainy, trainX = svm_read_problem('scaffold/faceID.data', return_scipy=True) 
        # dual_coef = clf.dual_coef_[:,idx].toarray().ravel()
        # dual_coef_str = " ".join (['%.17g' % elm for elm in dual_coef.tolist()])
        # sv_coef_sparse = clf.support_vectors_[idx,:].todok()
        # sv_coef_str = " ".join (["%d:%.8g" % (sum(key)+newFormat, value)
        #                          for (key, value) in sv_coef_sparse.items()])

        # dual_coef = clf.dual_coef_[:,idx].toarray().ravel()
        dual_coef = clf.dual_coef_[:,idx]
        dual_coef_str = " ".join (['%.17g' % elm for elm in dual_coef.tolist()])

        sv_coef = clf.support_vectors_[idx,:]
        sv_coef_str = " ".join (["%d:%.8g" % (key+newFormat, value)
                                 for (key, value) in enumerate(sv_coef)])

        content.append (dual_coef_str + " " + sv_coef_str + " ")
    
    open (filename, 'w').writelines(["%s\n" % item for item in content])


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("classiferFile", help="The input sickit SVM classifier")
    parser.add_argument ("libsvmFile", help="The output libsvm classifier")
    parser.add_argument ("-s", "--sparseIdx",
                         help="sparse matrix index value, either 0 (phone) or 1 (desktop)",
                         default="0")

    # Specify Example:
    parser.epilog='''Example:
        %s -s 0 svm.pkl model
        %s -s 1 svm.pkl model.desktop
        ''' % (sys.argv[0], sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    args = parseCommandLine ()

    # Access the options:
    print ()
    print ("classiferFile = ", args.classiferFile)
    print ("libsvmFile = ", args.libsvmFile)
    print ("sparseIdx = ", args.sparseIdx)
    input ("press enter to continue")


    clf = pickle.load (open (args.classiferFile, 'rb'))
    SaveAsLibSVM (clf, args.libsvmFile, int (args.sparseIdx))
    print ("Wrote to ", args.libsvmFile) 
    
    #---------------------------
    # run the program :
    #---------------------------


    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

