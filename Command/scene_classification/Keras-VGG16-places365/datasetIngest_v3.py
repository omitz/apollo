#!/usr/bin/env python3
#
# This program directly populates the database without running the
# scene classification.  This program must be running with the Apollo
# infrastructure.
#
# 2020-07-25 (Sat)

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import time
from pathlib import Path

from commandutils import postgres_utils, models
from commandutils import rabbit_utils, s3_utils
from commandutils.rabbit_worker import RabbitWorker

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
S3_INPUT_DIRECTORY = 'inputs/load_test/test_256/'


#-------------------------
# Private Implementations 
#-------------------------

classes = ['airfield', 'airplane_cabin', 'airport_terminal', 'alcove',
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

categoryLut = {category:idx for (idx,category) in enumerate (classes)}

              
#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------

    #---------------------------
    # run the program :
    #---------------------------

    # input file
    print (f"Downloading pre-computed result", file=sys.stderr)
    s3_resultFile_path = "s3://" + str(Path(s3_utils.S3_BUCKET) / S3_INPUT_DIRECTORY / "result.txt")
    (s3, target) = s3_utils.access_bucket_and_download (s3_resultFile_path, "/dev/shm")
    testResut_Path = Path("/dev/shm") / Path(target).name


    # Build the result lookup -- because only want a subset of result,
    # depending on if image is on S3 or not.
    print (f"Loading entire test set result", file=sys.stderr)
    resultLut = dict()
    for line in testResut_Path.read_text().splitlines():
        words = line.split()
        imgName = words[0]
        classHier = words[1]
        top5s = eval("".join(words[2:]))
        top5Idxs = [categoryLut[category] for category in top5s]
        s3_file_path = "s3://" + str (Path(s3_utils.S3_BUCKET) / S3_INPUT_DIRECTORY / imgName)
        resultLut [s3_file_path] = (classHier, top5Idxs)
    
    # Open the database:
    postgres_utils.init_database (models.ClassifyScene)
    engine = postgres_utils.get_engine()
    session = postgres_utils.get_session(engine)

    # Go over the image we have on S3:
    print (f"Parsing S3 {s3_utils.S3_BUCKET}, {S3_INPUT_DIRECTORY}", file=sys.stderr)
    for (idx, elm) in enumerate (s3_utils.iterate_bucket_items(bucket=s3_utils.S3_BUCKET,
                                                               directory=S3_INPUT_DIRECTORY)):
        key="path"
        val= elm["Key"]         # eg. inputs/load_test/iaprtc12/images/00/33.jpg
        if not val.endswith(".jpg"):
            continue

        # Populate the database:
        s3_file_path = f's3://{s3_utils.S3_BUCKET}/{val}'
        processed = postgres_utils.check_processed (s3_file_path, session, models.ClassifyScene)

        # If this image isn't in the postgres db yet
        if (not processed) and (s3_file_path in resultLut): 
            
            (classHier, top5Idxs) = resultLut [s3_file_path]
            row = dict()
            row['path'] = s3_file_path
            row['class_hierarchy'] = classHier
            row['top_five_classes'] = top5Idxs
            postgres_utils.save_record_to_database (engine, row, models.ClassifyScene)
            print (f"Saved s3_file_path = {s3_file_path}", file=sys.stderr)
        else:
            print (f"{s3_file_path} skipped", file=sys.stderr)
        
    #---------------------------
    # program termination:
    #---------------------------

