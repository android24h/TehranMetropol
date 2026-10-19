package com.example.tehranmetropoll.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tehranmetropoll.core.AppLink
import com.example.tehranmetropoll.domain.model.AppLanguage
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.ThemeMode
import com.example.tehranmetropoll.presentation.home.HomeScreen
import com.example.tehranmetropoll.presentation.home.HomeViewModel
import com.example.tehranmetropoll.presentation.map.RouteMapScreen
import com.example.tehranmetropoll.presentation.picker.PickerScreen
import com.example.tehranmetropoll.presentation.picker.PickerViewModel
import com.example.tehranmetropoll.presentation.ride.RideScreen
import com.example.tehranmetropoll.presentation.ride.RideViewModel
import com.example.tehranmetropoll.presentation.route.RouteScreen
import com.example.tehranmetropoll.presentation.route.RouteViewModel

object Destinations {
    const val ARG_ORIGIN = "origin"
    const val ARG_DESTINATION = "destination"
    const val ARG_PREFERENCE = "pref"

    const val HOME = "home"
    const val PICKER = "picker?$ARG_ORIGIN={$ARG_ORIGIN}&$ARG_DESTINATION={$ARG_DESTINATION}"
    const val ROUTE = "route/{$ARG_ORIGIN}/{$ARG_DESTINATION}?$ARG_PREFERENCE={$ARG_PREFERENCE}"
    const val ROUTE_MAP = "routeMap/{$ARG_ORIGIN}/{$ARG_DESTINATION}?$ARG_PREFERENCE={$ARG_PREFERENCE}"
    /** The trip currently being tracked (there is at most one). */
    const val RIDE = "ride"

    const val RESULT_ORIGIN = "picked_origin"
    const val RESULT_DESTINATION = "picked_destination"

    fun picker(origin: String?, destination: String?) =
        "picker?$ARG_ORIGIN=${origin.orEmpty()}&$ARG_DESTINATION=${destination.orEmpty()}"

    fun route(origin: String, destination: String, pref: RoutePreference = RoutePreference.FASTEST) =
        "route/$origin/$destination?$ARG_PREFERENCE=${pref.name}"

    fun routeMap(origin: String, destination: String, pref: RoutePreference) =
        "routeMap/$origin/$destination?$ARG_PREFERENCE=${pref.name}"

}

private val tripArguments = listOf(
    navArgument(Destinations.ARG_ORIGIN) { type = NavType.StringType },
    navArgument(Destinations.ARG_DESTINATION) { type = NavType.StringType },
    navArgument(Destinations.ARG_PREFERENCE) {
        type = NavType.StringType
        defaultValue = RoutePreference.FASTEST.name
    },
)

@Composable
fun MetroNavHost(
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    pendingLink: AppLink?,
    onLinkHandled: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Notifications and the widget open a screen directly, on top of Home so Back still works.
    LaunchedEffect(pendingLink) {
        val link = pendingLink ?: return@LaunchedEffect
        val target = when (link) {
            is AppLink.Route -> Destinations.route(link.originId, link.destinationId, link.preference)
            AppLink.Ride -> Destinations.RIDE
        }
        navController.navigate(target) {
            popUpTo(Destinations.HOME)
            launchSingleTop = true
        }
        onLinkHandled()
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(280)) },
    ) {
        composable(Destinations.HOME) { entry ->
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            val results = entry.savedStateHandle
            LaunchedEffect(results) {
                results.getStateFlow<String?>(Destinations.RESULT_ORIGIN, null).collect { originId ->
                    if (originId != null) {
                        val destinationId = results.get<String>(Destinations.RESULT_DESTINATION)
                        viewModel.onPickedFromMap(originId.ifEmpty { null }, destinationId?.ifEmpty { null })
                        results.remove<String>(Destinations.RESULT_ORIGIN)
                        results.remove<String>(Destinations.RESULT_DESTINATION)
                    }
                }
            }
            HomeScreen(
                viewModel = viewModel,
                themeMode = themeMode,
                language = language,
                onThemeSelected = onThemeSelected,
                onLanguageSelected = onLanguageSelected,
                onOpenRoute = { o, d, pref -> navController.navigate(Destinations.route(o, d, pref)) },
                onOpenPicker = { o, d -> navController.navigate(Destinations.picker(o, d)) },
            )
        }

        composable(
            Destinations.PICKER,
            arguments = listOf(
                navArgument(Destinations.ARG_ORIGIN) { type = NavType.StringType; defaultValue = "" },
                navArgument(Destinations.ARG_DESTINATION) { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            PickerScreen(
                viewModel = viewModel(factory = PickerViewModel.Factory),
                onBack = { navController.popBackStack() },
                onConfirm = { origin, destination ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(Destinations.RESULT_DESTINATION, destination.orEmpty())
                        // Written last: Home observes this key.
                        set(Destinations.RESULT_ORIGIN, origin.orEmpty())
                    }
                    navController.popBackStack()
                },
            )
        }

        composable(Destinations.ROUTE, arguments = tripArguments) { entry ->
            val origin = entry.arguments?.getString(Destinations.ARG_ORIGIN).orEmpty()
            val destination = entry.arguments?.getString(Destinations.ARG_DESTINATION).orEmpty()
            RouteScreen(
                viewModel = viewModel(factory = RouteViewModel.Factory),
                onBack = { navController.popBackStack() },
                onShowMap = { pref -> navController.navigate(Destinations.routeMap(origin, destination, pref)) },
                onTripStarted = {
                    navController.navigate(Destinations.RIDE) { launchSingleTop = true }
                },
            )
        }

        composable(Destinations.ROUTE_MAP, arguments = tripArguments) {
            RouteMapScreen(
                viewModel = viewModel(factory = RouteViewModel.Factory),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.RIDE) {
            RideScreen(
                viewModel = viewModel(factory = RideViewModel.Factory),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
