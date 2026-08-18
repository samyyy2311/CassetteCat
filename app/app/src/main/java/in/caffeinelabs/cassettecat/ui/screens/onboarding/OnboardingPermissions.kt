package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.Manifest
import android.os.Build

object OnboardingPermissions {
    fun required(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        add(
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (sdkInt >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}
