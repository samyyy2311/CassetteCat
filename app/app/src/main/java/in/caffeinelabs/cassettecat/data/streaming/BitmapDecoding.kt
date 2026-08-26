package `in`.caffeinelabs.cassettecat.data.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory

// Full-width artwork and artist heroes can occupy most of a 1440p display. Keeping
// the decoded edge at 1440 avoids the soft upscaling visible with 1024px artwork,
// while still bounding memory usage for scrolling lists.
internal fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int = 1440): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    )
}
