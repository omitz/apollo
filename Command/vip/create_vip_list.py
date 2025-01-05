import pandas as pd
import sys

# Load metadata for VGGFace2
faces = pd.read_csv('identity_meta.csv', sep=', ')
# ignore the ones in the train set
faces = faces.loc[faces['Flag'] == 0]
# # remove the quotes around names
faces['Name'] = faces.Name.map(lambda x: x.lstrip('\"').rstrip('\"'))
print(f'VGG FACE 2:\n{faces.head(3)}\n\nexample: \n{faces.iloc[[0]].values}\n\n')

# # Let's see what's in the vox1 test set
# voices = pd.read_csv('vox1_meta.csv', sep='\t')
# # Make sure 'isin' line works
# length = len(voices)
# voices.loc[length] = ['id x', '14th_Dalai_Lama', 'male', 'not sure', 'w/e' ]
# print(f'voices example: \n{voices.iloc[[0]].values}\n')
# in_both = faces[faces['Name'].isin(voices['VGGFace1 ID'].values)]
# print(in_both)
# # No matches = v sad

# Let's see what's in the vox2 dataset
voices = pd.read_csv('vox2_meta.csv')
voices.columns = ['VoxCeleb2_ID', 'VGGFace2_ID', 'Gender', 'VoxCeleb2_Set']
# remove spaces
for column in voices.columns:
    voices[column] = voices[column].map(lambda x: x.lstrip().rstrip())
print(f'VOX CELEB 2:\n{voices.head(3)}\n\nexample: \n{voices.iloc[[0]].values}\n\n')

# Get the people who are in the VGGFace2 AND VoxCeleb2 TEST sets
voices_test = voices.loc[voices['VoxCeleb2_Set'] == 'test']
vggface2ids = voices_test['VGGFace2_ID'].values
in_both = faces[faces['Class_ID'].isin(vggface2ids)]
print(f'in both test sets: \n{in_both.head()}\n')
print(f'num in both test sets: {len(in_both)}')
# Note: Luke_Hemsworth is included in both test sets according to this, but my download of voxceleb2 didn't have any recordings for his id. It's possible that he only has samples in the video dataset.

# Get the people who are in the VGGFace2 and VoxCeleb sets via isin
vggface2ids = voices['VGGFace2_ID'].values
in_both = faces[faces['Class_ID'].isin(vggface2ids)]
# # Just to compare methods
# in_both.to_csv('in_vggface2_test_and_voxceleb2.csv', index=False)
print(f'len in both vgg test and vox all: {len(in_both)}')

# Get the people who are in the VGGFace2 and VoxCeleb sets via join
faces.columns = ['VGGFace2_ID', 'Name', 'Sample_Num', 'Flag', 'Gender_duplicate']
in_both = pd.merge(faces, voices, on='VGGFace2_ID')
# in_both = pd.merge(faces, voices_test, on='VGGFace2_ID')
print(f'len in both vgg test and vox all: {len(in_both)}')
in_both.drop(columns=['Flag', 'Gender_duplicate'], inplace=True)
# Commenting out so we don't overwrite existing
# in_both.to_csv('in_vggface2_test_and_voxceleb2.csv', index=False)

# Build the vip csv using the data from VGGFace2 test and VoxCeleb2
vips = ['Ewan_McGregor', 'Leonardo_DiCaprio', 'Aishwarya_Rai_Bachchan', 'Liza_Minnelli', 'Frankie_Muniz', 'Mohammad_Ali', 'Jim_Gaffigan', 'Katie_Holmes', 'Liam_Neeson', 'Haley_Joel_Osment']
vips = in_both[in_both['Name'].isin(vips)]
# Commenting out so we don't overwrite existing
# vips.to_csv('vips.csv', index=False)



