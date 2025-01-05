Mission API Library
_________________________________________________________________
PURPOSE AND CAPABILITIES

This is the client-side Mission API used by the Data Sync plugin.
This library may be used for other plugins that utilize the Mission API.

To import this library as a sub-project, `git clone` to the `plugins` directory and add the following to your project's `app/build.gradle` file:
```gradle
implementation project(':mission-api-lib:lib')
```

This library can also be built and imported in .aar format (added under the `libs` directory):
```gradle
implementation fileTree(dir: 'libs', include: ['*.aar', '*.jar'])
```

Within your plugin's source code, the library must be initialized with the following call:
```java
MissionAPI.init(<plugin context>);
```

Missions API ports: 8080, 8443

_________________________________________________________________
STATUS

Phase 1 complete.

_________________________________________________________________
ATAK VERSIONS

ATAK 4.2+

_________________________________________________________________
POINT OF CONTACTS

Primary Developer Contact:     Vincent Costanza. vincent_costanza@partech.com / 1-315-356-2137 / PAR
Program Office Contact:  Josh Sterling.  joshua.d.sterling.civ@mail.mil / USASOC 

_________________________________________________________________
USER GROUPS

USASOC

_________________________________________________________________
EQUIPMENT REQUIRED

TAK Server 1.3.6+

_________________________________________________________________
EQUIPMENT SUPPORTED

N/A

_________________________________________________________________
COMPILATION

Gradle build or Android Studio file

_________________________________________________________________
DEVELOPER NOTES

HTTP REST is used heavily, but not exclusively, to communicate with TAK Server.
TAK Server based streaming CoT is also used for some communications.

