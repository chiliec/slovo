fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## iOS

### ios signing_assets

```sh
[bundle exec] fastlane ios signing_assets
```

One-time (local): create the Apple Distribution cert + App Store profile

### ios beta

```sh
[bundle exec] fastlane ios beta
```

Build and upload a new build to TestFlight

### ios release

```sh
[bundle exec] fastlane ios release
```

Prepare the App Store listing: push metadata + screenshots (does NOT submit)

### ios submit

```sh
[bundle exec] fastlane ios submit
```

Submit the APP_VERSION App Store version for review (attaches the latest build)

----


## Android

### android play_stage

```sh
[bundle exec] fastlane android play_stage
```

Stage the Play listing from store-assets/ without uploading (no credentials needed)

### android bundle

```sh
[bundle exec] fastlane android bundle
```

Build the signed release AAB

### android play_internal

```sh
[bundle exec] fastlane android play_internal
```

Build and upload a new build to the Play internal testing track

### android play_listing

```sh
[bundle exec] fastlane android play_listing
```

Push the Play listing (text, screenshots, graphics) without a binary

### android play_promote

```sh
[bundle exec] fastlane android play_promote
```

Promote the internal-track build to production (staged rollout)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
