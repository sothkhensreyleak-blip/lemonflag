# LemonFlag 3.0.0

Paper 26.2 / Java 25 plugin. Requires PlaceholderAPI for placeholders:
- `%lemon_flag%`
- `%lemon_country%`
- `%lemon_code%`

Build with `gradle clean build`; output JAR is in `build/libs/`.

GeoIP lookup is asynchronous through `https://ipwho.is/`. The player's public connection IP is sent to that service for country detection. Results are cached in memory for 30 minutes. This plugin does not create a plugin data folder or config file and does not save IP addresses to disk. Flag emoji display depends on the Minecraft client font/resource pack.
