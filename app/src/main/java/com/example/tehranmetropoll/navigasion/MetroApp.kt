package com.example.tehranmetropoll.navigasion

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tehranmetropoll.ui.screen.MainScreen
import com.example.tehranmetropoll.ui.screen.MetroMap
import com.example.tehranmetropoll.ui.screen.SplashScreen
import com.example.tehranmetropoll.ui.screen.StationDetailScreen
import com.example.tehranmetropoll.viewModel.MainViewModel


@Composable
fun MetroApp(viewModel: MainViewModel) {
    val navController= rememberNavController()

    NavHost(navController = navController, startDestination = "splash"){
        
        composable("splash") { 
            SplashScreen(navController = navController)
        }
        composable("main") {
            MainScreen(
                navController=navController,
                viewModel=viewModel,
                origin = viewModel.origin,
                destination = viewModel.destination,
                onValueChangeOrigin = { viewModel.updateOrigin(it) },
                onValueChangeDestination = { viewModel.updateDestination(it) },
                onSelectOrigin = { viewModel.selectOrigin(it) },
                onSelectDestination = { viewModel.selectDestination(it) },
                originSuggestions = viewModel.originSuggestions,
                destinationSuggestions = viewModel.destinationSuggestions,
                onSearch = { viewModel.findRoute() },
                route = viewModel.route,
                onStationClick = { station -> navController.navigate("detail/${station.id}") }
            )

        }

       composable(route="detail/{stationId}",
           arguments = listOf(
               navArgument("stationId"){
                   type= NavType.IntType
               }
           )
           ){backStackEntry->
           val id=backStackEntry.arguments?.getInt("stationId") ?: return@composable

           StationDetailScreen(stationId=id) { navController.popBackStack() }


       }


        composable("map") {
            MetroMap(allStations = viewModel.allStation,routeStations=viewModel.route)

            
        }





    }

}