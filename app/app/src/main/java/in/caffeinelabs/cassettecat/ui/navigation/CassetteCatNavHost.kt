package `in`.caffeinelabs.cassettecat.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.data.OnboardingRepository
import `in`.caffeinelabs.cassettecat.data.update.markReleaseNotesSeen
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.LibraryScanScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.ListeningHistoryScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PermissionsScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.RestoreBackupScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.SetupCustomizationScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.WelcomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private object Graph {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
}

private object OnboardingRoute {
    const val WELCOME = "onboarding/welcome"
    const val PERMISSIONS = "onboarding/permissions"
    const val RESTORE = "onboarding/restore"
    const val LIBRARY_SCAN = "onboarding/library_scan"
    const val LISTENING_HISTORY = "onboarding/listening_history"
    const val CUSTOMIZATION = "onboarding/customization"
}

@Composable
fun CassetteCatNavHost(
    shortcutAction: String? = null,
    shortcutQuery: String? = null,
    shortcutMediaType: String? = null,
    onShortcutHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { OnboardingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Read once at cold start; null represents the loading state.
    val onboardingCompleted by produceState<Boolean?>(initialValue = null, repository) {
        value = repository.onboardingCompleted.first()
    }

    val completed = onboardingCompleted
    if (completed == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()
    val onOnboardingFinished: () -> Unit = {
        scope.launch {
            // Someone who just set the app up has nothing to catch up on.
            markReleaseNotesSeen(context, BuildConfig.VERSION_NAME)
            repository.setOnboardingCompleted(true)
            navController.navigate(Graph.MAIN) {
                popUpTo(Graph.ONBOARDING) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (completed) Graph.MAIN else Graph.ONBOARDING,
        modifier = modifier.fillMaxSize(),
        enterTransition = mechanicalEnter,
        exitTransition = mechanicalExit,
        popEnterTransition = mechanicalPopEnter,
        popExitTransition = mechanicalPopExit,
        predictivePopEnterTransition = { mechanicalPopEnter() },
        predictivePopExitTransition = { mechanicalPopExit() }
    ) {
        onboardingGraph(navController, onOnboardingFinished)
        composable(Graph.MAIN) { entry ->
            val playbackViewModel: PlaybackViewModel = viewModel(entry)
            MainShell(
                playbackViewModel,
                shortcutAction = shortcutAction,
                shortcutQuery = shortcutQuery,
                shortcutMediaType = shortcutMediaType,
                onShortcutHandled = onShortcutHandled
            )
        }
    }
}

private fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
    onFinished: () -> Unit
) {
    navigation(startDestination = OnboardingRoute.WELCOME, route = Graph.ONBOARDING) {
        composable(OnboardingRoute.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(OnboardingRoute.PERMISSIONS) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.PERMISSIONS) {
            PermissionsScreen(
                onContinue = { navController.navigate(OnboardingRoute.RESTORE) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.RESTORE) {
            RestoreBackupScreen(
                onContinue = { navController.navigate(OnboardingRoute.LIBRARY_SCAN) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.LIBRARY_SCAN) {
            val toListeningHistory: () -> Unit = {
                navController.navigate(OnboardingRoute.LISTENING_HISTORY) {
                    popUpTo(OnboardingRoute.LIBRARY_SCAN) { inclusive = true }
                }
            }
            LibraryScanScreen(
                onContinue = toListeningHistory,
                onSkip = toListeningHistory,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.LISTENING_HISTORY) {
            val context = LocalContext.current
            ListeningHistoryScreen(
                onContinue = { navController.navigate(OnboardingRoute.CUSTOMIZATION) },
                onOpenUrl = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.CUSTOMIZATION) {
            SetupCustomizationScreen(
                onFinish = onFinished,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}
