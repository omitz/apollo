# Speaker Recognition

## Network Explanation 

When the network in this repo (vggvox_resnet2d_icassp) was trained, the label for each sample was a one-hot encoded vector indicating which person was the speaker (as is done for most classification networks). 

In train mode, the last layer of the model is a Dense layer with <num people\> nodes. In this way, the layer before the final Dense layer, the *vlad pooling layer, is trained to output a <bottleneck dim\>-length vector. Thus, these vector outputs (from multiple samples), in their <bottleneck dim\>-dimensional space were optimized to be far from other people's samples and close to samples from the same person.

In eval mode, the Dense layer mentioned above is not included. The output is the <bottleneck dim\>-length vector. Then, speaker verification is performed, i.e. a pair-wise similarity assessment is run.

## Resources
https://github.com/WeidiXie/VGG-Speaker-Recognition