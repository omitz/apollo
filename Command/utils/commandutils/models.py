from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY, Index, func
from sqlalchemy_utils import TSVectorType

Base = declarative_base()
class DetectedFace(Base):
    __tablename__ = 'detected_faces'
    
    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    ulx = Column(Integer)
    uly = Column(Integer)
    lrx = Column(Integer)
    lry = Column(Integer)
    probability = Column(Float)
    prediction = Column(String)
    vector_id = Column(BigInteger)
    query_results = Column(ARRAY(BigInteger))

    def __repr__(self):
        return f"<DetectedFace  (id='{self.id}', path='{self.path}')>"

class DetectedObj(Base):
    __tablename__ = 'detected_objs'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    bb_ymin_xmin_ymax_xmax = Column(ARRAY(Float))
    detection_scores = Column(ARRAY(Float))
    detection_class = Column(String)

    def __repr__(self):
        return f"<DetectedObject  (id='{self.id}', path='{self.path}')>"

class VideoDetections(Base):
    __tablename__ = 'video_detections'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    detection_class = Column(String)
    detection_score = Column(Float)
    seconds = Column(ARRAY(Integer))

    def __repr__(self):
        return f"<VideoDetection  (id='{self.id}', path='{self.path}')>"

class FileHash(Base):
    __tablename__ = 'file_hashes'
    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    sha1 = Column(String)
    sha256 = Column(String)
    sha512 = Column(String)
    md5 = Column(String)

    def __repr__(self):
        return f"<FileHash  (id='{self.id}', path='{self.path}', sha1='{self.sha1})>"

class Landmark(Base):
    __tablename__ = 'landmarks'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    delf_locations = Column(ARRAY(Float))
    delf_descriptors = Column(ARRAY(Float))

    def __repr__(self):
        return f"<Landmark  (id='{self.id}', path='{self.path}')>"

class TestFaces(Base):
    __tablename__ = 'test_faces'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    vector_id = Column(BigInteger)
    query_results = Column(ARRAY(BigInteger))

    def __repr__(self):
        return f"<test  (id='{self.id}', path='{self.path}')>"

class ClassifyScene(Base):
    __tablename__ = 'scene_classes'

    id = Column (BigInteger, primary_key=True, nullable=False)
    path = Column (String)            # name of the image file
    class_hierarchy = Column (String) # either 'indoor' or 'outdoor'
    top_five_classes = Column (ARRAY(Integer)) # index into the classes list
    
    classes_ori = ['airfield', 'airplane_cabin', 'airport_terminal', 'alcove',
               'alley', 'amphitheater', 'amusement_arcade', 'amusement_park',
               'apartment_building/outdoor', 'aquarium', 'aqueduct', 'arcade',
               'arch', 'archaelogical_excavation', 'archive', 'arena/hockey',
               'arena/performance', 'arena/rodeo', 'army_base', 'art_gallery',
               'art_school', 'art_studio', 'artists_loft', 'assembly_line',
               'athletic_field/outdoor', 'atrium/public', 'attic', 'auditorium',
               'auto_factory', 'auto_showroom', 'badlands', 'bakery/shop',
               'balcony/exterior', 'balcony/interior', 'ball_pit', 'ballroom',
               'bamboo_forest', 'bank_vault', 'banquet_hall', 'bar', 'barn',
               'barndoor', 'baseball_field', 'basement',
               'basketball_court/indoor', 'bathroom', 'bazaar/indoor',
               'bazaar/outdoor', 'beach', 'beach_house', 'beauty_salon',
               'bedchamber', 'bedroom', 'beer_garden', 'beer_hall', 'berth',
               'biology_laboratory', 'boardwalk', 'boat_deck', 'boathouse',
               'bookstore', 'booth/indoor', 'botanical_garden',
               'bow_window/indoor', 'bowling_alley', 'boxing_ring', 'bridge',
               'building_facade', 'bullring', 'burial_chamber', 'bus_interior',
               'bus_station/indoor', 'butchers_shop', 'butte', 'cabin/outdoor',
               'cafeteria', 'campsite', 'campus', 'canal/natural', 'canal/urban',
               'candy_store', 'canyon', 'car_interior', 'carrousel', 'castle',
               'catacomb', 'cemetery', 'chalet', 'chemistry_lab', 'childs_room',
               'church/indoor', 'church/outdoor', 'classroom', 'clean_room',
               'cliff', 'closet', 'clothing_store', 'coast', 'cockpit',
               'coffee_shop', 'computer_room', 'conference_center',
               'conference_room', 'construction_site', 'corn_field', 'corral',
               'corridor', 'cottage', 'courthouse', 'courtyard', 'creek',
               'crevasse', 'crosswalk', 'dam', 'delicatessen',
               'department_store', 'desert/sand', 'desert/vegetation',
               'desert_road', 'diner/outdoor', 'dining_hall', 'dining_room',
               'discotheque', 'doorway/outdoor', 'dorm_room', 'downtown',
               'dressing_room', 'driveway', 'drugstore', 'elevator/door',
               'elevator_lobby', 'elevator_shaft', 'embassy', 'engine_room',
               'entrance_hall', 'escalator/indoor', 'excavation', 'fabric_store',
               'farm', 'fastfood_restaurant', 'field/cultivated', 'field/wild',
               'field_road', 'fire_escape', 'fire_station', 'fishpond',
               'flea_market/indoor', 'florist_shop/indoor', 'food_court',
               'football_field', 'forest/broadleaf', 'forest_path',
               'forest_road', 'formal_garden', 'fountain', 'galley',
               'garage/indoor', 'garage/outdoor', 'gas_station',
               'gazebo/exterior', 'general_store/indoor',
               'general_store/outdoor', 'gift_shop', 'glacier', 'golf_course',
               'greenhouse/indoor', 'greenhouse/outdoor', 'grotto',
               'gymnasium/indoor', 'hangar/indoor', 'hangar/outdoor', 'harbor',
               'hardware_store', 'hayfield', 'heliport', 'highway',
               'home_office', 'home_theater', 'hospital', 'hospital_room',
               'hot_spring', 'hotel/outdoor', 'hotel_room', 'house',
               'hunting_lodge/outdoor', 'ice_cream_parlor', 'ice_floe',
               'ice_shelf', 'ice_skating_rink/indoor',
               'ice_skating_rink/outdoor', 'iceberg', 'igloo', 'industrial_area',
               'inn/outdoor', 'islet', 'jacuzzi/indoor', 'jail_cell',
               'japanese_garden', 'jewelry_shop', 'junkyard', 'kasbah',
               'kennel/outdoor', 'kindergarden_classroom', 'kitchen', 'lagoon',
               'lake/natural', 'landfill', 'landing_deck', 'laundromat', 'lawn',
               'lecture_room', 'legislative_chamber', 'library/indoor',
               'library/outdoor', 'lighthouse', 'living_room', 'loading_dock',
               'lobby', 'lock_chamber', 'locker_room', 'mansion',
               'manufactured_home', 'market/indoor', 'market/outdoor', 'marsh',
               'martial_arts_gym', 'mausoleum', 'medina', 'mezzanine',
               'moat/water', 'mosque/outdoor', 'motel', 'mountain',
               'mountain_path', 'mountain_snowy', 'movie_theater/indoor',
               'museum/indoor', 'museum/outdoor', 'music_studio',
               'natural_history_museum', 'nursery', 'nursing_home', 'oast_house',
               'ocean', 'office', 'office_building', 'office_cubicles', 'oilrig',
               'operating_room', 'orchard', 'orchestra_pit', 'pagoda', 'palace',
               'pantry', 'park', 'parking_garage/indoor',
               'parking_garage/outdoor', 'parking_lot', 'pasture', 'patio',
               'pavilion', 'pet_shop', 'pharmacy', 'phone_booth',
               'physics_laboratory', 'picnic_area', 'pier', 'pizzeria',
               'playground', 'playroom', 'plaza', 'pond', 'porch', 'promenade',
               'pub/indoor', 'racecourse', 'raceway', 'raft', 'railroad_track',
               'rainforest', 'reception', 'recreation_room', 'repair_shop',
               'residential_neighborhood', 'restaurant', 'restaurant_kitchen',
               'restaurant_patio', 'rice_paddy', 'river', 'rock_arch',
               'roof_garden', 'rope_bridge', 'ruin', 'runway', 'sandbox',
               'sauna', 'schoolhouse', 'science_museum', 'server_room', 'shed',
               'shoe_shop', 'shopfront', 'shopping_mall/indoor', 'shower',
               'ski_resort', 'ski_slope', 'sky', 'skyscraper', 'slum',
               'snowfield', 'soccer_field', 'stable', 'stadium/baseball',
               'stadium/football', 'stadium/soccer', 'stage/indoor',
               'stage/outdoor', 'staircase', 'storage_room', 'street',
               'subway_station/platform', 'supermarket', 'sushi_bar', 'swamp',
               'swimming_hole', 'swimming_pool/indoor', 'swimming_pool/outdoor',
               'synagogue/outdoor', 'television_room', 'television_studio',
               'temple/asia', 'throne_room', 'ticket_booth', 'topiary_garden',
               'tower', 'toyshop', 'train_interior', 'train_station/platform',
               'tree_farm', 'tree_house', 'trench', 'tundra',
               'underwater/ocean_deep', 'utility_room', 'valley',
               'vegetable_garden', 'veterinarians_office', 'viaduct', 'village',
               'vineyard', 'volcano', 'volleyball_court/outdoor', 'waiting_room',
               'water_park', 'water_tower', 'waterfall', 'watering_hole', 'wave',
               'wet_bar', 'wheat_field', 'wind_farm', 'windmill', 'yard',
               'youth_hostel', 'zen_garden']
    ## replaced '_' and '/' with space.
    classes = [elm.replace('_', ' ').replace('/', ' ') for elm in classes_ori]

    
    def __repr__(self):
        return f"<ClassifyScene  (id='{self.id}', path='{self.path}')>"

class SearchFullText(Base):
    """
    ref: https://stackoverflow.com/questions/42388956/create-a-full-text-search-index-with-sqlalchemy-on-postgresql


    Query example:
        people = Person.query.filter (Person.__ts_vector__.match(expressions, 
                                  postgresql_regconfig='english')).all()

     session.query(Model).filter (Model.tsvector.op('@@')(func.plainto_tsquery('search string')))

    """
    
    __tablename__ = 'fulltext'

    id = Column (BigInteger, primary_key=True, nullable=False)
    path = Column (String) # name of the source file, could be image (ocr) or audio (asr)

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
