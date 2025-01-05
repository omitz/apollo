#!/data/data/com.termux/files/usr/bin/bash
set -eu
MY_NAME=`basename $0`

function err_report {
    echo "Fail @ [${MY_NAME}:${1}]"
    exit 1
}

trap 'err_report $LINENO' ERR

################################
# A. BASE INSTALLATION
################################

echo "## 1.) Setup Termux storage and api"
apt update; apt upgrade -y;
termux-setup-storage;
apt install termux-api -y;

echo "## 2.) Install Openssh"
apt install openssh -y
if (! grep X11Forwarding /data/data/com.termux/files/usr/etc/ssh/sshd_config); then
    echo "X11Forwarding yes" >> /data/data/com.termux/files/usr/etc/ssh/sshd_config
    echo "ListenAddress 127.0.0.1" >> /data/data/com.termux/files/usr/etc/ssh/sshd_config
fi
echo -e "apollo\napollo" | passwd
if (ps | grep sshd); then
  sshd  ## will it get kill after exit?
fi
mkdir -p ~/.termux/boot/ || true
cat <<"EOF" > ~/.termux/boot/A_base.sh
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock

## Make some notification next.
TS=`date +"%H:%M:%S"`
termux-notification -c "Boot $TS: Starting Boot Scripts"

# Create a tmux to work around Android 10 (Some Termux-API won't work in ssh)
tmux kill-session -t termux || true
tmux new -s termux -d

## Start ssh deamon
termux-wake-unlock
killall sshd
sshd -D               # don't exit
EOF
chmod a+x  ~/.termux/boot/A_base.sh

echo "## 3.) Install basic and useful software."
apt install tmux -y;  ## Useful for running program remotely
apt install nano less tree python -y;
pip install --upgrade pip
pip install remi;   ## Useful for creating front-end webapp using python 

echo "## 4.) clean up"
apt clean all; apt update; apt upgrade -y;
rm -rf ~/.cache/pip/

echo "## 5.) Install Debian"
if [[ ! -d ~/projects/AnLinux/debian/ ]]; then
  mkdir -p ~/projects/AnLinux/debian; cd ~/projects/AnLinux/debian;
  pkg install wget openssl-tool proot -y && hash -r && wget https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Scripts/Installer/Debian/debian.sh && bash debian.sh

  # enable /sdcard
  sed -i 's|#command+=" -b \/sdcard"|command+=" -b \/sdcard"|' \
      ~/projects/AnLinux/debian/start-debian.sh
  
  # insert lines after #command+=" -b /sdcard"
  if (! grep storage ~/projects/AnLinux/debian/start-debian.sh); then
    sed -i '/b \/sdcard/a command+=" -b \/storage"' ~/projects/AnLinux/debian/start-debian.sh

    ## these are needed to access termux-api and broadcast within debian
    sed -i '/b \/sdcard/a command+=" -b \/data"' ~/projects/AnLinux/debian/start-debian.sh
    sed -i '/b \/sdcard/a command+=" -b \/system"' ~/projects/AnLinux/debian/start-debian.sh
  fi

  ## Add more DNS alternatives
  if [[ -e ~/projects/AnLinux/debian/debian-fs/etc/resolv.conf ]]; then
      cat <<EOF >> ~/projects/AnLinux/debian/debian-fs/etc/resolv.conf
nameserver 64.6.64.6
nameserver 149.112.112.112
nameserver 8.26.56.26
nameserver 199.85.126.10
EOF
  fi
fi
       
################################
# Success and clean up
################################
echo "Installation Successful"
