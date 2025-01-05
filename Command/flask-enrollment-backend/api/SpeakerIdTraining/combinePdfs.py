#! /usr/bin/env python3

import sys
from PyPDF2 import PdfFileMerger
print (len(sys.argv))

if (len(sys.argv) != 4):
    print ("Please specifiy two pdfs to merge and the output pdf file")
    print ("   Example:")
    print ("   %s file1.pdf file2.pdf out.pdf" % sys.argv[0])
    exit (1)
file1 = sys.argv[1]
file2 = sys.argv[2]
out = sys.argv[3]


pdfs = [file1, file2]
merger = PdfFileMerger()
for pdf in pdfs:
    merger.append(pdf)
merger.write(out)
merger.close()
