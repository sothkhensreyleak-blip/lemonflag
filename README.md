# LemonFlag 3.0.0 (source project)

Paper plugin providing PlaceholderAPI placeholders:
- `%lemon_flag%`
- `%lemon_country%`
- `%lemon_code%`

Requires Paper and PlaceholderAPI. No plugin config file is generated.

## Build

Requires JDK 25 and Gradle 8.14+ (or a compatible newer Gradle release).

Run:

```bash
gradle clean build
```

The plugin jar will be in `build/libs/`.

## Install

Put the built JAR and PlaceholderAPI JAR in the server's `plugins/` folder, then fully restart Paper.

## Notes

Country lookup sends the connecting player's public IP address to `https://ipwho.is/`. Local/private IP addresses cannot be geolocated and show `🌐 / Unknown / XX`. Behind a proxy, Paper must receive the real player IP through correctly configured forwarding. This project is source code and has not been tested on a live Paper 26.2 server in this environment.
