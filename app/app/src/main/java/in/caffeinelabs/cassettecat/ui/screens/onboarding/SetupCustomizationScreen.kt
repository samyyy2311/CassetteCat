package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.backup.BackupRepository
import `in`.caffeinelabs.cassettecat.data.settings.AppFontFamily
import `in`.caffeinelabs.cassettecat.data.settings.DefaultStartScreen
import `in`.caffeinelabs.cassettecat.data.settings.NowPlayingBackdropStyle
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import `in`.caffeinelabs.cassettecat.ui.screens.settings.AccentSwatchItem
import `in`.caffeinelabs.cassettecat.ui.screens.settings.BackdropStylePreviewDot
import `in`.caffeinelabs.cassettecat.ui.screens.settings.NavigationRow
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsDivider
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SettingsViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.settings.SheetPickerRow
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ToggleRow
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupCustomizationScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupRepository = remember { BackupRepository(context) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            isRestoring = true
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        text?.let { backupRepository.restoreBackup(it) }
                    }
                    resultMessage = if (result?.isSuccess == true) {
                        "Backup restored."
                    } else {
                        "Restore failed: the file may not be a valid CassetteCat backup."
                    }
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 2, totalSteps = 5)
        Spacer(Modifier.height(10.dp))

        Text("Make it yours", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Personalize your accent, font, and gestures.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            NavigationRow(
                title = "Restore from Backup",
                subtitle = "Already have a CassetteCat backup? Import it here.",
                iconRes = R.drawable.lucide_ic_upload,
                onClick = { restoreLauncher.launch(arrayOf("*/*")) }
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            Spacer(Modifier.height(16.dp))

            Text("Accent", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            val accents = ThemeAccent.entries.filter { it != ThemeAccent.CUSTOM }
            val accentColumns = 4
            accents.chunked(accentColumns).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { accent ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            val isSelected = prefs.themeAccent == accent
                            AccentSwatchItem(
                                color = Color(accent.colorValue),
                                label = accent.label.substringBefore(" "),
                                isSelected = isSelected,
                                iconRes = if (isSelected) R.drawable.lucide_ic_check else null,
                                iconTint = if (accent == ThemeAccent.MONOCHROME_SILVER) Color.Black else Color.White,
                                onClick = { viewModel.setThemeAccent(accent) }
                            )
                        }
                    }
                    repeat(accentColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
                if (rowIndex != accents.chunked(accentColumns).lastIndex) Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            ToggleRow(
                title = "Pure Black (AMOLED Mode)",
                subtitle = "True #000000 background for OLED battery savings and sleek contrast",
                checked = prefs.amoledDarkTheme,
                onCheckedChange = viewModel::setAmoledDarkTheme,
                iconRes = R.drawable.lucide_ic_moon,
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            SheetPickerRow(
                title = "App Typography",
                subtitle = "Font family used throughout the app",
                iconRes = R.drawable.lucide_ic_type,
                options = AppFontFamily.entries,
                selected = prefs.appFontFamily,
                label = { it.shortName },
                sheetLabel = { it.label },
                onSelect = viewModel::setAppFontFamily
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            SheetPickerRow(
                title = "Backdrop Style",
                subtitle = "Atmospheric visual theme for Now Playing",
                iconRes = R.drawable.lucide_ic_image,
                options = NowPlayingBackdropStyle.entries,
                selected = prefs.nowPlayingBackdropStyle,
                label = { it.label },
                sheetSubtitle = { it.description },
                optionLeading = { item, isSheet -> BackdropStylePreviewDot(item, size = if (isSheet) 24.dp else 12.dp) },
                onSelect = viewModel::setNowPlayingBackdropStyle
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            SheetPickerRow(
                title = "Start Screen",
                subtitle = "Choose where CassetteCat opens",
                iconRes = R.drawable.lucide_ic_house,
                options = DefaultStartScreen.entries,
                selected = prefs.defaultStartScreen,
                label = { it.label },
                onSelect = viewModel::setDefaultStartScreen
            )

            Spacer(Modifier.height(24.dp))
            Text("Sensory Gestures", style = MaterialTheme.typography.bodyMedium)
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            ToggleRow(
                title = "Shake to Skip",
                subtitle = "Quickly shake your phone to skip to the next track",
                checked = prefs.shakeToSkipEnabled,
                onCheckedChange = viewModel::setShakeToSkipEnabled,
                iconRes = R.drawable.lucide_ic_smartphone,
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            ToggleRow(
                title = "Flip to Pause",
                subtitle = "Place your phone face-down to pause music, pick up to resume",
                checked = prefs.flipToPauseEnabled,
                onCheckedChange = viewModel::setFlipToPauseEnabled,
                iconRes = R.drawable.lucide_ic_rotate_ccw,
            )
            SettingsDivider(startPadding = 0.dp, endPadding = 0.dp)
            ToggleRow(
                title = "Wave to Skip",
                subtitle = "Wave your finger over the top proximity sensor to skip to the next track",
                checked = prefs.proximityWaveSkipEnabled,
                onCheckedChange = viewModel::setProximityWaveSkipEnabled,
                iconRes = R.drawable.lucide_ic_hand,
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = hapticClick(onContinue),
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }

    val message = resultMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("Restore from Backup") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupCustomizationScreenPreview() {
    CassetteCatTheme {
        SetupCustomizationScreen(onContinue = {})
    }
}
