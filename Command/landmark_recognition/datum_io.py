# Copyright 2017 The TensorFlow Authors All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
"""Python interface for DatumProto.

DatumProto is protocol buffer used to serialize tensor with arbitrary shape.
Please refer to datum.proto for details.

Support read and write of DatumProto from/to NumPy array and file.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import numpy as np
import tensorflow as tf

from protos import datum_pb2


def ArrayToDatum(arr):
  """Converts NumPy array to DatumProto.

  Supports arrays of types:
    - float16 (it is converted into a float32 in DatumProto)
    - float32
    - float64 (it is converted into a float32 in DatumProto)
    - uint8 (it is converted into a uint32 in DatumProto)
    - uint16 (it is converted into a uint32 in DatumProto)
    - uint32
    - uint64 (it is converted into a uint32 in DatumProto)

  Args:
    arr: NumPy array of arbitrary shape.

  Returns:
    datum: DatumProto object.

  Raises:
    ValueError: If array type is unsupported.
  """
  datum = datum_pb2.DatumProto()
  if arr.dtype in ('float16', 'float32', 'float64'):
    datum.float_list.value.extend(arr.astype('float32').flat)
  elif arr.dtype in ('uint8', 'uint16', 'uint32', 'uint64'):
    datum.uint32_list.value.extend(arr.astype('uint32').flat)
  else:
    raise ValueError('Unsupported array type: %s' % arr.dtype)

  datum.shape.dim.extend(arr.shape)
  return datum


def DatumToArray(datum):
  """Converts data saved in DatumProto to NumPy array.

  Args:
    datum: DatumProto object.

  Returns:
    NumPy array of arbitrary shape.
  """
  if datum.HasField('float_list'):
    return np.array(datum.float_list.value).astype('float32').reshape(
        datum.shape.dim)
  elif datum.HasField('uint32_list'):
    return np.array(datum.uint32_list.value).astype('uint32').reshape(
        datum.shape.dim)
  else:
    raise ValueError('Input DatumProto does not have float_list or uint32_list')


def ParseFromString(string):
  """Converts serialized DatumProto string to NumPy array.

  Args:
    string: Serialized DatumProto string.

  Returns:
    NumPy array.
  """
  datum = datum_pb2.DatumProto()
  datum.ParseFromString(string)
  return DatumToArray(datum)


def ReadFromFile(file_path):
  """Helper function to load data from a DatumProto format in a file.

  Args:
    file_path: Path to file containing data.

  Returns:
    data: NumPy array.
  """
  with tf.io.gfile.GFile(file_path, 'rb') as f:
    return ParseFromString(f.read())