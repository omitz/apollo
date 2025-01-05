#!/bin/bash
#sudo apt-get update

#install dependencies
#sudo apt-get install -y unzip
#sudo apt-get install -y python
#sudo apt-get install -y default-jdk

#install jenkins
#wget -qO - https://pkg.jenkins.io/debian-stable/jenkins.io.key | apt-key add -
#sudo sh -c 'echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
#sudo apt-get update
#sudo apt-get install -y jenkins

#install aws cli
#curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
#unzip awscli-bundle.zip
#sudo ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws

#install kubectl
#curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl
#chmod +x ./kubectl
#sudo mv ./kubectl /usr/local/bin/kubectl

#install helm
#wget https://get.helm.sh/helm-v3.0.2-linux-amd64.tar.gz
#tar -zxvf helm-v3.0.2-linux-amd64.tar.gz
#sudo mv linux-amd64/helm /usr/local/bin/helm

#install docker
#sudo apt-get install -y docker.io
#sudo usermod -aG docker ubuntu
#sudo usermod -aG docker jenkins

sudo apt-get update

#mount efs
sudo service jenkins stop
sudo apt-get -y install nfs-common
sudo mkdir -p /var/lib/jenkins/
sudo chown -R jenkins:jenkins /var/lib/jenkins/
sudo echo "$(curl -s http://169.254.169.254/latest/meta-data/placement/availability-zone).${efs-id}.efs.${region}.amazonaws.com:/ /var/lib/jenkins nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 0 0" | sudo tee -a /etc/fstab
sudo mount -a -t -v nfs4
sudo chown -R jenkins:jenkins /var/lib/jenkins/

sudo reboot
sudo service jenkins start
