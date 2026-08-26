package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ProximityWaveDetector(
    context: Context,
    private val onWave: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isListening = false
    private var isCovered = false
    private var coveredTimestamp = 0L
    private var lastWaveTriggerTimestamp = 0L

    companion object {
        private const val MIN_WAVE_DURATION_MS = 60L
        private const val MAX_WAVE_DURATION_MS = 450L
        private const val WAVE_COOLDOWN_MS = 1000L
    }

    fun start() {
        if (isListening || proximitySensor == null) return
        isCovered = false
        coveredTimestamp = 0L
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        isCovered = false
        coveredTimestamp = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val now = System.currentTimeMillis()
        val distance = event.values.firstOrNull() ?: Float.MAX_VALUE
        val maxRange = event.sensor.maximumRange
        val covered = distance < maxRange.coerceAtMost(5f)

        if (covered && !isCovered) {
            isCovered = true
            coveredTimestamp = now
        } else if (!covered && isCovered) {
            isCovered = false
            val duration = now - coveredTimestamp
            if (duration in MIN_WAVE_DURATION_MS..MAX_WAVE_DURATION_MS) {
                if (now - lastWaveTriggerTimestamp >= WAVE_COOLDOWN_MS) {
                    lastWaveTriggerTimestamp = now
                    onWave()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
