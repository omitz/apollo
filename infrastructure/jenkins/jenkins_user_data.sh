#!/bin/bash
sudo apt-get update

#install dependencies
sudo apt-get install -y unzip
sudo apt-get install -y python
sudo apt-get install -y default-jdk

#install aws cli
curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
unzip awscli-bundle.zip
sudo ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws

#install kubectl
curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl

#install helm
wget https://get.helm.sh/helm-v3.0.2-linux-amd64.tar.gz
tar -zxvf helm-v3.0.2-linux-amd64.tar.gz
sudo mv linux-amd64/helm /usr/local/bin/helm

#install jenkins
wget -q -O - https://pkg.jenkins.io/debian/jenkins-ci.org.key | sudo apt-key add -
sudo sh -c 'echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt-get update
sudo apt-get install -y jenkins

#install docker
sudo apt-get install -y docker.io
sudo usermod -aG docker $USER
sudo usermod -aG docker ubuntu
sudo usermod -aG docker jenkins

#misc
aws configure set default.region us-east-1

sudo apt-get update
sudo reboot