package `in`.caffeinelabs.cassettecat.ui.navigation

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import `in`.caffeinelabs.cassettecat.data.OnboardingRepository
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.DeviceIntroScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.LibraryScanScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingScreen
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PermissionsScreen
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
    const val LIBRARY_SCAN = "onboarding/library_scan"
    const val DEVICE_INTRO = "onboarding/device_intro"
    const val PAIRING = "onboarding/pairing"
}

@Composable
fun CassetteCatNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { OnboardingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    // Read once at cold start; null means still loading. Completion is a one-way transition
    // via navigate()+popUpTo below, no need to keep observing the flow afterward.
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
        scope.launch { repository.setOnboardingCompleted(true) }
        navController.navigate(Graph.MAIN) {
            popUpTo(Graph.ONBOARDING) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (completed) Graph.MAIN else Graph.ONBOARDING,
        modifier = modifier.fillMaxSize(),
        enterTransition = mechanicalEnter,
        exitTransition = mechanicalExit,
        popEnterTransition = mechanicalPopEnter,
        popExitTransition = mechanicalPopExit
    ) {
        onboardingGraph(navController, onOnboardingFinished)
        composable(Graph.MAIN) { entry ->
            val playbackViewModel: PlaybackViewModel = viewModel(entry)
            MainShell(playbackViewModel)
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
                onContinue = { navController.navigate(OnboardingRoute.LIBRARY_SCAN) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.LIBRARY_SCAN) {
            LibraryScanScreen(
                onContinue = { navController.navigate(OnboardingRoute.DEVICE_INTRO) },
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.DEVICE_INTRO) {
            DeviceIntroScreen(
                onSetUpDevice = { navController.navigate(OnboardingRoute.PAIRING) },
                onSkip = onFinished,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(OnboardingRoute.PAIRING) {
            PairingScreen(
                onFinish = onFinished,
                isOnboarding = true,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}
