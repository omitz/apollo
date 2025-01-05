import re
import json
from sqlalchemy import Column, Integer, String, BigInteger, Index, func
from sqlalchemy_utils import TSVectorType
from .ModelBase import ModelBase
from ..search.api import db


class SearchFullText (ModelBase):
    """Table schema for supporting full-text search.
    """
    
    __tablename__ = 'fulltext'

    id = Column (BigInteger, primary_key=True, nullable=False)
    path = Column (String) # name of the source file, could be image (ocr) or audio (asr)
    original_source = Column (String) # name of the original source file, could be image (ocr) or audio (asr)

    # The text to be searched.
    fulltext_path = Column (String) # name of the full-text file, 
    full_text = Column (String)  # doesn't have to be in the db... TBF

    # Text-search vector
    search_vector = Column (TSVectorType)

    # meta data
    metadata_path = Column (String) # name of the meta file
    meta_data = Column (String) # the actual metadata -- doesn't have to be stored in the db.. TBF

    # identify the data source (eg., "speech_to_text")
    service_name = Column (String) # rabbit service name
    
    # Create index for the text
    __table_args__ = (
        Index ('ix_fulltext_tsv',
               # __ts_vector__,
               search_vector,
               postgresql_using='gin'
        ),
    )

    ## Taken from Command/ner/ner_spacy/ner_utils.py:
    def concat_text(self, doc_tokens_or_chars, indices_to_return, spaces=True):
        '''
        Give the tokens or characters (from a document) and the indices of
        the tokens or characters that should be returned, string
        together the tokens or characters, string together the text so
        that omitted text is replaced with an ellipsis.

        Args:
            doc_tokens_or_chars:
              A list of tokens (ie words, punctuation) or a list of characters
              (letters, punctuation)
            indices_to_return:
              A list of indices indicating which tokens (or characters)  should be returned
            spaces:
              Leave as True when using this function to string together words

        '''

        indices_to_return = sorted(set(indices_to_return))
        snippets = ''
        for i in range(len(indices_to_return)):
            # Place ellipsis between non-adjacent tokens
            previous_index_plus_one = indices_to_return[i - 1] + 1
            is_next = indices_to_return[i] == previous_index_plus_one
            # if this token came right after the previous token (ie
            # this token is part of the same snippet as the previous
            # token)
            if not is_next:
                snippets += '...'
            word_to_append = doc_tokens_or_chars[indices_to_return[i]]
            if spaces:
                if word_to_append not in string.punctuation:
                    snippets += ' '
            snippets += word_to_append
        if len(indices_to_return) > 0 and indices_to_return[-1] != len(
                doc_tokens_or_chars)-1:  # If doesn't include the very end of the document
            snippets += '...'
        return snippets

    ## Taken from Command/ner/ner_spacy/ner_utils.py:
    def get_text_surrounding_entity(self, text, search_entity):
        # Find the search entity in the doc
        start_indices_of_search_entity = [m.start() for m in re.finditer(search_entity, text)]
        padding = 100
        string_indices_to_return = []
        for i in start_indices_of_search_entity:
            to_add = [j for j in range(i - padding, i + len(search_entity) + padding)]
            string_indices_to_return += to_add
        # Remove negatives
        string_indices_to_return = [i for i in string_indices_to_return if i >= 0]
        # Remove indices beyond the length of the text
        string_indices_to_return = [i for i in string_indices_to_return if i < len(text)]
        # Remove duplicates, sort, add ellipses if necessary.
        snippets = self.concat_text (text, string_indices_to_return, spaces=False)
        return snippets


    def serialize(self, search_entity=""):
        """Serialize Fulltext data.
        Args:
          search_entity: The query term

        Return:
          A dictionary.
        """

        # Currently, snippet only works if it matches the search entity exactly.
        snippets = None
        snippets = self.get_text_surrounding_entity (self.full_text.lower(), # ignore case
                                                     search_entity)

        # Convert ts_vector to python dictionary.  ts_vector has
        # information about word locations.
        # eg. 'ate':10 'cat':3 'fat':2,12 'mat':7 'rat':13 'sat':4 'spi':8
        tokenIdxLut = dict()
        pairs = [elm.split(':') for elm in self.search_vector.split()]
        for pair in pairs:
            key = eval(pair[0])
            val = eval(pair[1])# either int or tuple
            if type(val) == int:
                val = (val,)       # make it a tuple
            tokenIdxLut[key] = val # val is always a tuple

        
        # Apply stemming and lammanation (and others) to search entity:
        ts_query = None
        rs = db.engine.execute (f"SELECT to_tsquery('english', '{search_entity}');")
        # database = PostgresDatabase()
        # engine = database.get_or_create_engine()
        # rs = engine.execute(f"SELECT to_tsquery('english', '{search_entity}');")
        print (f"TESTING!!! search_entity = {search_entity}", flush=True)
        for row in rs:          # only has one elment
            # print (f"row = {row}", flush=True)
            assert (ts_query == None) # TBF -- check if loop only run once.
            ts_query = row[0].replace("'","")
        print (f"after lemmatization, ts_query = {ts_query}", flush=True)


        # Package and serial the result:
        textSearchResult = {
            "id": self.id,
            "path": self.path,
            "original_source": self.original_source,
            "fulltext_path": self.fulltext_path,
            "full_text": self.full_text,
            "query" : search_entity,
            "ts_query" : ts_query,
            "metadata_path": self.metadata_path,
            "service_name": self.service_name,
            "snippets": snippets,}

        # Addtional field depending on service_name: For
        # speech-to-text data, we also return the timestamp of the
        # search entity.  Note, timestamp currently does not work for
        # advanced query such as logical or proximity queries:
        if self.service_name == "speech_to_text":
            ts_query_timestamps = []
            if ts_query in tokenIdxLut:
                idxes = tokenIdxLut [ts_query] # idx start from 1
                meta_data = json.loads (self.meta_data)
                for _idx in idxes:
                    idx = _idx - 1  # make idx start from 0
                    (confidence, stop_time, start_time, ori_word) = ( # hardcoded structure - TBF
                        meta_data[idx])
                    ts_query_timestamps.append (start_time)
                    # print ("ts_query = ", ts_query, flush=True)
                    # print ("ori_word = ", ori_word, flush=True)
                    # print ("search_entity = ", search_entity, flush=True)
                    # assert (search_entity.lower() in ori_word.lower() ) # assert -- TBF
            else:
                print (f"**WARNING: id{self.id}: '{ts_query}' does not have timestamp", flush=True)
            textSearchResult["ts_query_timestamps"] = ts_query_timestamps

        elif ("ocr_" in self.service_name):
            # For OCR service, we return the locations (ie., the
            # bounding polygons) of the search term.  Note, currently
            # does not work for advanced query such as logical or
            # proximity queries.
            ts_query_polygons = [] 

            # meta_data is a dictionary with keys = "width", "height",
            # and "pred" "pred" is a list of [(bbs, text, conf)], We
            # search through and find the one containing the query term
            if self.meta_data:
                metaDataDict = eval(self.meta_data) # TBF: might raise exception
                width = metaDataDict['width']
                height = metaDataDict['height']
                predictions = metaDataDict['pred']
                print (f"ts_query = {ts_query}", flush=True)
                if ts_query in tokenIdxLut:
                    for prediction in predictions:
                        (bbs, text, conf) = prediction

                        #
                        # postgres lemmatization can turn the word
                        # "spy" to "spi", which we can not get a
                        # match.  So, in that case, we might as well
                        # match agains the original search term...
                        # But if we search for "spies" it will never
                        # match "spy"...
                        text = text.lower()
                        if ((ts_query in text) or (search_entity in text)):
                        # if ts_query in text.lower(): # poor-man's lemmatization method... TBF
                            print (f"Found text = {text}", flush=True)
                            ts_query_polygons.append(
                                [(corner[0]/width, corner[1]/height)
                                 for corner in bbs])
                            # print (f"ts_query_polygons = {ts_query_polygons}", flush=True)

                    # If we didn't find a match, that means
                    # lemmatization changed the original word too
                    # much.  We will just report it for now.  A proper
                    # fix is to call postgres "to_tsquery" function
                    # again on text, but that is likely to be
                    # expensive.
                    if (len (ts_query_polygons) == 0):
                        print (f"**WARNING: could not find in the OCR text " +
                               f"for '{search_entity}', which got lemmatized to '{ts_query}'.",
                               flush=True)
                        print (self.meta_data, flush=True)
                        # assert (len (ts_query_polygons)) # TBF: lemmatization issue..
                        
            textSearchResult["ts_query_polygons"] = ts_query_polygons
            

        # We are done.
        return textSearchResult
