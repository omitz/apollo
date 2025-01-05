# Virus and Malware scanning

## Installation

Currently for Ubuntu only.

### Setting up ClamAV and the daemon

Install clamav and the daemon. Run freshclam to update signatures.

```bash
$ sudo apt install clamav clamav-daemon
$ sudo freshclam
```

Edit /etc/clamav/clamd.conf to add the following above the LocalSocket entry.

```
TCPSocket 3310
TCPAddr 127.0.0.1
```

Then edit /etc/systemd/system/clamav-daemon.socket.d/extend.conf to add (at the bottom)
```
ListenStream=127.0.0.1:3310
```

Start the daemon
```bash
$ sudo /etc/init.d/clamav-daemon start
```

```bash
$ python3 -m venv venv
$ . venv/bin/activate
(venv) $ pip install --upgrade pip
(venv) $ pip install -r requirements.txt
```

## Usage

```bash
(venv) $ python main.py -h
usage: main.py [-h] [-i INPUT]

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        File to scan

```

The code currently copies the file to /tmp so that the daemon has the ability to scan. This could also be addressed in a Docker container with the correct groups/privileges.
