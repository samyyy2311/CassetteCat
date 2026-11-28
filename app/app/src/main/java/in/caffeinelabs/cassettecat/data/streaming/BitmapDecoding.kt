package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

// Cap decoded dimensions at 1440px to balance display resolution with memory usage.
internal fun decodeSampledBitmap(
    bytes: ByteArray,
    maxDimension: Int = 1440,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampledOptions(bounds, maxDimension, config))
}

internal fun decodeSampledBitmap(
    file: File,
    maxDimension: Int = 1440,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    return BitmapFactory.decodeFile(file.absolutePath, sampledOptions(bounds, maxDimension, config))
}

private fun sampledOptions(bounds: BitmapFactory.Options, maxDimension: Int, config: Bitmap.Config): BitmapFactory.Options {
    var sampleSize = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
}

// A saved picture that no longer opens is removed, so it's downloaded again.
internal fun decodeSavedBitmap(file: File, maxDimension: Int): Bitmap? {
    if (!file.exists()) return null
    return decodeSampledBitmap(file, maxDimension).also { if (it == null) file.delete() }
}

// Written to a file beside it first, so a picture cut off part way is never kept.
internal fun saveWhole(file: File, bytes: ByteArray) {
    runCatching {
        file.parentFile?.mkdirs()
        val part = File.createTempFile(file.name, ".part", file.parentFile)
        part.writeBytes(bytes)
        if (!part.renameTo(file)) part.delete()
    }
}
