FROM ubuntu:18.04

RUN apt-get update
RUN apt-get install -y unzip
RUN apt-get install -y python
RUN apt-get install -y curl
RUN apt-get install -y wget

#install aws cli
RUN curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
RUN unzip awscli-bundle.zip
RUN ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws

#install kubectl
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl

#install helm
RUN wget https://get.helm.sh/helm-v3.0.2-linux-amd64.tar.gz
RUN tar -zxvf helm-v3.0.2-linux-amd64.tar.gz
RUN mv linux-amd64/helm /usr/local/bin/helm

#install docker, docker-compose
RUN apt-get install -y docker.io
#RUN curl -L "https://github.com/docker/compose/releases/download/1.25.3/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
#RUN chmod +x /usr/local/bin/docker-compose
RUN apt-get install -y gnupg2 pass

RUN apt-get install -y docker-compose

RUN aws configure set default.region us-east-1
