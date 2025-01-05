#!/data/data/com.termux/files/usr/bin/bash
set -eu
MY_NAME=`basename $0`

function err_report {
    echo "Fail @ [${MY_NAME}:${1}]"
    exit 1
}

trap 'err_report $LINENO' ERR

################################
# C. Anti Virus BackEnd Installation
################################
echo "## 1.) Install ClamAV and update the latest virus database."
apt install clamav -y;
apt install file -y;
freshclam;

echo "## 2.) Add ClamAV deamon to boot script.  (i.e., Insert clamd just before the line termux-wake-unlock)"
cat <<"EOF" > ~/.termux/boot/C_anitVirus.sh
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock
clamd
termux-wake-unlock
EOF
chmod a+x  ~/.termux/boot/C_anitVirus.sh


################################
# Success and clean up
################################
echo "Installation Successful"
