package `in`.caffeinelabs.cassettecat.data.library

fun String.matchesFolderFilter(config: FolderFilterConfig): Boolean = when (config.mode) {
    FolderFilterMode.NONE -> true
    FolderFilterMode.WHITELIST -> config.folders.any { startsWith(it) }
    FolderFilterMode.BLACKLIST -> config.folders.none { startsWith(it) }
}
