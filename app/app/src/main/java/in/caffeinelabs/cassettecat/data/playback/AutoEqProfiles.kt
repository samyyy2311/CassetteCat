package `in`.caffeinelabs.cassettecat.data.playback

import kotlin.math.abs
import kotlin.math.log10

data class AutoEqProfile(
    val name: String,
    val brand: String,
    val gainsDb: Map<Int, Float>
)

object AutoEqProfiles {
    val profiles: List<AutoEqProfile> = listOf(
        // Apple & Beats
        AutoEqProfile("AirPods Pro 2", "Apple", mapOf(31 to 0.8f, 62 to 0.2f, 125 to -0.6f, 250 to -0.4f, 500 to 0.0f, 1000 to 0.2f, 2000 to -0.8f, 4000 to 1.2f, 8000 to -1.5f, 16000 to 1.0f)),
        AutoEqProfile("AirPods Pro", "Apple", mapOf(31 to 1.2f, 62 to 0.5f, 125 to -0.5f, 250 to -0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to -1.0f, 4000 to 1.8f, 8000 to -2.0f, 16000 to 0.5f)),
        AutoEqProfile("AirPods Max", "Apple", mapOf(31 to -1.2f, 62 to -1.8f, 125 to -1.5f, 250 to -0.2f, 500 to 0.4f, 1000 to -0.6f, 2000 to 1.4f, 4000 to 0.8f, 8000 to -2.2f, 16000 to 0.5f)),
        AutoEqProfile("AirPods 3", "Apple", mapOf(31 to 4.5f, 62 to 3.0f, 125 to 1.2f, 250 to -0.5f, 500 to 0.2f, 1000 to -0.2f, 2000 to 0.8f, 4000 to -1.5f, 8000 to -2.0f, 16000 to 1.0f)),
        AutoEqProfile("AirPods 2", "Apple", mapOf(31 to 6.2f, 62 to 4.5f, 125 to 2.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -2.0f, 8000 to -3.0f, 16000 to 0.0f)),
        AutoEqProfile("Beats Studio Pro", "Beats", mapOf(31 to -2.0f, 62 to -3.5f, 125 to -3.0f, 250 to -1.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 1.2f, 4000 to 2.0f, 8000 to -1.8f, 16000 to 0.2f)),
        AutoEqProfile("Beats Fit Pro", "Beats", mapOf(31 to -1.5f, 62 to -2.8f, 125 to -2.2f, 250 to -0.5f, 500 to 0.2f, 1000 to 0.2f, 2000 to 0.5f, 4000 to 1.4f, 8000 to -2.0f, 16000 to 0.8f)),
        AutoEqProfile("Powerbeats Pro", "Beats", mapOf(31 to -1.0f, 62 to -2.2f, 125 to -1.8f, 250 to 0.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.0f, 8000 to -2.5f, 16000 to 0.0f)),

        // Sony
        AutoEqProfile("WH-1000XM5", "Sony", mapOf(31 to -2.8f, 62 to -4.5f, 125 to -4.8f, 250 to -2.0f, 500 to 0.5f, 1000 to -1.0f, 2000 to 1.8f, 4000 to 3.2f, 8000 to -2.5f, 16000 to -1.0f)),
        AutoEqProfile("WH-1000XM4", "Sony", mapOf(31 to -1.5f, 62 to -4.2f, 125 to -5.6f, 250 to -2.8f, 500 to 0.2f, 1000 to -0.5f, 2000 to 2.2f, 4000 to 1.5f, 8000 to -1.8f, 16000 to 0.0f)),
        AutoEqProfile("WH-1000XM3", "Sony", mapOf(31 to -1.0f, 62 to -3.8f, 125 to -5.0f, 250 to -2.5f, 500 to 0.0f, 1000 to -0.8f, 2000 to 2.0f, 4000 to 2.0f, 8000 to -2.2f, 16000 to -0.5f)),
        AutoEqProfile("WF-1000XM5", "Sony", mapOf(31 to 0.2f, 62 to -1.0f, 125 to -1.8f, 250 to -0.8f, 500 to 0.2f, 1000 to 0.5f, 2000 to 1.2f, 4000 to 2.2f, 8000 to -2.8f, 16000 to -0.8f)),
        AutoEqProfile("WF-1000XM4", "Sony", mapOf(31 to 0.5f, 62 to -1.2f, 125 to -2.0f, 250 to -1.0f, 500 to 0.0f, 1000 to 0.8f, 2000 to 1.5f, 4000 to 2.8f, 8000 to -3.0f, 16000 to -1.2f)),
        AutoEqProfile("LinkBuds S", "Sony", mapOf(31 to -0.5f, 62 to -1.8f, 125 to -1.5f, 250 to -0.2f, 500 to 0.4f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.6f, 8000 to -2.0f, 16000 to 0.0f)),
        AutoEqProfile("WH-CH720N", "Sony", mapOf(31 to -2.0f, 62 to -3.5f, 125 to -3.8f, 250 to -1.5f, 500 to 0.2f, 1000 to -0.5f, 2000 to 1.5f, 4000 to 2.5f, 8000 to -2.0f, 16000 to -0.5f)),
        AutoEqProfile("MDR-7506", "Sony", mapOf(31 to 2.5f, 62 to 1.2f, 125 to -0.5f, 250 to -0.8f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -2.8f, 8000 to -4.5f, 16000 to -1.0f)),

        // Sennheiser
        AutoEqProfile("HD 600", "Sennheiser", mapOf(31 to 5.5f, 62 to 3.8f, 125 to 1.2f, 250 to -0.2f, 500 to 0.0f, 1000 to -0.5f, 2000 to -1.0f, 4000 to 1.8f, 8000 to -1.2f, 16000 to 2.0f)),
        AutoEqProfile("HD 650", "Sennheiser", mapOf(31 to 5.8f, 62 to 3.5f, 125 to 0.8f, 250 to -0.6f, 500 to 0.0f, 1000 to -0.2f, 2000 to -0.8f, 4000 to 2.0f, 8000 to -1.0f, 16000 to 2.5f)),
        AutoEqProfile("HD 6XX", "Sennheiser", mapOf(31 to 5.8f, 62 to 3.5f, 125 to 0.8f, 250 to -0.6f, 500 to 0.0f, 1000 to -0.2f, 2000 to -0.8f, 4000 to 2.0f, 8000 to -1.0f, 16000 to 2.5f)),
        AutoEqProfile("HD 660S2", "Sennheiser", mapOf(31 to 3.5f, 62 to 2.2f, 125 to 0.5f, 250 to -0.2f, 500 to 0.0f, 1000 to -0.4f, 2000 to -0.6f, 4000 to 1.5f, 8000 to -1.5f, 16000 to 1.8f)),
        AutoEqProfile("HD 560S", "Sennheiser", mapOf(31 to 2.8f, 62 to 1.5f, 125 to 0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to -0.8f, 2000 to -1.2f, 4000 to -1.8f, 8000 to -2.5f, 16000 to 1.0f)),
        AutoEqProfile("HD 599", "Sennheiser", mapOf(31 to 4.2f, 62 to 2.8f, 125 to -0.5f, 250 to -1.5f, 500 to -0.5f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.2f, 8000 to -2.2f, 16000 to 0.5f)),
        AutoEqProfile("HD 800 S", "Sennheiser", mapOf(31 to 6.5f, 62 to 4.8f, 125 to 2.2f, 250 to 0.5f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to -1.0f, 8000 to -4.5f, 16000 to 1.5f)),
        AutoEqProfile("Momentum 4", "Sennheiser", mapOf(31 to -2.2f, 62 to -4.0f, 125 to -4.2f, 250 to -1.8f, 500 to 0.6f, 1000 to 0.2f, 2000 to 1.6f, 4000 to 2.4f, 8000 to -1.5f, 16000 to 0.5f)),
        AutoEqProfile("IE 200", "Sennheiser", mapOf(31 to 1.5f, 62 to 0.8f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to -1.2f, 8000 to -2.0f, 16000 to 0.5f)),
        AutoEqProfile("IE 600", "Sennheiser", mapOf(31 to -0.5f, 62 to -1.0f, 125 to -0.5f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -0.8f, 8000 to -3.2f, 16000 to 0.0f)),

        // Bose
        AutoEqProfile("QuietComfort 45", "Bose", mapOf(31 to 1.0f, 62 to 0.5f, 125 to -0.8f, 250 to -0.5f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -2.5f, 8000 to -3.8f, 16000 to 0.0f)),
        AutoEqProfile("QuietComfort Ultra", "Bose", mapOf(31 to -0.8f, 62 to -1.5f, 125 to -1.2f, 250 to -0.2f, 500 to 0.4f, 1000 to 0.0f, 2000 to 0.8f, 4000 to -1.8f, 8000 to -2.5f, 16000 to 0.2f)),
        AutoEqProfile("QuietComfort 35 II", "Bose", mapOf(31 to 0.8f, 62 to 0.2f, 125 to -0.5f, 250 to 0.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 0.8f, 4000 to -1.5f, 8000 to -2.8f, 16000 to 0.5f)),
        AutoEqProfile("Noise Cancelling 700", "Bose", mapOf(31 to 1.5f, 62 to 0.8f, 125 to -0.2f, 250 to 0.0f, 500 to 0.4f, 1000 to 0.2f, 2000 to 0.5f, 4000 to -1.2f, 8000 to -2.2f, 16000 to 0.8f)),

        // Beyerdynamic
        AutoEqProfile("DT 770 Pro (80Ω)", "Beyerdynamic", mapOf(31 to -2.5f, 62 to -3.2f, 125 to -2.0f, 250 to 0.8f, 500 to 1.2f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -1.5f, 8000 to -5.8f, 16000 to -2.0f)),
        AutoEqProfile("DT 770 Pro (250Ω)", "Beyerdynamic", mapOf(31 to -1.8f, 62 to -2.5f, 125 to -1.5f, 250 to 0.5f, 500 to 1.0f, 1000 to 0.0f, 2000 to 0.2f, 4000 to -1.8f, 8000 to -6.2f, 16000 to -2.5f)),
        AutoEqProfile("DT 990 Pro (250Ω)", "Beyerdynamic", mapOf(31 to 2.8f, 62 to 1.0f, 125 to -1.2f, 250 to 0.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -2.2f, 8000 to -6.5f, 16000 to -3.0f)),
        AutoEqProfile("DT 880 Pro", "Beyerdynamic", mapOf(31 to 3.5f, 62 to 2.0f, 125 to 0.2f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -1.8f, 8000 to -4.5f, 16000 to -1.5f)),
        AutoEqProfile("DT 1990 Pro", "Beyerdynamic", mapOf(31 to 2.2f, 62 to 0.8f, 125 to -0.5f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -2.5f, 8000 to -5.5f, 16000 to -2.0f)),
        AutoEqProfile("DT 700 Pro X", "Beyerdynamic", mapOf(31 to -0.8f, 62 to -1.5f, 125 to -0.8f, 250 to 0.2f, 500 to 0.5f, 1000 to 0.0f, 2000 to 0.2f, 4000 to -1.2f, 8000 to -3.5f, 16000 to -1.0f)),
        AutoEqProfile("DT 900 Pro X", "Beyerdynamic", mapOf(31 to 1.5f, 62 to 0.5f, 125 to -0.2f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -1.5f, 8000 to -3.2f, 16000 to -0.8f)),

        // Audio-Technica
        AutoEqProfile("ATH-M50x", "Audio-Technica", mapOf(31 to -1.8f, 62 to -3.2f, 125 to -2.5f, 250 to 0.5f, 500 to 0.8f, 1000 to -0.2f, 2000 to 0.5f, 4000 to -2.8f, 8000 to -3.5f, 16000 to 1.2f)),
        AutoEqProfile("ATH-M40x", "Audio-Technica", mapOf(31 to 0.5f, 62 to -1.5f, 125 to -1.2f, 250 to 0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.8f, 4000 to -2.0f, 8000 to -2.8f, 16000 to 1.5f)),
        AutoEqProfile("ATH-R70x", "Audio-Technica", mapOf(31 to 4.2f, 62 to 2.5f, 125 to 0.5f, 250 to -0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 1.0f, 8000 to -1.8f, 16000 to 1.0f)),

        // Moondrop
        AutoEqProfile("Chu II", "Moondrop", mapOf(31 to -1.0f, 62 to -1.8f, 125 to -1.5f, 250 to 0.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 1.2f, 8000 to -2.0f, 16000 to 0.0f)),
        AutoEqProfile("Chu", "Moondrop", mapOf(31 to 1.5f, 62 to 0.8f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.8f, 4000 to 0.5f, 8000 to -1.8f, 16000 to 0.2f)),
        AutoEqProfile("Aria", "Moondrop", mapOf(31 to -0.5f, 62 to -1.2f, 125 to -0.8f, 250 to 0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 1.0f, 8000 to -1.5f, 16000 to 0.5f)),
        AutoEqProfile("Aria 2", "Moondrop", mapOf(31 to -0.8f, 62 to -1.5f, 125 to -1.0f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.8f, 8000 to -1.2f, 16000 to 0.2f)),
        AutoEqProfile("Blessing 2", "Moondrop", mapOf(31 to 2.2f, 62 to 1.5f, 125 to 0.5f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.8f, 4000 to 0.5f, 8000 to -1.0f, 16000 to 1.2f)),
        AutoEqProfile("Blessing 3", "Moondrop", mapOf(31 to 1.8f, 62 to 1.0f, 125 to 0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 0.2f, 8000 to -1.5f, 16000 to 1.0f)),
        AutoEqProfile("Variations", "Moondrop", mapOf(31 to 0.0f, 62 to -0.5f, 125 to 0.2f, 250 to 0.5f, 500 to 0.2f, 1000 to 0.0f, 2000 to -0.8f, 4000 to 0.5f, 8000 to -1.0f, 16000 to 0.8f)),
        AutoEqProfile("Space Travel", "Moondrop", mapOf(31 to 0.5f, 62 to 0.0f, 125 to -0.5f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.8f, 8000 to -2.2f, 16000 to 0.0f)),

        // Truthear, 7Hz, Tangzu & Kiwi Ears
        AutoEqProfile("Zero:RED", "Truthear", mapOf(31 to -0.2f, 62 to -0.5f, 125 to -0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.5f, 8000 to -1.0f, 16000 to 0.2f)),
        AutoEqProfile("Hexa", "Truthear", mapOf(31 to 1.5f, 62 to 0.8f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 0.8f, 8000 to -1.2f, 16000 to 0.5f)),
        AutoEqProfile("Hola", "Truthear", mapOf(31 to -0.5f, 62 to -1.0f, 125 to -0.8f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 1.0f, 8000 to -1.5f, 16000 to 0.0f)),
        AutoEqProfile("Nova", "Truthear", mapOf(31 to 0.2f, 62 to -0.2f, 125 to 0.0f, 250 to 0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 0.5f, 8000 to -1.0f, 16000 to 0.5f)),
        AutoEqProfile("Zero 2", "7Hz", mapOf(31 to -0.5f, 62 to -1.0f, 125 to -0.8f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.8f, 8000 to -1.5f, 16000 to 0.2f)),
        AutoEqProfile("Timeless", "7Hz", mapOf(31 to -1.2f, 62 to -2.0f, 125 to -1.5f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -1.2f, 8000 to -3.0f, 16000 to 0.5f)),
        AutoEqProfile("Salnotes Zero", "7Hz", mapOf(31 to 1.2f, 62 to 0.8f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.5f, 4000 to 0.5f, 8000 to -1.2f, 16000 to 0.8f)),
        AutoEqProfile("Wan'er S.G", "Tangzu", mapOf(31 to -0.8f, 62 to -1.5f, 125 to -1.0f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to -0.6f, 4000 to 0.8f, 8000 to -1.8f, 16000 to 0.2f)),
        AutoEqProfile("Cadenza", "Kiwi Ears", mapOf(31 to -0.6f, 62 to -1.2f, 125 to -0.8f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.8f, 8000 to -1.5f, 16000 to 0.2f)),
        AutoEqProfile("Quintet", "Kiwi Ears", mapOf(31 to 0.0f, 62 to -0.5f, 125 to 0.0f, 250 to 0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -0.5f, 8000 to -2.5f, 16000 to 0.8f)),

        // HiFiMAN, Focal, Audeze & Planars
        AutoEqProfile("Sundara", "HiFiMAN", mapOf(31 to 3.8f, 62 to 2.2f, 125 to 0.8f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to 1.2f, 8000 to -2.2f, 16000 to 1.0f)),
        AutoEqProfile("Edition XS", "HiFiMAN", mapOf(31 to 2.2f, 62 to 1.2f, 125 to 0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.5f, 8000 to -2.8f, 16000 to 0.8f)),
        AutoEqProfile("Arya Stealth", "HiFiMAN", mapOf(31 to 2.5f, 62 to 1.5f, 125 to 0.5f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.6f, 4000 to 1.2f, 8000 to -3.0f, 16000 to 1.2f)),
        AutoEqProfile("Ananda", "HiFiMAN", mapOf(31 to 3.0f, 62 to 1.8f, 125 to 0.5f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.0f, 8000 to -2.5f, 16000 to 1.0f)),
        AutoEqProfile("HE400se", "HiFiMAN", mapOf(31 to 4.2f, 62 to 2.8f, 125 to 1.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to 1.5f, 8000 to -2.0f, 16000 to 0.5f)),
        AutoEqProfile("Clear", "Focal", mapOf(31 to 3.5f, 62 to 2.0f, 125 to 0.5f, 250 to -0.2f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.2f, 4000 to -1.0f, 8000 to -2.5f, 16000 to 1.5f)),
        AutoEqProfile("Bathys", "Focal", mapOf(31 to -1.2f, 62 to -2.0f, 125 to -1.8f, 250 to -0.5f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.2f, 8000 to -1.5f, 16000 to 0.8f)),
        AutoEqProfile("Utopia", "Focal", mapOf(31 to 2.8f, 62 to 1.5f, 125 to 0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -0.8f, 8000 to -2.0f, 16000 to 1.8f)),
        AutoEqProfile("LCD-X (2021)", "Audeze", mapOf(31 to 0.5f, 62 to 0.0f, 125 to 0.0f, 250 to 0.2f, 500 to 0.0f, 1000 to -0.5f, 2000 to 1.5f, 4000 to 2.8f, 8000 to -1.8f, 16000 to 1.0f)),
        AutoEqProfile("Maxwell", "Audeze", mapOf(31 to 0.0f, 62 to -0.2f, 125 to 0.0f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.8f, 8000 to -1.2f, 16000 to 0.5f)),

        // Samsung, AKG & Shure
        AutoEqProfile("Galaxy Buds 2 Pro", "Samsung", mapOf(31 to 0.2f, 62 to -0.5f, 125 to -0.8f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.2f, 2000 to 0.0f, 4000 to -1.2f, 8000 to -1.8f, 16000 to 0.5f)),
        AutoEqProfile("Galaxy Buds 2", "Samsung", mapOf(31 to -0.5f, 62 to -1.2f, 125 to -1.0f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -0.8f, 8000 to -2.2f, 16000 to 0.2f)),
        AutoEqProfile("Galaxy Buds FE", "Samsung", mapOf(31 to -0.8f, 62 to -1.5f, 125 to -1.2f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to -0.5f, 8000 to -2.0f, 16000 to 0.0f)),
        AutoEqProfile("K371", "AKG", mapOf(31 to 0.2f, 62 to -0.2f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -0.2f, 4000 to 0.5f, 8000 to -1.5f, 16000 to 0.5f)),
        AutoEqProfile("K361", "AKG", mapOf(31 to 0.8f, 62 to 0.2f, 125 to 0.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.8f, 8000 to -1.8f, 16000 to 0.2f)),
        AutoEqProfile("K702", "AKG", mapOf(31 to 5.2f, 62 to 3.5f, 125 to 1.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to -1.5f, 4000 to 1.0f, 8000 to -2.8f, 16000 to 1.2f)),
        AutoEqProfile("SE215", "Shure", mapOf(31 to -2.0f, 62 to -3.8f, 125 to -4.0f, 250 to -1.5f, 500 to 0.8f, 1000 to 0.5f, 2000 to 1.5f, 4000 to 2.8f, 8000 to 0.0f, 16000 to 3.5f)),
        AutoEqProfile("SRH840A", "Shure", mapOf(31 to 0.2f, 62 to -0.5f, 125 to -0.5f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.5f, 8000 to -2.0f, 16000 to 0.8f)),

        // Anker, JBL, Jabra, Nothing & Others
        AutoEqProfile("Space Q45", "Anker", mapOf(31 to -1.5f, 62 to -3.0f, 125 to -3.2f, 250 to -1.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 1.5f, 4000 to 2.2f, 8000 to -2.5f, 16000 to 0.2f)),
        AutoEqProfile("Life Q30", "Anker", mapOf(31 to -3.5f, 62 to -5.8f, 125 to -5.5f, 250 to -2.0f, 500 to 0.8f, 1000 to 0.5f, 2000 to 1.8f, 4000 to 2.5f, 8000 to -3.0f, 16000 to 0.0f)),
        AutoEqProfile("Liberty 4 NC", "Anker", mapOf(31 to -1.8f, 62 to -3.2f, 125 to -2.8f, 250 to -0.8f, 500 to 0.4f, 1000 to 0.2f, 2000 to 0.8f, 4000 to 1.8f, 8000 to -2.8f, 16000 to 0.5f)),
        AutoEqProfile("Tune 710BT", "JBL", mapOf(31 to 0.2f, 62 to -0.5f, 125 to -0.5f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.8f, 8000 to -1.8f, 16000 to 0.5f)),
        AutoEqProfile("Live 660NC", "JBL", mapOf(31 to -1.5f, 62 to -2.8f, 125 to -2.5f, 250 to -0.5f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.5f, 8000 to -2.2f, 16000 to 0.2f)),
        AutoEqProfile("Elite 85t", "Jabra", mapOf(31 to -1.0f, 62 to -2.2f, 125 to -2.0f, 250 to -0.5f, 500 to 0.4f, 1000 to 0.0f, 2000 to 0.8f, 4000 to 1.2f, 8000 to -2.5f, 16000 to 0.0f)),
        AutoEqProfile("Ear (2)", "Nothing", mapOf(31 to 0.2f, 62 to -0.5f, 125 to -0.8f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.8f, 8000 to -2.0f, 16000 to 0.5f)),
        AutoEqProfile("Ear (a)", "Nothing", mapOf(31 to -0.8f, 62 to -1.8f, 125 to -1.5f, 250 to -0.2f, 500 to 0.4f, 1000 to 0.0f, 2000 to 0.5f, 4000 to 1.2f, 8000 to -1.8f, 16000 to 0.2f)),
        AutoEqProfile("Fidelio X2HR", "Philips", mapOf(31 to 2.0f, 62 to 0.5f, 125 to -1.2f, 250 to -0.8f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.5f, 4000 to 0.8f, 8000 to -3.8f, 16000 to 0.5f)),
        AutoEqProfile("SHP9500", "Philips", mapOf(31 to 5.8f, 62 to 3.8f, 125 to 1.0f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.2f, 4000 to 0.5f, 8000 to -3.2f, 16000 to 0.8f)),
        AutoEqProfile("Porta Pro", "Koss", mapOf(31 to 1.5f, 62 to -1.2f, 125 to -3.8f, 250 to -3.0f, 500 to 0.0f, 1000 to 0.5f, 2000 to 1.8f, 4000 to 2.5f, 8000 to -1.0f, 16000 to 3.0f)),
        AutoEqProfile("KSC75", "Koss", mapOf(31 to 4.8f, 62 to 2.5f, 125 to 0.2f, 250 to 0.0f, 500 to 0.0f, 1000 to 0.0f, 2000 to 0.5f, 4000 to 1.2f, 8000 to -1.8f, 16000 to 1.5f)),
        AutoEqProfile("SR80x", "Grado", mapOf(31 to 5.5f, 62 to 3.2f, 125 to 0.5f, 250 to -0.5f, 500 to 0.0f, 1000 to 0.0f, 2000 to -1.8f, 4000 to -2.5f, 8000 to -4.0f, 16000 to 0.5f)),
        AutoEqProfile("Major IV", "Marshall", mapOf(31 to -2.5f, 62 to -4.2f, 125 to -4.5f, 250 to -1.8f, 500 to 0.5f, 1000 to 0.2f, 2000 to 1.4f, 4000 to 2.0f, 8000 to -2.8f, 16000 to -0.5f)),
        AutoEqProfile("ZSN Pro X", "KZ", mapOf(31 to -1.8f, 62 to -3.0f, 125 to -2.5f, 250 to 0.0f, 500 to 0.5f, 1000 to 0.0f, 2000 to 0.5f, 4000 to -2.0f, 8000 to -5.5f, 16000 to -2.5f)),
        AutoEqProfile("Castor (Harman)", "KZ", mapOf(31 to -0.5f, 62 to -1.2f, 125 to -0.8f, 250 to 0.0f, 500 to 0.2f, 1000 to 0.0f, 2000 to 0.0f, 4000 to 0.5f, 8000 to -2.0f, 16000 to 0.0f))
    ).sortedWith(compareBy({ it.brand }, { it.name }))

    fun calculateBandGains(profile: AutoEqProfile, centerFreqsHz: List<Int>, rangeMb: IntRange): List<Int> {
        val sortedPoints = profile.gainsDb.toList().sortedBy { it.first }
        return centerFreqsHz.map { centerHz ->
            val targetGainDb = interpolateGain(centerHz, sortedPoints)
            (targetGainDb * 100).toInt().coerceIn(rangeMb)
        }
    }

    private fun interpolateGain(freqHz: Int, points: List<Pair<Int, Float>>): Float {
        if (points.isEmpty()) return 0f
        if (freqHz <= points.first().first) return points.first().second
        if (freqHz >= points.last().first) return points.last().second

        val logTarget = log10(freqHz.toDouble())
        for (i in 0 until points.size - 1) {
            val (f1, g1) = points[i]
            val (f2, g2) = points[i + 1]
            if (freqHz in f1..f2) {
                val log1 = log10(f1.toDouble())
                val log2 = log10(f2.toDouble())
                val ratio = (logTarget - log1) / (log2 - log1)
                return (g1 + ratio * (g2 - g1)).toFloat()
            }
        }
        return points.minByOrNull { abs(it.first - freqHz) }?.second ?: 0f
    }
}
