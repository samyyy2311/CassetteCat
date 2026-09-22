package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory

// Cap decoded dimensions at 1440px to balance display resolution with memory usage.
internal fun decodeSampledBitmap(
    bytes: ByteArray,
    maxDimension: Int = 1440,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = config
        }
    )
}

internal fun decodeSampledBitmap(
    file: java.io.File,
    maxDimension: Int = 1440,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = config
        }
    )
}
