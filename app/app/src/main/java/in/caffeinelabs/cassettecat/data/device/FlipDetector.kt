package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

class FlipDetector(
    context: Context,
    private val onFlipDown: () -> Unit,
    private val onFlipUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isProximityRegistered = false

    @Volatile
    private var isFaceDown = false

    @Volatile
    private var isProximityNear = false
    private var hasProximitySensor = proximitySensor != null

    private var faceDownPendingRunnable: Runnable? = null

    fun start() {
        if (isListening || sensorManager == null) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isListening = true
    }

    fun stop() {
        if (!isListening || sensorManager == null) return
        cancelPendingFlipDown()
        sensorManager.unregisterListener(this)
        isListening = false
        isProximityRegistered = false
        isFaceDown = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                isProximityNear = distance < maxRange && distance < 5f
                checkOrientation(currentGz, currentGx, currentGy)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                currentGx = gx
                currentGy = gy
                currentGz = gz

                // Power optimization: only turn on proximity sensor when phone begins tilting face down
                if (hasProximitySensor && proximitySensor != null) {
                    if (gz < -4.0f && !isProximityRegistered) {
                        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
                        isProximityRegistered = true
                    } else if (gz > -1.5f && isProximityRegistered && !isFaceDown) {
                        sensorManager?.unregisterListener(this, proximitySensor)
                        isProximityRegistered = false
                        isProximityNear = false
                    }
                }

                checkOrientation(gz, gx, gy)
            }
        }
    }

    private var currentGx = 0f
    private var currentGy = 0f
    private var currentGz = 9.8f

    private fun checkOrientation(gz: Float, gx: Float, gy: Float) {
        val lateralForceSq = gx * gx + gy * gy
        val isPhysicallyFaceDown = gz < -6.5f && lateralForceSq < 40f
        val isConfirmedFaceDown = if (hasProximitySensor && isProximityRegistered) {
            isPhysicallyFaceDown && isProximityNear
        } else {
            isPhysicallyFaceDown
        }

        if (isConfirmedFaceDown) {
            if (!isFaceDown && faceDownPendingRunnable == null) {
                // Debounce: must remain face down for 350ms to avoid momentary rotation triggers
                val runnable = Runnable {
                    if (isConfirmedFaceDown && !isFaceDown) {
                        isFaceDown = true
                        onFlipDown()
                    }
                    faceDownPendingRunnable = null
                }
                faceDownPendingRunnable = runnable
                handler.postDelayed(runnable, 350L)
            }
        } else {
            cancelPendingFlipDown()
            // Face up threshold
            if (gz > 1.5f || (hasProximitySensor && isProximityRegistered && !isProximityNear && gz > -2f)) {
                if (isFaceDown) {
                    isFaceDown = false
                    onFlipUp()
                }
            }
        }
    }

    private fun cancelPendingFlipDown() {
        faceDownPendingRunnable?.let {
            handler.removeCallbacks(it)
            faceDownPendingRunnable = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
