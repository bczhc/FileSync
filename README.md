FileSync
---

Incrementally send files from Android to another host over network.

I use this to back up Android files periodically.

## Build
An example for arm64 Android devices:
1. Install Rust target: `aarch64-linux-android`
2. Create `config.properties` and write:

   ```properties
   ndk.target=arm64-v8a-29
   ndk.buildType=debug
   ```
3. Run `./gradlew asD`

## Usage:
1. See this repository: https://github.com/bczhc/adb-sync
2. Compile it on host
3. Run `tcp-receive` on host
4. Configure the host destination on Android
5. Add directory paths to sync on Android and then click "Sync"
