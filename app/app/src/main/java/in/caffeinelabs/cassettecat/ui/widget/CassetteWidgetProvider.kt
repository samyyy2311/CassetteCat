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
import android.os.Build
import android.util.SizeF
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.media3.common.Player
import androidx.media3.session.MediaButtonReceiver
import com.composables.icons.lucide.R as LucideR
import java.io.File
import `in`.caffeinelabs.cassettecat.MainActivity
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.accentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CassetteWidgetProvider : AppWidgetProvider() {

    /** What the widget shows, as the player last reported it. */
    data class WidgetState(
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val art: Bitmap?,
        val isShuffle: Boolean = false,
        val repeatMode: Int = Player.REPEAT_MODE_OFF,
        val isFavorite: Boolean = false
    )

    // Android asks for a redraw when a widget is added, after a reboot or when the launcher restarts. The player only
    // reports changes, so the widget shows the song it last reported instead of going blank.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = current ?: savedState(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, state)
        }
        // A running player redraws with the current cover, which is only looked up while widgets exist.
        onWidgetsAdded?.invoke()
        // The app also passes the accent on whenever it changes, but a widget can be added before the app is opened.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                setAccent(context, AppPreferencesRepository(context).preferences.first().accentColor.toInt())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        // Matches the player while the app is running; after the app has been closed the saved song shows as paused.
        @Volatile private var current: WidgetState? = null
        private var appliedGeneration = 0L

        // Set by the playback service while it runs.
        @Volatile var onWidgetsAdded: (() -> Unit)? = null

        private const val PREFS = "cassette_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT = "repeat"
        private const val KEY_FAVORITE = "favorite"
        private const val KEY_ACCENT = "accent"
        private const val IDLE_ICON_COLOR = 0xFFA8A29A.toInt()

        /** Cover size in pixels, large enough for the tall layout's cover to stay sharp. */
        const val COVER_SIZE = 256

        private fun artFile(context: Context) = File(context.noBackupFilesDir, "widget_art.png")

        private fun savedState(context: Context): WidgetState {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val art = artFile(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            return WidgetState(
                title = prefs.getString(KEY_TITLE, null),
                artist = prefs.getString(KEY_ARTIST, null),
                isPlaying = false,
                art = art,
                isShuffle = prefs.getBoolean(KEY_SHUFFLE, false),
                repeatMode = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF),
                isFavorite = prefs.getBoolean(KEY_FAVORITE, false)
            )
        }

        private fun save(context: Context, state: WidgetState) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_TITLE, state.title)
                putString(KEY_ARTIST, state.artist)
                putBoolean(KEY_SHUFFLE, state.isShuffle)
                putInt(KEY_REPEAT, state.repeatMode)
                putBoolean(KEY_FAVORITE, state.isFavorite)
            }
            val file = artFile(context)
            if (state.art != null && !state.art.isRecycled) {
                file.outputStream().use { state.art.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } else {
                file.delete()
            }
        }

        /** Called by the app whenever its accent changes; the widget keeps it for after a reboot. */
        fun setAccent(context: Context, color: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_ACCENT) && prefs.getInt(KEY_ACCENT, 0) == color) return
            prefs.edit { putInt(KEY_ACCENT, color) }
            show(context, current ?: savedState(context))
        }

        fun hasActiveWidgets(context: Context): Boolean = runCatching {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@runCatching false
            val componentName = ComponentName(context, CassetteWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds != null && appWidgetIds.isNotEmpty()
        }.getOrDefault(false)

        fun updateAllWidgets(context: Context, generation: Long, state: WidgetState) {
            // Cover lookups can finish out of order; an older song must not replace a newer one.
            synchronized(this) {
                if (generation < appliedGeneration) return
                appliedGeneration = generation
                show(context, state)
            }
        }

        private fun show(context: Context, state: WidgetState) {
            runCatching {
                current = state
                save(context, state)
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, CassetteWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, state)
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            state: WidgetState
        ) {
            runCatching {
                val cover = state.art?.takeIf { !it.isRecycled }?.let(::roundedBitmap)
                fun views(layout: Int) = buildViews(context, layout, state, cover)
                // Android 12+ switches between the layouts itself as the widget is resized; earlier versions keep the row.
                val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(264f, 120f) to views(R.layout.widget_cassette),
                            SizeF(276f, 170f) to views(R.layout.widget_cassette_tall)
                        )
                    )
                } else {
                    views(R.layout.widget_cassette)
                }
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            }
        }

        private fun buildViews(context: Context, layout: Int, state: WidgetState, cover: Bitmap?): RemoteViews {
            val views = RemoteViews(context.packageName, layout)
            val accent = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .takeIf { it.contains(KEY_ACCENT) }
                ?.getInt(KEY_ACCENT, 0)

            views.setTextViewText(R.id.widget_title, state.title ?: context.getString(R.string.widget_not_playing))
            views.setTextViewText(R.id.widget_artist, state.artist ?: context.getString(R.string.app_name))

            if (cover != null) {
                views.setImageViewBitmap(R.id.widget_album_art, cover)
                views.setInt(R.id.widget_album_art, "setBackgroundResource", 0)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.cat_black_cassette)
                views.setInt(R.id.widget_album_art, "setBackgroundResource", R.drawable.bg_cassette_window)
            }

            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            if (accent != null) {
                views.setInt(R.id.widget_btn_play_pause, "setColorFilter", accent)
                views.setInt(R.id.widget_play_ring, "setColorFilter", accent)
            }
            views.setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, mediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_btn_next, actionIntent(context, WidgetActionReceiver.ACTION_NEXT))
            views.setOnClickPendingIntent(R.id.widget_btn_prev, actionIntent(context, WidgetActionReceiver.ACTION_PREVIOUS))

            if (layout == R.layout.widget_cassette_tall) {
                val activeColor = accent ?: IDLE_ICON_COLOR
                views.setImageViewResource(
                    R.id.widget_btn_shuffle,
                    if (state.isShuffle) R.drawable.ic_notification_shuffle_on else R.drawable.ic_notification_shuffle_off
                )
                views.setInt(R.id.widget_btn_shuffle, "setColorFilter", if (state.isShuffle) activeColor else IDLE_ICON_COLOR)
                views.setImageViewResource(
                    R.id.widget_btn_repeat,
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) LucideR.drawable.lucide_ic_repeat_1 else LucideR.drawable.lucide_ic_repeat
                )
                views.setInt(
                    R.id.widget_btn_repeat,
                    "setColorFilter",
                    if (state.repeatMode == Player.REPEAT_MODE_OFF) IDLE_ICON_COLOR else activeColor
                )
                views.setImageViewResource(
                    R.id.widget_btn_like,
                    if (state.isFavorite) R.drawable.ic_notification_heart_filled else R.drawable.ic_notification_heart_outline
                )
                views.setInt(R.id.widget_btn_like, "setColorFilter", if (state.isFavorite) activeColor else IDLE_ICON_COLOR)
                views.setContentDescription(
                    R.id.widget_btn_shuffle,
                    context.getString(if (state.isShuffle) R.string.notification_shuffle_on else R.string.notification_shuffle_off)
                )
                views.setContentDescription(
                    R.id.widget_btn_like,
                    context.getString(if (state.isFavorite) R.string.notification_unfavorite else R.string.notification_favorite)
                )
                views.setOnClickPendingIntent(R.id.widget_btn_shuffle, actionIntent(context, WidgetActionReceiver.ACTION_SHUFFLE))
                views.setOnClickPendingIntent(R.id.widget_btn_repeat, actionIntent(context, WidgetActionReceiver.ACTION_REPEAT))
                views.setOnClickPendingIntent(R.id.widget_btn_like, actionIntent(context, WidgetActionReceiver.ACTION_LIKE))
            }
            return views
        }

        private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play is a media button: Android lets it start the player from the background, and the player then resumes
        // the queue the app saved. Media3 only accepts play this way, so the other buttons go through the receiver.
        private fun mediaButtonIntent(context: Context, keyCode: Int): PendingIntent = PendingIntent.getBroadcast(
            context,
            keyCode,
            Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, WidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun roundedBitmap(source: Bitmap): Bitmap {
            // Matches the 6dp corner of the cover's frame.
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
