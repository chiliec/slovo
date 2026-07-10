package cx.viz.slovo.platform

/**
 * True in debug/dev builds, false in release. Gates debug-only UI such as the
 * SRS "+1 DAY" clock control on the YOU screen so it never ships to users.
 */
expect val isDebugBuild: Boolean
