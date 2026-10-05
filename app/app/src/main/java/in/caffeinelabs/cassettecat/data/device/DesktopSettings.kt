package `in`.caffeinelabs.cassettecat.data.device

import `in`.caffeinelabs.cassettecat.data.backup.BackupAppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AlbumArtCornerStyle
import `in`.caffeinelabs.cassettecat.data.settings.CROSSFADE_SECONDS_OPTIONS
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.abs

private val DESKTOP_ACCENTS = mapOf(
    "RECORD_RED" to "recordRed",
    "CASSETTE_AMBER" to "amber",
    "ELECTRIC_CYAN" to "cyan",
    "NEON_EMERALD" to "emerald",
    "TAPE_MAGENTA" to "magenta",
    "MONOCHROME_SILVER" to "silver",
    "CUSTOM" to "custom"
)
private val DESKTOP_SERVICES = mapOf(
    ExternalService.DEEZER to "services/deezer",
    ExternalService.AUDIODB to "services/audiodb",
    ExternalService.LRCLIB to "services/lrclib",
    ExternalService.COVER_ART_ARCHIVE to "services/archive",
    ExternalService.WIKIPEDIA to "services/wiki",
    ExternalService.RADIO_BROWSER to "services/radio"
)
private val DESKTOP_LYRICS_SIZES = mapOf("SMALL" to 24, "MEDIUM" to 28, "LARGE" to 32)
private val DESKTOP_CROSSFADE_SECONDS = listOf(0, 3, 6, 10)

private fun List<Int>.nearest(value: Int) = minBy { abs(it - value) }

internal fun desktopSettingsFrom(prefs: BackupAppPreferences, services: ServiceSettings, desktop: JsonObject): JsonObject {
    val desktopReplayGain = desktop["player/replayGainMode"]?.jsonPrimitive?.contentOrNull
    return buildJsonObject {
        DESKTOP_ACCENTS[prefs.themeAccent]?.let { put("ui/accentName", it) }
        put("ui/customAccentColor", "#%06X".format(prefs.customAccentColor and 0xFFFFFF))
        put("ui/albumArtRadius", prefs.albumArtCornerRadiusDp)
        put("player/showRemainingTime", prefs.showRemainingTime)
        put("ui/trackDensity", if (prefs.trackRowDensity == "COMPACT") "compact" else "comfortable")
        put("ui/showFormatBadges", prefs.showAudioQualityBadge)
        put("player/crossfadeSeconds", DESKTOP_CROSSFADE_SECONDS.nearest(prefs.crossfadeSeconds))
        put("player/autoplayEnabled", prefs.autoplayEnabled)
        put("player/volumeLimitEnabled", prefs.volumeLimitEnabled)
        put("player/maxVolumePercent", prefs.maxVolumePercent)
        put("player/replayGainMode", if (!prefs.replayGainEnabled) "off" else desktopReplayGain.takeIf { it != null && it != "off" } ?: "track")
        put("player/resumeQueueOnLaunch", prefs.resumeQueueOnLaunch)
        put("library/ignoreShortClips", prefs.ignoreShortAudioClips)
        put("lyrics/alignment", if (prefs.lyricsAlignment == "LEFT") "left" else "center")
        put("lyrics/activeStyle", if (prefs.lyricsActiveStyle == "CLEAN_WHITE") "white" else "accent")
        put("lyrics/fontSize", DESKTOP_LYRICS_SIZES[prefs.lyricsFontSize] ?: 28)
        put("lyrics/preferLocal", prefs.localLrcPriority)
        DESKTOP_SERVICES.forEach { (service, key) -> put(key, services.isEnabled(service)) }
    }
}

internal fun BackupAppPreferences.withDesktopSettings(desktop: JsonObject): BackupAppPreferences {
    fun text(key: String) = desktop[key]?.jsonPrimitive?.contentOrNull
    fun flag(key: String) = desktop[key]?.jsonPrimitive?.booleanOrNull
    fun number(key: String) = desktop[key]?.jsonPrimitive?.intOrNull
    return copy(
        themeAccent = text("ui/accentName")?.let { name -> DESKTOP_ACCENTS.entries.firstOrNull { it.value == name }?.key } ?: themeAccent,
        customAccentColor = text("ui/customAccentColor")?.removePrefix("#")?.toLongOrNull(16)?.let { 0xFF000000 or it } ?: customAccentColor,
        albumArtCornerRadiusDp = number("ui/albumArtRadius")?.let { AlbumArtCornerStyle.entries.map { style -> style.radiusDp }.nearest(it) }
            ?: albumArtCornerRadiusDp,
        showRemainingTime = flag("player/showRemainingTime") ?: showRemainingTime,
        trackRowDensity = text("ui/trackDensity")?.let { if (it == "compact") "COMPACT" else "DETAILED" } ?: trackRowDensity,
        showAudioQualityBadge = flag("ui/showFormatBadges") ?: showAudioQualityBadge,
        crossfadeSeconds = number("player/crossfadeSeconds")?.let(CROSSFADE_SECONDS_OPTIONS::nearest) ?: crossfadeSeconds,
        autoplayEnabled = flag("player/autoplayEnabled") ?: autoplayEnabled,
        volumeLimitEnabled = flag("player/volumeLimitEnabled") ?: volumeLimitEnabled,
        maxVolumePercent = number("player/maxVolumePercent") ?: maxVolumePercent,
        replayGainEnabled = text("player/replayGainMode")?.let { it != "off" } ?: replayGainEnabled,
        resumeQueueOnLaunch = flag("player/resumeQueueOnLaunch") ?: resumeQueueOnLaunch,
        ignoreShortAudioClips = flag("library/ignoreShortClips") ?: ignoreShortAudioClips,
        lyricsAlignment = text("lyrics/alignment")?.let { if (it == "left") "LEFT" else "CENTER" } ?: lyricsAlignment,
        lyricsActiveStyle = text("lyrics/activeStyle")?.let { if (it == "white") "CLEAN_WHITE" else "ACCENT_GLOW" } ?: lyricsActiveStyle,
        lyricsFontSize = number("lyrics/fontSize")?.let { size -> DESKTOP_LYRICS_SIZES.minBy { abs(it.value - size) }.key } ?: lyricsFontSize,
        localLrcPriority = flag("lyrics/preferLocal") ?: localLrcPriority
    )
}

internal fun desktopServiceStates(desktop: JsonObject): Map<ExternalService, Boolean> =
    DESKTOP_SERVICES.mapNotNull { (service, key) -> desktop[key]?.jsonPrimitive?.booleanOrNull?.let { service to it } }.toMap()
