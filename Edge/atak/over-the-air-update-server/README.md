# Installing over-the-air Update Server

## Prepare local update directory
    ```
    mkdir update/
    cp app-mil-debug_arm64_v2p4.apk update/
    ./generate-inf.sh update true
    ```
Three output files will be gnerated:  (.png, product.inf, product.infz).
```
update/
├── app-mil-debug_arm64_v2p4.apk
├── app-mil-debug_arm64_v2p4.png
├── product.inf
└── product.infz
```
    
    
## Copy to the server
    Log into the TAKserver and create an directory containing the apk file.
    ```
    TAKServer=localhost    # change to your TAK server here
    PORT=8822              # change to our server ssh port (eg., 22)
    RCMD="sshpass -p'atakatak' ssh -p $PORT tak@$TAKServer"  # assume have ssh access
    echo "$RCMD mkdir -p /opt/tak/webcontent/update/" | sh
    echo "scp -P $PORT -r update tak@${TAKServer}:/opt/tak/webcontent/" | sh
    ## Below may not be needed:
    echo "$RCMD chmod a+r /opt/tak/webcontent/update -R" | sh
    echo "$RCMD chown tak:tak /opt/tak/webcontent/update -R" | sh
    ```

##  Configure ATAK Client

### Copy the certificate to the phone
```
adb push truststore-root.p12 /sdcard/Download/
adb push user2.p12 /sdcard/Download/
```

### Setup certificates on the phone
     - Click "Settings"->"Network Connections"->"Network Connections"
     - Click "Default SSL/TLS TrustStore Location"
       - load /sdcard/Download/truststore-root.p12
     - Click "Default SSL/TLS TrustStore Password"
       - enter "atakatak"
     - Click "Default SSL/TLS Client Certificate Store"
       - load /sdcard/Download/user2.p12
     - Click "Default SSL/TLS Client Certificate Password"
       - enter "atakatak"

### Setup Package Manager
     - Click "Settings"->"TAK Package Mgmt"
     - Click "..."->"Edit"
     - Check "Update Server"
     - Click "Update Server URL"
       - enter "https://192.168.1.167:8444/update"   # change to your TAK Server
     - Click "OK"
     - Click the sync icon to refresh the server.
