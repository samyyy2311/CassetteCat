@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package `in`.caffeinelabs.cassettecat.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.File
import `in`.caffeinelabs.cassettecat.MainActivity
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackService

class CassetteWidgetProvider : AppWidgetProvider() {

    // Android asks for a redraw when a widget is added, after a reboot or when the launcher restarts. The player only
    // reports changes, so the widget shows the song it last reported instead of going blank.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = current ?: savedState(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, state.title, state.artist, state.isPlaying, state.art)
        }
        // A running player redraws with the current cover, which is only looked up while widgets exist.
        onWidgetsAdded?.invoke()
    }

    companion object {
        private class WidgetState(val title: String?, val artist: String?, val isPlaying: Boolean, val art: Bitmap?)

        // Matches the player while the app is running; after the app has been closed the saved song shows as paused.
        @Volatile private var current: WidgetState? = null
        private var appliedGeneration = 0L

        // Set by the playback service while it runs.
        @Volatile var onWidgetsAdded: (() -> Unit)? = null

        private const val PREFS = "cassette_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"

        private fun artFile(context: Context) = File(context.noBackupFilesDir, "widget_art.png")

        private fun savedState(context: Context): WidgetState {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val art = artFile(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            return WidgetState(prefs.getString(KEY_TITLE, null), prefs.getString(KEY_ARTIST, null), false, art)
        }

        private fun save(context: Context, state: WidgetState) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_TITLE, state.title)
                putString(KEY_ARTIST, state.artist)
            }
            val file = artFile(context)
            if (state.art != null && !state.art.isRecycled) {
                file.outputStream().use { state.art.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } else {
                file.delete()
            }
        }

        fun hasActiveWidgets(context: Context): Boolean = runCatching {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@runCatching false
            val componentName = ComponentName(context, CassetteWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds != null && appWidgetIds.isNotEmpty()
        }.getOrDefault(false)

        fun updateAllWidgets(
            context: Context,
            generation: Long,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artBitmap: Bitmap?
        ) {
            // Cover lookups can finish out of order; an older song must not replace a newer one.
            synchronized(this) {
                if (generation < appliedGeneration) return
                appliedGeneration = generation
                show(context, title, artist, isPlaying, artBitmap)
            }
        }

        private fun show(context: Context, title: String?, artist: String?, isPlaying: Boolean, artBitmap: Bitmap?) {
            runCatching {
                val state = WidgetState(title, artist, isPlaying, artBitmap)
                current = state
                save(context, state)
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, CassetteWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, title, artist, isPlaying, artBitmap)
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artBitmap: Bitmap?
        ) {
            runCatching {
                val views = RemoteViews(context.packageName, R.layout.widget_cassette)

                views.setTextViewText(R.id.widget_title, title ?: context.getString(R.string.widget_not_playing))
                views.setTextViewText(R.id.widget_artist, artist ?: context.getString(R.string.app_name))

                if (artBitmap != null && !artBitmap.isRecycled) {
                    val safeBitmap = if (artBitmap.width > 128 || artBitmap.height > 128) {
                        artBitmap.scale(120, 120)
                    } else {
                        artBitmap
                    }
                    views.setImageViewBitmap(R.id.widget_album_art, roundedBitmap(safeBitmap))
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.cat_black_cassette)
                }

                views.setImageViewResource(
                    R.id.widget_btn_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                )

                val openAppIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)

                val playPauseIntent = PendingIntent.getService(
                    context,
                    1,
                    Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_WIDGET_PLAY_PAUSE
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPauseIntent)

                val nextIntent = PendingIntent.getService(
                    context,
                    2,
                    Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_WIDGET_NEXT
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextIntent)

                val prevIntent = PendingIntent.getService(
                    context,
                    3,
                    Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_WIDGET_PREV
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun roundedBitmap(source: Bitmap): Bitmap {
            // Matches the 6dp corner on the 76dp art view.
            val cornerRadius = source.width * 0.08f
            val output = createBitmap(source.width, source.height)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val rect = android.graphics.RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(source, 0f, 0f, paint)
            return output
        }
    }
}
