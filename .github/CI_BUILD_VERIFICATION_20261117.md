# Forge build and JAR artifact verification

This metadata-only commit triggers the existing GitHub Actions smoke tests and Java 17 / Gradle 8.8 Forge build on a validation pull request.

The original mod source code and production `main` branch are unchanged. The Forge workflow uploads `build/libs/*.jar` only if the build succeeds.
