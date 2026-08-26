package `in`.caffeinelabs.cassettecat.ui.util

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.R

private fun hasBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

// Audio already plays to whatever device the system has active regardless of this permission,
// this only affects whether we can show its real name instead of a generic label.
@Composable
internal fun rememberConnectedBluetoothDevice(): AudioDeviceInfo? {
    val context = LocalContext.current
    var device by remember { mutableStateOf<AudioDeviceInfo?>(null) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fun refresh() {
            device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
                }
        }
        refresh()
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    return device
}

fun isCarAudioDevice(device: AudioDeviceInfo?, context: Context): Boolean {
    if (device == null) return false

    // 1. Direct hardware car audio output device type (Android Automotive audio bus)
    if (device.type == AudioDeviceInfo.TYPE_BUS) return true

    val rawName = device.productName?.toString()?.trim() ?: ""
    val name = rawName.lowercase()

    // 2. Filter out explicit personal audio devices (headphones, earbuds, speakers)
    val personalAudioKeywords = listOf(
        "airpod", "earbud", "earphone", "headphone", "headset", "buds",
        "wh-1000", "wf-1000", "quietcomfort", "soundcore", "freebud",
        "linkbud", "galaxy buds", "pixel buds", "ear (", "ear 1", "ear 2", "ear (a)", "openfit"
    )
    if (personalAudioKeywords.any { name.contains(it) }) {
        return false
    }

    // 3. Check for car audio / infotainment / automotive keywords
    val carKeywords = listOf(
        "car", "auto", "carkit", "car kit", "sync", "uconnect", "infotainment",
        "handsfree", "hands-free", "mmi", "entune", "carplay", "android auto",
        "toyota", "honda", "ford", "bmw", "audi", "hyundai", "kia",
        "mazda", "subaru", "nissan", "mercedes", "volkswagen", "vw",
        "chevrolet", "chevy", "volvo", "skoda", "renault", "peugeot",
        "pioneer", "kenwood", "alpine", "jvc", "clarion", "blaupunkt", "headunit"
    )
    if (carKeywords.any { keyword ->
        name.contains(keyword) || name.split(" ", "-", "_", "/").contains(keyword)
    }) {
        return true
    }

    // 4. Inspect BluetoothDevice Class if BLUETOOTH_CONNECT permission is granted
    if (hasBluetoothConnectPermission(context)) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            val bondedDevices = adapter?.bondedDevices
            val matchingDevice = bondedDevices?.firstOrNull {
                it.name?.equals(rawName, ignoreCase = true) == true
            }
            if (matchingDevice != null) {
                val btClass = matchingDevice.bluetoothClass
                if (btClass != null) {
                    val deviceClass = btClass.deviceClass
                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                        deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
                    ) {
                        return true
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission missing at runtime
        } catch (_: Exception) {
            // Fallback safely
        }
    }

    return false
}

@Composable
fun BluetoothOutputLabel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val device = rememberConnectedBluetoothDevice() ?: return
    var hasPermission by remember { mutableStateOf(hasBluetoothConnectPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.tapScale {
            if (!hasPermission) launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_bluetooth),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Text(
            if (hasPermission) device.productName.toString() else "Bluetooth device",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
