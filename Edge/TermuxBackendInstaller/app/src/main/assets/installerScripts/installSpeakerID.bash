#!/data/data/com.termux/files/usr/bin/bash
set -eu
MY_NAME=`basename $0`

function err_report {
    echo "Fail @ [${MY_NAME}:${1}]"
    exit 1
}

trap 'err_report $LINENO' ERR

################################
# B.1 Speaker ID BackEnd TERMUX Installation
################################
        
echo "## 2.) Install additional packages in Termux."
apt install ffmpeg -y;
pip install watchdog
        
echo "## 3.) Within Debian, install system-wide packages for the speaker recognition back-end."
cd ~/projects/AnLinux/debian;
strcmd=$(cat  << "EOF"
apt update; apt upgrade -y;
apt install tmux -y;
apt install portaudio19-dev python3-pyaudio --no-install-recommends -y;
apt install libsndfile1 --no-install-recommends -y;
apt install python3-numpy --no-install-recommends -y;
apt install python3-pandas --no-install-recommends -y;
apt install python3-soundfile --no-install-recommends -y;
apt install python3-sklearn --no-install-recommends -y;
apt install python3-numba --no-install-recommends -y;
EOF
)
./start-debian.sh "$strcmd"

echo "## 4.) Still within Debian, install Python 3 virtual environment."
strcmd=$(cat  << "EOF"
apt install virtualenvwrapper --no-install-recommends -y;
if (! grep virtualenvs ~/.bashrc); then
  echo -e "\n# virtualenv and virtualenvwrapper" >> ~/.bashrc;
  echo "export WORKON_HOME=$HOME/.virtualenvs" >> ~/.bashrc;
  echo "export VIRTUALENVWRAPPER_PYTHON=/usr/bin/python2" >> ~/.bashrc;
  echo "source /etc/bash_completion.d/virtualenvwrapper" >> ~/.bashrc;
fi
EOF
)
./start-debian.sh "$strcmd"

strcmd=$(cat  << "EOF"
if [[ ! -d ~/.virtualenvs/ ]]; then
  mkvirtualenv venv3 -p /usr/bin/python3 --system-site-packages || true
fi
EOF
)
./start-debian.sh "$strcmd"

strcmd=$(cat  << "EOF"
source ~/.virtualenvs/venv3/bin/activate; # same as workon venv3

echo "## 5.) Within Python 3 environment, install the needed python packages for the speaker recognition back-end."
echo "## 6.) Install pre-build TensorFlow Lite Runtime 2.1"
if (! pip list | grep tflite-runtime); then
  pip install librosa==0.7.0;
  if [[ "$(uname -m)" =  "x86_64" ]]; then
    pip install https://dl.google.com/coral/python/tflite_runtime-2.1.0-cp37-cp37m-linux_x86_64.whl
  else
    pip install https://dl.google.com/coral/python/tflite_runtime-2.1.0-cp37-cp37m-linux_aarch64.whl
  fi
fi
EOF
)
./start-debian.sh "$strcmd"

## done
cd -
        

################################
# Success and clean up
################################
echo "Installation Successful"

