package com.cloudstorage.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * Standard Gojek / Material 3 style slide transitions for all screens and tabs.
 * Uses fluid easing (CubicBezier / FastOutSlowInEasing) with subtle parallax and fading.
 */
object NavigationTransitions {

    const val DURATION_SLIDE = 260
    const val DURATION_FADE = 180

    // Smooth natural easing matching Gojek / Android predictive back design
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /**
     * Standard Forward Push - Screen enters by sliding from the right edge with a subtle fade in.
     */
    fun defaultSlideEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_FADE, easing = FastOutSlowInEasing)
        )
    }

    /**
     * Standard Forward Exit - Current screen exits by sliding slightly left (parallax effect) and fading out.
     */
    fun defaultSlideExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = DURATION_FADE, easing = FastOutSlowInEasing)
        )
    }

    /**
     * Standard Back Pop Enter - Previous screen re-enters by sliding back from the left parallax position.
     */
    fun defaultSlidePopEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_FADE, easing = FastOutSlowInEasing)
        )
    }

    /**
     * Standard Back Pop Exit - Current screen exits by sliding back to the right edge with a fade out.
     */
    fun defaultSlidePopExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = DURATION_FADE, easing = FastOutSlowInEasing)
        )
    }

    // Ordered list of main bottom bar tabs for directional slide transitions
    private val tabOrder = listOf(
        "drive",
        "shared",
        "recent",
        "starred",
        "storage"
    )

    private fun getTabIndex(route: String?): Int {
        if (route == null) return -1
        return tabOrder.indexOfFirst { route.startsWith(it) }
    }

    /**
     * Smart Tab-Aware Enter Transition:
     * - If switching between tabs: slides left or right depending on relative tab index (like Gojek tabs).
     * - If entering a subpage (Search, Trash, Preview, Settings, etc.): standard slide in from right.
     */
    fun tabAwareSlideEnter(
        initialRoute: String?,
        targetRoute: String?
    ): EnterTransition {
        val initialIndex = getTabIndex(initialRoute)
        val targetIndex = getTabIndex(targetRoute)

        return if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
            if (targetIndex > initialIndex) {
                // Moving to tab on the right -> target slides in from right
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = DURATION_FADE))
            } else {
                // Moving to tab on the left -> target slides in from left
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = DURATION_FADE))
            }
        } else {
            defaultSlideEnter()
        }
    }

    /**
     * Smart Tab-Aware Exit Transition:
     * - If switching between tabs: slides out in opposite direction.
     * - If pushing a subpage: standard parallax exit to the left.
     */
    fun tabAwareSlideExit(
        initialRoute: String?,
        targetRoute: String?
    ): ExitTransition {
        val initialIndex = getTabIndex(initialRoute)
        val targetIndex = getTabIndex(targetRoute)

        return if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
            if (targetIndex > initialIndex) {
                // Moving to tab on the right -> current tab slides out to the left
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = DURATION_FADE))
            } else {
                // Moving to tab on the left -> current tab slides out to the right
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = DURATION_SLIDE, easing = StandardEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = DURATION_FADE))
            }
        } else {
            defaultSlideExit()
        }
    }
}

