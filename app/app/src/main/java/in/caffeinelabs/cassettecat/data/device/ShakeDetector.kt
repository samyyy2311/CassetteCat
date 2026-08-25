package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakeTimestamp = 0L

    private var shakePulseCount = 0
    private var firstPulseTimestamp = 0L
    private var lastPulseTimestamp = 0L

    private var currentThresholdGForce = 1.45f

    companion object {
        private const val SHAKE_SLIDING_WINDOW_MS = 650L
        private const val MIN_PULSE_INTERVAL_MS = 140L
        private const val REQUIRED_PULSES = 2
        private const val SHAKE_COOLDOWN_MS = 2000L
    }

    fun setSensitivity(level: Int) {
        currentThresholdGForce = when (level.coerceIn(1, 5)) {
            1 -> 1.30f
            2 -> 1.45f
            3 -> 1.70f
            4 -> 2.00f
            else -> 2.40f
        }
    }

    fun start() {
        if (isListening || accelerometer == null) return
        resetTracking()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        resetTracking()
    }

    private fun resetTracking() {
        shakePulseCount = 0
        firstPulseTimestamp = 0L
        lastPulseTimestamp = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastShakeTimestamp < SHAKE_COOLDOWN_MS) return

        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce >= currentThresholdGForce) {
            if (now - lastPulseTimestamp < MIN_PULSE_INTERVAL_MS) return
            lastPulseTimestamp = now

            if (firstPulseTimestamp == 0L || (now - firstPulseTimestamp > SHAKE_SLIDING_WINDOW_MS)) {
                firstPulseTimestamp = now
                shakePulseCount = 1
            } else {
                shakePulseCount++
                if (shakePulseCount >= REQUIRED_PULSES) {
                    lastShakeTimestamp = now
                    resetTracking()
                    onShake()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
