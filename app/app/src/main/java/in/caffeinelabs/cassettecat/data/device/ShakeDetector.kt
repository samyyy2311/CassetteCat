package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isListening = false
    private var isCoveredInPocket = false
    private var lastUncoveredTimestamp = 0L
    private var lastShakeTimestamp = 0L

    private var reversalCount = 0
    private var lastDominantAxisSign = 0
    private var firstPulseTimestamp = 0L
    private var lastPulseTimestamp = 0L

    // Low-pass filtered gravity estimation for gravity-isolated dynamic acceleration
    private val gravity = FloatArray(3) { 0f }
    private var isGravityInitialized = false

    private var currentLinearThresholdG = 0.90f

    companion object {
        private const val GRAVITY_ALPHA = 0.80f
        private const val POCKET_UNCOVER_GRACE_MS = 400L
        private const val SHAKE_SLIDING_WINDOW_MS = 600L
        private const val MIN_DIRECTION_INTERVAL_MS = 60L
        private const val MAX_DIRECTION_INTERVAL_MS = 400L
        private const val REQUIRED_REVERSALS = 1
        private const val SHAKE_COOLDOWN_MS = 1000L
    }

    fun setSensitivity(level: Int) {
        currentLinearThresholdG = when (level.coerceIn(1, 5)) {
            1 -> 0.70f
            2 -> 0.90f
            3 -> 1.15f
            4 -> 1.45f
            else -> 1.80f
        }
    }

    fun start() {
        if (isListening || accelerometer == null) return
        resetTracking()
        isGravityInitialized = false
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        isCoveredInPocket = false
        resetTracking()
    }

    private fun resetTracking() {
        reversalCount = 0
        lastDominantAxisSign = 0
        firstPulseTimestamp = 0L
        lastPulseTimestamp = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()

        // 1. Proximity Sensor: Ignore all movement while in pocket/bag
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values.firstOrNull() ?: Float.MAX_VALUE
            val maxRange = event.sensor.maximumRange
            val covered = distance < maxRange.coerceAtMost(5f)
            if (isCoveredInPocket && !covered) {
                lastUncoveredTimestamp = now
            }
            isCoveredInPocket = covered
            if (isCoveredInPocket) {
                resetTracking()
            }
            return
        }

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (isCoveredInPocket) return
        if (now - lastUncoveredTimestamp < POCKET_UNCOVER_GRACE_MS) return
        if (now - lastShakeTimestamp < SHAKE_COOLDOWN_MS) return

        // 2. High-pass filter to subtract steady Earth gravity and isolate pure dynamic hand motion
        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        if (!isGravityInitialized) {
            gravity[0] = rawX
            gravity[1] = rawY
            gravity[2] = rawZ
            isGravityInitialized = true
            return
        }

        gravity[0] = GRAVITY_ALPHA * gravity[0] + (1f - GRAVITY_ALPHA) * rawX
        gravity[1] = GRAVITY_ALPHA * gravity[1] + (1f - GRAVITY_ALPHA) * rawY
        gravity[2] = GRAVITY_ALPHA * gravity[2] + (1f - GRAVITY_ALPHA) * rawZ

        val dynamicX = (rawX - gravity[0]) / SensorManager.GRAVITY_EARTH
        val dynamicY = (rawY - gravity[1]) / SensorManager.GRAVITY_EARTH
        val dynamicZ = (rawZ - gravity[2]) / SensorManager.GRAVITY_EARTH
        val dynamicGForce = sqrt(dynamicX * dynamicX + dynamicY * dynamicY + dynamicZ * dynamicZ)

        // 3. Evaluate rapid intentional shake pulses requiring directional reversal
        if (dynamicGForce >= currentLinearThresholdG) {
            val absX = abs(dynamicX)
            val absY = abs(dynamicY)
            val absZ = abs(dynamicZ)
            val maxVal = maxOf(absX, absY, absZ)
            val currentSign = when {
                absX == maxVal -> if (dynamicX > 0) 1 else -1
                absY == maxVal -> if (dynamicY > 0) 2 else -2
                else -> if (dynamicZ > 0) 3 else -3
            }

            if (firstPulseTimestamp == 0L || (now - firstPulseTimestamp > SHAKE_SLIDING_WINDOW_MS)) {
                firstPulseTimestamp = now
                lastPulseTimestamp = now
                lastDominantAxisSign = currentSign
                reversalCount = 0
            } else if (lastDominantAxisSign != 0 && (lastDominantAxisSign == -currentSign || (abs(lastDominantAxisSign) == abs(currentSign) && lastDominantAxisSign * currentSign < 0))) {
                val interval = now - lastPulseTimestamp
                if (interval in MIN_DIRECTION_INTERVAL_MS..MAX_DIRECTION_INTERVAL_MS) {
                    reversalCount++
                    lastPulseTimestamp = now
                    lastDominantAxisSign = currentSign

                    if (reversalCount >= REQUIRED_REVERSALS) {
                        lastShakeTimestamp = now
                        resetTracking()
                        onShake()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
