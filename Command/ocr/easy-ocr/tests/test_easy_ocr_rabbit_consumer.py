import os
from unittest import TestCase
from unittest.mock import MagicMock
from apollo.models import SearchFullText
from apollo import PostgresDatabase
from sqlalchemy import func
from easy_ocr_rabbit_consumer import EasyOcrRabbitConsumer
from easy_ocr_analytic import SERVICE_NAME


class TestEasyOcrRabbitConsumer(TestCase):

    @classmethod
    def setUpClass(cls):
        os.environ['RABBITMQ_HOST'] = "rabbitmq"
        cls.analytic = MagicMock()
        cls.analytic.name = 'test_ocr'
        cls.rabbit_consumer = EasyOcrRabbitConsumer(SERVICE_NAME, 'ApolloExchange', cls.analytic)
        cls.database = PostgresDatabase('apollo')
        cls.database.delete_all_from_table(SearchFullText)
        cls.exchange_name = 'ApolloExchange'
        cls.connection = cls.rabbit_consumer.connection
        cls.channel = cls.connection.channel()

    @classmethod
    def tearDownClass(cls):
        # Clean up postgres
        cls.database.delete_all_from_table(SearchFullText)
        # Clean up rabbitmq
        cls.channel.queue_delete(queue=cls.analytic.name + cls.queue_suffix)
        cls.channel.queue_delete(queue='ocr_easy_queue')
        cls.connection.close()

    @classmethod
    def setUp(cls):
        # Empty any NER queue by deleting it
        cls.ner_analytic_name = 'named_entity_recognition'
        cls.queue_suffix = '_queue'
        cls.ner_queue_name = cls.ner_analytic_name + cls.queue_suffix
        cls.channel.queue_delete(queue=cls.ner_queue_name)
        # Recreate NER queue (since OCR will publish messages to it)
        cls.ner_queue = cls.rabbit_consumer.declare_queue_and_route(cls.ner_analytic_name, cls.exchange_name)

    @classmethod
    def tearDown(cls):
        cls.channel.queue_delete(queue=cls.ner_queue_name)

    def test_save_results_to_database(self):
        og_source = 's3://apollo-source-data/inputs/uploads/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg'
        msg_dict = {'name': 's3://apollo-source-data/inputs/uploads/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg', 'description': 'ocr_easy', 'mime': 'image/jpeg', 'original_source': og_source}
        full_text = '1000811N{8 [ (RX) 90^{M& SCUA0RON 915 9 4N7 8 J6 7 +0FC ^ 0/0P^18 12 ! 5 دوا دداط , DR9 5ا  191 Y0M| THE RDYAL WESSEX VEOMANIY THE S0UTH WESTS YEDMNIfIfffIIT |0I 7 R[0RغIآIN& شتن IWN|-bsqISa0QIIOIIk Amall: 438300 01722 \n'
        clean_full_text = full_text.replace("'", "")
        search_vector = func.to_tsvector('english', clean_full_text)
        analytic_results = {'path': 's3://apollo-source-data/inputs/uploads/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg',
                            'fulltext_path': 's3://apollo-source-data/outputs/ocr/Army_Reserves_Recruitment_Banner_MOD_45156284_jpg_ocr-easy.txt',
                            'full_text': full_text,
                            'search_vector': search_vector,
                            'service_name': 'ocr_easy',
                            'metadata_path': 's3://apollo-source-data/outputs/ocr/Army_Reserves_Recruitment_Banner_MOD_45156284_jpg_ocr-easy_metadata.json',
                            'meta_data': '{"width": 2400, "height": 2212, "pred": [[[[1327.6815351223509, 199.0255177585747], [1421.897366596101, 247.36754446796633], [1413.3184648776491, 264.9744822414253], [1319.102633403899, 215.63245553203367]], "1000811N{8", 0.0016493976581841707], [[[1526, 252], [1550, 252], [1550, 284], [1526, 284]], "[", 0.26729607582092285], [[[1553.4976792543355, 256.03166610925], [1647.923739951368, 309.22265529265354], [1628.5023207456645, 342.96833389075], [1534.076260048632, 289.77734470734646]], "(RX)", 0.07246094942092896], [[[1329.5469730106795, 261.0519839459216], [1388.9538000347925, 290.57260624238916], [1380.4530269893205, 307.9480160540784], [1321.0461999652075, 277.42739375761084]], "90^{M&", 0.03396355360746384], [[[1507.6534815345406, 270.01503764721724], [1668.7991023429665, 358.7482726384446], [1649.3465184654594, 392.98496235278276], [1488.2008976570335, 304.2517273615554]], "SCUA0RON", 0.1279664784669876], [[[1388.3053712883527, 291.1245024785526], [1420.9969207064107, 305.88905996075493], [1412.6946287116473, 321.8754975214474], [1381.0030792935893, 308.11094003924507]], "915", 0.11662253737449646], [[[1353.5469730106795, 334.0519839459216], [1420.9632307793372, 364.61826068179556], [1413.4530269893205, 380.9480160540784], [1345.0367692206628, 351.38173931820444]], "9 4N7 8", 0.004583427216857672], [[[1527.6291842816042, 368.03467669250307], [1606.9230478952816, 407.45055774420524], [1598.3708157183958, 424.96532330749693], [1519.0769521047184, 384.54944225579476]], "J6 7 +0FC ^", 0.00015597055607941002], [[[1325.5469730106795, 382.0519839459216], [1392.9632307793372, 412.61826068179556], [1385.4530269893205, 428.9480160540784], [1317.0367692206628, 399.38173931820444]], "0/0P^18 12", 0.003056755056604743], [[[1392.2820418413824, 412.1333087875939], [1423.9926932980834, 425.8291977173071], [1416.7179581586176, 441.8666912124061], [1385.0073067019166, 429.1708022826929]], "! 5", 0.007217833306640387], [[[1731.4647122724277, 418.07296418074014], [1777.969567117636, 441.6524293321819], [1770.5352877275723, 457.92703581925986], [1723.030432882364, 433.3475706678181]], "\\u062f\\u0648\\u0627 \\u062f\\u062f\\u0627\\u0637 ,", 0.00012345869618002325], [[[1583.202547777171, 596.1658598874935], [1619.9669063771782, 605.6376751410461], [1615.797452222829, 622.8341401125065], [1578.0330936228218, 613.3623248589539]], "DR9", 0.15746808052062988], [[[1626.4199852094343, 611.0859511911331], [1678.9917864129354, 631.818928507915], [1672.5800147905657, 647.9140488088669], [1620.0082135870646, 627.181071492085]], "5\\u0627  191", 0.001717934268526733], [[[1502.3729298231972, 696.0494578861421], [1614.0, 732.0], [1601.6270701768028, 766.9505421138579], [1489.0, 732.0]], "Y0M|", 0.06710201501846313], [[[611, 1024], [1688, 1024], [1688, 1188], [611, 1188]], "THE RDYAL WESSEX VEOMANIY", 0.07671298086643219], [[[614, 1239], [2041, 1239], [2041, 1422], [614, 1422]], "THE S0UTH WESTS YEDMNIfIfffIIT", 0.0008654493140056729], [[[1730, 1356], [1933, 1356], [1933, 1504], [1730, 1504]], "|0I", 0.15490269660949707], [[[2213.418861169916, 1420.2565835097475], [2277.706021867625, 1412.6436028368676], [2281.581138830084, 1459.7434164902525], [2217.293978132375, 1467.3563971631324]], "7", 0.09937664121389389], [[[1075.5410560593086, 1426.1318213515694], [1750.6321340837396, 1373.1865004650215], [1748.4589439406914, 1564.8681786484306], [1073.3678659162604, 1617.8134995349785]], "R[0R\\u063aI\\u0622IN&", 0.007265565451234579], [[[2217.981981618011, 1456.7801797798122], [2272.4877992827733, 1447.3234396137832], [2276.018018381989, 1472.2198202201878], [2221.5122007172267, 1481.6765603862168]], "\\u0634\\u062a\\u0646", 0.0017173944506794214], [[[1630.200573707924, 1601.362180090111], [2089.9930387063014, 1490.402263881608], [2102.799426292076, 1584.637819909889], [1644.0069612936989, 1695.597736118392]], "IWN|-bsqISa0QIIOIIk", 0.00021520658629015088], [[[1453.448167612687, 1646.9859687479113], [1645.4770979954167, 1595.9900920422517], [1663.551832387313, 1689.0140312520887], [1472.5229020045833, 1739.0099079577483]], "Amall:", 0.011441628448665142], [[[1026.7798668849084, 1704.7735607201976], [1408.1024324838313, 1622.20630135897], [1429.2201331150916, 1772.2264392798024], [1047.8975675161687, 1855.79369864103]], "438300", 0.600999653339386], [[[571.7523916304408, 1787.2410239239534], [1026.868984671038, 1710.6879967519399], [1042.2476083695592, 1897.7589760760466], [587.1310153289618, 1974.3120032480601]], "01722", 0.5347863435745239]]}'}
        self.rabbit_consumer.save_results_to_database(msg_dict, analytic_results)
        # Check that the path is in the postgres table
        pg_query_res = self.rabbit_consumer.database.query(SearchFullText, SearchFullText.original_source == msg_dict[
            'original_source']).first()
        self.assertEqual(og_source, pg_query_res.original_source)
        # Check that a message was sent to the NER queue
        self.ner_queue = self.rabbit_consumer.declare_queue_and_route(self.ner_analytic_name, self.exchange_name)
        self.assertEqual(1, self.ner_queue.method.message_count)

    def test_save_results_to_database_no_text(self):
        og_source = 's3://some_image_with_no_text.jpg'
        msg_dict = {'name': og_source, 'description': 'ocr_easy', 'mime': 'image/jpeg', 'original_source': og_source}
        analytic_results = {} # Expected result if no text is detected
        self.rabbit_consumer.save_results_to_database(msg_dict, analytic_results)
        # Check that the path is NOT in the postgres table
        pg_query_res = self.rabbit_consumer.database.query(SearchFullText, SearchFullText.original_source == msg_dict[
            'original_source']).first()
        self.assertIsNone(pg_query_res)
        # Check that a message was NOT sent to the NER queue
        self.ner_queue = self.rabbit_consumer.declare_queue_and_route(self.ner_analytic_name, self.exchange_name)
        self.assertEqual(0, self.ner_queue.method.message_count)