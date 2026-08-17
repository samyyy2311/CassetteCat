package `in`.caffeinelabs.cassettecat.ui.util

import android.content.Context
import android.content.Intent
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.library.Song

fun shareSongs(context: Context, songs: List<Song>) {
    if (songs.isEmpty()) return

    val text = songs.joinToString("\n") { "${it.title} — ${it.artist}" }
    val intent = if (songs.all { it.source == MusicSource.Local }) {
        if (songs.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, songs.first().contentUri)
                putExtra(Intent.EXTRA_TEXT, text)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(songs.map { it.contentUri }))
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
    context.startActivity(Intent.createChooser(intent, null))
}
