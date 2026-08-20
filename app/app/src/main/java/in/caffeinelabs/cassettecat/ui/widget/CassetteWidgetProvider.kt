package `in`.caffeinelabs.cassettecat.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import `in`.caffeinelabs.cassettecat.MainActivity
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.playback.PlaybackService

class CassetteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, null, false, null)
        }
    }

    companion object {
        fun updateAllWidgets(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artBitmap: Bitmap?
        ) {
            runCatching {
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

                views.setTextViewText(R.id.widget_title, title ?: "Not playing")
                views.setTextViewText(R.id.widget_artist, artist ?: "CassetteCat")

                if (artBitmap != null && !artBitmap.isRecycled) {
                    val safeBitmap = if (artBitmap.width > 128 || artBitmap.height > 128) {
                        artBitmap.scale(120, 120)
                    } else {
                        artBitmap
                    }
                    views.setImageViewBitmap(R.id.widget_album_art, roundedBitmap(safeBitmap))
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.bg_cassette_window)
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
            val cornerRadius = source.width * 0.3f
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
