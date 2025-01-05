import unittest

from full_text_search_analytic import FullTextSearchAnalytic, SERVICE_NAME


class TestFullTextSearchAnalytic(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.analytic = FullTextSearchAnalytic(SERVICE_NAME, testing_in_jenkins=True)

    def test_check_mime_and_read_doc(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/doc.doc')
        self.assertEqual(EXPECTED_DOC_FULL_TEXT, full_text_result)

    def test_check_mime_and_read_docx(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/docx.docx')
        self.assertEqual(EXPECTED_DOCX_FULL_TEXT, full_text_result)

    def test_check_mime_and_read_pdf(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/pdf.pdf')
        self.assertEqual(EXPECTED_PDF_FULL_TEXT, full_text_result)

    def test_check_mime_and_read_arabic_doc(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/bbc_news_arabic_2020-01-15.doc')
        expected_arabic_word = 'لتنظيم'
        self.assertIn(expected_arabic_word, full_text_result)

    def test_check_mime_and_read_french_docx(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/bbc_news_french_2020-01-15.docx')
        expected_french_word = 'privés'
        self.assertIn(expected_french_word, full_text_result)

    def test_check_mime_and_read_russian_pdf(self):
        full_text_result = self.analytic.check_mime_and_read('tests/test_files/bbc_news_russian_2020-01-15.pdf')
        expected_russian_word = 'Кто'
        self.assertIn(expected_russian_word, full_text_result)


EXPECTED_DOC_FULL_TEXT='\n[pic]\n\nthis is a .doc with a picture\n\nNote re textract shell error:\n\nLet me explain.\nInside a Word file the text is stored in a so called text stream. There are\n\ntwo possible text streams: a small block text stream and a large block text\n\nstream. The small blocks are 64 bytes in size, the large blocks are 512\nbytes in size. Because the difference in size Antiword would need two\ndifferent methods for reading those two text streams. The method for\nreading that small block text stream has not been implemented yet. The\nresult is that Word files with no large block text stream can no be read by\n\nAntiword. Such Word file are mostly smaller than about 12 kilobytes and\nhave less than 1024 bytes of text.\n\nThe reason for not implementing the missing fearture is simple. Word\ndocuments that use the small block text stream can not be produced by Word\nfor Windows (all versions), but only Word for Mac. And now by OpenOffice.\nNote that these documents can be read by all versions of Word.\n\n\n'

EXPECTED_DOCX_FULL_TEXT='this is a docx with a picture'

EXPECTED_PDF_FULL_TEXT='this is a pdf with a picture\n\n\x0c'