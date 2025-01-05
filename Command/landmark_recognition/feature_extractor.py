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
"""DELF feature extractor."""
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import tensorflow as tf

import datum_io
import delf_v1


def CalculateReceptiveBoxes(height, width, rf, stride, padding):
  """Calculate receptive boxes for each feature point.

  Args:
    height: The height of feature map.
    width: The width of feature map.
    rf: The receptive field size.
    stride: The effective stride between two adjacent feature points.
    padding: The effective padding size.

  Returns:
    rf_boxes: [N, 4] receptive boxes tensor. Here N equals to height x width.
    Each box is represented by [ymin, xmin, ymax, xmax].
  """
  x, y = tf.meshgrid(tf.range(width), tf.range(height))
  coordinates = tf.reshape(tf.stack([y, x], axis=2), [-1, 2])
  # [y,x,y,x]
  point_boxes = tf.cast(
      tf.concat([coordinates, coordinates], 1), dtype=tf.float32)
  bias = [-padding, -padding, -padding + rf - 1, -padding + rf - 1]
  rf_boxes = stride * point_boxes + bias
  return rf_boxes


def CalculateKeypointCenters(boxes):
  """Helper function to compute feature centers, from RF boxes.

  Args:
    boxes: [N, 4] float tensor.

  Returns:
    centers: [N, 2] float tensor.
  """
  return tf.divide(
      tf.add(
          tf.gather(boxes, [0, 1], axis=1), tf.gather(boxes, [2, 3], axis=1)),
      2.0)


def ApplyPcaAndWhitening(data,
                         pca_matrix,
                         pca_mean,
                         output_dim,
                         use_whitening=False,
                         pca_variances=None):
  """Applies PCA/whitening to data.

  Args:
    data: [N, dim] float tensor containing data which undergoes PCA/whitening.
    pca_matrix: [dim, dim] float tensor PCA matrix, row-major.
    pca_mean: [dim] float tensor, mean to subtract before projection.
    output_dim: Number of dimensions to use in output data, of type int.
    use_whitening: Whether whitening is to be used.
    pca_variances: [dim] float tensor containing PCA variances. Only used if
      use_whitening is True.

  Returns:
    output: [N, output_dim] float tensor with output of PCA/whitening operation.
  """
  output = tf.matmul(
      tf.subtract(data, pca_mean),
      tf.slice(pca_matrix, [0, 0], [output_dim, -1]),
      transpose_b=True,
      name='pca_matmul')

  # Apply whitening if desired.
  if use_whitening:
    output = tf.divide(
        output,
        tf.sqrt(tf.slice(pca_variances, [0], [output_dim])),
        name='whitening')

  return output


def PostProcessDescriptors(descriptors, use_pca, pca_parameters):
  """Post-process descriptors.

  Args:
    descriptors: [N, input_dim] float tensor.
    use_pca: Whether to use PCA.
    pca_parameters: DelfPcaParameters proto.

  Returns:
    final_descriptors: [N, output_dim] float tensor with descriptors after
      normalization and (possibly) PCA/whitening.
  """
  # L2-normalize, and if desired apply PCA (followed by L2-normalization).
  with tf.compat.v1.variable_scope('postprocess'):
    final_descriptors = tf.nn.l2_normalize(
        descriptors, axis=1, name='l2_normalization')

    if use_pca:
      # Load PCA parameters.
      pca_mean = tf.constant(
          datum_io.ReadFromFile(pca_parameters.mean_path), dtype=tf.float32)
      pca_matrix = tf.constant(
          datum_io.ReadFromFile(pca_parameters.projection_matrix_path),
          dtype=tf.float32)
      pca_dim = pca_parameters.pca_dim
      pca_variances = None
      if pca_parameters.use_whitening:
        pca_variances = tf.squeeze(
            tf.constant(
                datum_io.ReadFromFile(pca_parameters.pca_variances_path),
                dtype=tf.float32))

      # Apply PCA, and whitening if desired.
      final_descriptors = ApplyPcaAndWhitening(final_descriptors, pca_matrix,
                                               pca_mean, pca_dim,
                                               pca_parameters.use_whitening,
                                               pca_variances)

      # Re-normalize.
      final_descriptors = tf.nn.l2_normalize(
          final_descriptors, axis=1, name='pca_l2_normalization')

  return final_descriptors


def DelfFeaturePostProcessing(boxes, descriptors, config):
  """Extract DELF features from input image.

  Args:
    boxes: [N, 4] float tensor which denotes the selected receptive box. N is
      the number of final feature points which pass through keypoint selection
      and NMS steps.
    descriptors: [N, input_dim] float tensor.
    config: DelfConfig proto with DELF extraction options.

  Returns:
    locations: [N, 2] float tensor which denotes the selected keypoint
      locations.
    final_descriptors: [N, output_dim] float tensor with DELF descriptors after
      normalization and (possibly) PCA/whitening.
  """

  # Get center of descriptor boxes, corresponding to feature locations.
  locations = CalculateKeypointCenters(boxes)
  final_descriptors = PostProcessDescriptors(
      descriptors, config.delf_local_config.use_pca,
      config.delf_local_config.pca_parameters)

  return locations, final_descriptors
