package `in`.caffeinelabs.cassettecat.data.library

// Operates on the raw MediaStore path, before a Song exists (Song has no filePath,
// that's a local-scanning-only concern).
fun String.matchesFolderFilter(config: FolderFilterConfig): Boolean = when (config.mode) {
    FolderFilterMode.NONE -> true
    FolderFilterMode.WHITELIST -> config.folders.any { startsWith(it) }
    FolderFilterMode.BLACKLIST -> config.folders.none { startsWith(it) }
}
