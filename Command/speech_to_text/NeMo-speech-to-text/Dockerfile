################################################################################
# Application Container
################################################################################
# Copyright (c) 2019, NVIDIA CORPORATION. All rights reserved.
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

################################################################################
# NGC base image
#-------------------------------------------------------------------------------
FROM nvcr.io/nvidia/nemo:v0.10

################################################################################
# Install system modules
#-------------------------------------------------------------------------------
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update

# Install EasyDict for manifest
RUN pip install easydict

# Install nodejs for jupyterlab
RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

# install jupyter lab extensions
RUN pip install jupyterlab-nvdashboard
RUN jupyter labextension install jupyterlab-nvdashboard
RUN jupyter lab build

# Reqs for n-gram language model 
RUN apt-get install -y libboost-all-dev swig
RUN cd /workspace/nemo/scripts && \
    ./install_decoders.sh
    
# NeMo ASR application
COPY . /tmp/nemo_asr_app
COPY lab-a511.jupyterlab-workspace /root/.jupyter/lab/workspaces/
RUN mkdir -p /tmp/nemo_asr_app/models/Jasper10x5DR/
ADD https://api.ngc.nvidia.com/v2/models/nvidia/multidataset_jasper10x5dr/versions/3/files/jasper10x5dr.yaml /tmp/nemo_asr_app/models/Jasper10x5DR/
ADD https://api.ngc.nvidia.com/v2/models/nvidia/multidataset_jasper10x5dr/versions/3/files/JasperDecoderForCTC-STEP-265520.pt /tmp/nemo_asr_app/models/Jasper10x5DR/
ADD https://api.ngc.nvidia.com/v2/models/nvidia/multidataset_jasper10x5dr/versions/3/files/JasperEncoder-STEP-265520.pt /tmp/nemo_asr_app/models/Jasper10x5DR/


################################################################################
# Set paths and user
#------------------------------------------------------------------------------
ARG WK_DIR=/tmp/nemo_asr_app
ENV APP_DIR $WK_DIR
#ENV WORK_DIR ${APP_DIR}/notebooks
ENV WORK_DIR ${APP_DIR}/
ENV PATH=$PATH:/usr/src/tensorrt/bin
ENV PYTHONPATH "${PYTHONPATH}:${APP_DIR}"

# Working directory
WORKDIR ${WORK_DIR}
