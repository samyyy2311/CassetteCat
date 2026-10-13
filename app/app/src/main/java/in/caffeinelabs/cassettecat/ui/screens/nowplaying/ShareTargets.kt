package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class ShareTarget { WHATSAPP, INSTAGRAM_STORIES, OTHER }

private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

/** The frame every share sheet uses: a titled sheet with a close button, scrolling when the preview is tall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_x,
                    contentDescription = stringResource(AppR.string.close),
                    onClick = onDismiss
                )
            }
            content()
        }
    }
}

/** The WhatsApp, Instagram Stories and More buttons that end every share sheet. */
@Composable
internal fun ShareImageActions(onShare: (ShareTarget) -> Unit) {
    ShareActionPill(
        iconRes = AppR.drawable.ic_logo_whatsapp,
        label = stringResource(AppR.string.share_whatsapp),
        packageNames = whatsAppPackages,
        backgroundColor = Color(0xFF25D366),
        iconTint = Color.White,
        onClick = { onShare(ShareTarget.WHATSAPP) }
    )
    ShareActionPill(
        iconRes = AppR.drawable.ic_logo_instagram,
        label = stringResource(AppR.string.share_stories),
        packageNames = listOf("com.instagram.android"),
        backgroundBrush = Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF77737))),
        iconTint = Color.White,
        onClick = { onShare(ShareTarget.INSTAGRAM_STORIES) }
    )
    ShareActionPill(
        iconRes = R.drawable.lucide_ic_share_2,
        label = stringResource(AppR.string.share_more),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        iconTint = MaterialTheme.colorScheme.onSurface,
        onClick = { onShare(ShareTarget.OTHER) }
    )
}

internal suspend fun shareImage(
    context: Context,
    bitmap: Bitmap,
    title: String,
    target: ShareTarget,
    storyTheme: LyricCardTheme = LyricCardTheme.ATMOSPHERE
) {
    when (target) {
        ShareTarget.WHATSAPP -> {
            val installed = whatsAppPackages.firstOrNull { pkg ->
                runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
            }
            shareImageWithApp(context, bitmap, title, installed ?: whatsAppPackages.first())
        }
        ShareTarget.INSTAGRAM_STORIES -> shareImageToInstagramStories(context, bitmap, title, storyTheme)
        ShareTarget.OTHER -> shareImageWithApp(context, bitmap, title, null)
    }
}

@Composable
internal fun ShareActionPill(
    iconRes: Int,
    label: String,
    packageNames: List<String> = emptyList(),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    backgroundBrush: Brush? = null,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val installedAppIcon = remember(packageNames) {
        packageNames.firstNotNullOfOrNull { pkg ->
            try {
                val drawable = context.packageManager.getApplicationIcon(pkg)
                drawable.toBitmap(width = 128, height = 128).asImageBitmap()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .tapScale(onClick)
            .padding(4.dp)
    ) {
        if (installedAppIcon != null) {
            Image(
                bitmap = installedAppIcon,
                contentDescription = label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else if (backgroundBrush != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun cacheBitmapAndGetUri(context: Context, bitmap: Bitmap, filenamePrefix: String): Uri =
    withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cacheDir, "${filenamePrefix}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

internal suspend fun shareImageToInstagramStories(
    context: Context,
    bitmap: Bitmap,
    title: String,
    theme: LyricCardTheme = LyricCardTheme.ATMOSPHERE
) {
    val uri = cacheBitmapAndGetUri(context, bitmap, "story")

    context.grantUriPermission(
        "com.instagram.android",
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    val topBg = if (theme == LyricCardTheme.OBSIDIAN) "#000000" else "#1A1715"
    val bottomBg = if (theme == LyricCardTheme.OBSIDIAN) "#000000" else "#0A0908"

    val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/png"
        putExtra("interactive_asset_uri", uri)
        putExtra("top_background_color", topBg)
        putExtra("bottom_background_color", bottomBg)
        putExtra("source_application", context.packageName)
        setPackage("com.instagram.android")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val canHandleStories = runCatching {
        context.packageManager.resolveActivity(storiesIntent, 0) != null
    }.getOrDefault(false)

    if (canHandleStories) {
        val launched = runCatching {
            context.startActivity(storiesIntent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    val directStoriesIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        setClassName("com.instagram.android", "com.instagram.share.handler.ShareHandlerActivity")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val canHandleDirect = runCatching {
        context.packageManager.resolveActivity(directStoriesIntent, 0) != null
    }.getOrDefault(false)

    if (canHandleDirect) {
        val launched = runCatching {
            context.startActivity(directStoriesIntent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, title)
        setPackage("com.instagram.android")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(fallbackIntent)
    }.onFailure {
        val chooserIntent = Intent(fallbackIntent).apply { setPackage(null) }
        runCatching {
            context.startActivity(Intent.createChooser(chooserIntent, context.getString(AppR.string.share_to_stories)))
        }
    }
}

internal suspend fun shareImageWithApp(
    context: Context,
    bitmap: Bitmap,
    title: String,
    targetPackage: String?
) {
    val uri = cacheBitmapAndGetUri(context, bitmap, "share_card")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (targetPackage != null) {
            setPackage(targetPackage)
        }
    }

    runCatching {
        if (targetPackage != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, context.getString(AppR.string.share_card_title)))
        }
    }.onFailure {
        val chooserIntent = Intent(intent).apply { setPackage(null) }
        runCatching {
            context.startActivity(Intent.createChooser(chooserIntent, context.getString(AppR.string.share_card_title)))
        }
    }
}
