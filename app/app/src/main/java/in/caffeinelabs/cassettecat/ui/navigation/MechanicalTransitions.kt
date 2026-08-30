package `in`.caffeinelabs.cassettecat.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

internal const val TRANSITION_MS = 220
internal val SmoothEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

internal fun slideEnter(fromRight: Boolean): EnterTransition = slideInHorizontally(
    animationSpec = tween(TRANSITION_MS, easing = SmoothEasing),
    initialOffsetX = { if (fromRight) it / 4 else -it / 4 }
) + fadeIn(tween(TRANSITION_MS, easing = SmoothEasing))

internal fun slideExit(toRight: Boolean): ExitTransition = slideOutHorizontally(
    animationSpec = tween(TRANSITION_MS, easing = SmoothEasing),
    targetOffsetX = { if (toRight) it / 4 else -it / 4 }
) + fadeOut(tween(TRANSITION_MS, easing = SmoothEasing))

internal val mechanicalEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideEnter(fromRight = true) }

internal val mechanicalExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideExit(toRight = false) }

internal val mechanicalPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideEnter(fromRight = false) }

internal val mechanicalPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideExit(toRight = true) }

