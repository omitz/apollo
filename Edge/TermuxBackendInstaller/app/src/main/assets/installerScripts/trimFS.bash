#!/data/data/com.termux/files/usr/bin/bash
set -eu
MY_NAME=`basename $0`

function err_report {
    echo "Fail @ [${MY_NAME}:${1}]"
    exit 1
}

trap 'err_report $LINENO' ERR

################################
# A. Termux Package removal
################################
apt-get update
apt-get remove clang --purge -y || true
apt-get autoremove --Purge -y || true
apt clean all; 
rm -rf ~/.cache/pip/


################################
# B. Debian Package removal
################################
cd ~/projects/AnLinux/debian;
strcmd=$(cat  << "EOF"
apt-get remove systemd --purge -y || true
apt-get remove linux-libc-dev -y || true
apt-get autoremove --Purge -y || true
apt clean all; 
rm -rf ~/.cache/pip/
EOF
)
./start-debian.sh "$strcmd"
cd -


################################
# C. Strip everything to reduce file size
################################
apt-get update
apt-get install binutils -y
apt-get clean all
cd /data/data/com.termux/files
(find -type f | xargs strip) || true


################################
# Success and clean up
################################
echo "Installation Successful"
