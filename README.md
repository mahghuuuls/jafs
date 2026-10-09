# Just Another Food System

A Minecraft 1.12.2 Forge mod. Replaces hunger with timed, configurable food bonuses and delayed health recovery. Ships no food definitions; pack makers and addons supply them.

Work in progress. This file is replaced by the full repository documentation at the first release.

- Source: https://github.com/mahghuuuls/jafs
- License: MIT (see `LICENSE`); template notices in `THIRD-PARTY-NOTICES.md`

## Building

The project uses the CleanroomMC ForgeDevEnv layout (Gradle wrapper, Java 25 toolchain, Java 8 output).

```text
./gradlew build
```

Third-party jars used only in the development runtime go in the git-ignored `libs/` folder; see `gradle/scripts/dependencies.gradle`.
