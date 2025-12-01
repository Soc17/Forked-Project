package com.example.cmpt362group1

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cmpt362group1.event.CreateEvent
import com.example.cmpt362group1.event.Fab
import com.example.cmpt362group1.event.CreateEvent
import com.example.cmpt362group1.event.EditEvent
import com.example.cmpt362group1.navigation.BottomNavigationBar
import com.example.cmpt362group1.navigation.explore.MapStateHolder
import com.example.cmpt362group1.navigation.profile.ProfileScreen
import com.example.cmpt362group1.navigation.profile.ViewUserProfileScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmpt362group1.auth.AuthViewModel
import com.example.cmpt362group1.database.EventViewModel
import com.example.cmpt362group1.database.UserViewModel
import com.example.cmpt362group1.navigation.planner.PlannerHost
import com.example.cmpt362group1.event.detail.EventDetailScreen
import com.example.cmpt362group1.navigation.explore.swipe.SwipeEventsScreen

@Composable
fun NavigationBar(currentRoute: String, navController: NavHostController) {
    AnimatedVisibility(
        visible = currentRoute != Route.CreateEvent.route,
    ) {
        BottomNavigationBar(
            currentScreen = currentRoute,
            onTabSelected = { route ->
                if (
                    currentRoute.startsWith(Route.EventDetail.route) ||
                    currentRoute.startsWith(Route.SwipeDecider.route)
                ) {
                    navController.popBackStack()
                }
                navController.navigate(route) {
                    popUpTo(Route.Explore.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun FloatingActionButton(currentRoute: String, navController: NavHostController) {
    AnimatedVisibility(
        currentRoute == Route.Explore.route || currentRoute == Route.Planner.route
    ) {
        Fab(
            label = "Create Event",
            icon = Icons.Default.Add,
            onClick = { navController.navigate(Route.CreateEvent.route) },
        )
    }
}

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val defaultRoute: String = Route.Explore.route
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: defaultRoute

    Scaffold(
        bottomBar = {
            NavigationBar(currentRoute, navController)
        },
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            FloatingActionButton(currentRoute, navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = defaultRoute,
            ) {
                composable(Route.Explore.route) {
                    MapStateHolder(
                        onEventSelected = { id ->
                            navController.navigate("${Route.EventDetail.route}/$id")
                        },
                        onOpenSwipeDecider = {
                            navController.navigate(Route.SwipeDecider.route)
                        }
                    )
                }

                composable(Route.Planner.route) {
                    PlannerHost(
                        onEventClick = { id ->
                            navController.navigate("${Route.EventDetail.route}/$id")
                        },
                        onEditClick = { id ->
                        },
                        onCreateClick = {
                            navController.navigate(Route.CreateEvent.route)
                        }
                    )
                }


                composable(Route.Profile.route) {
                    ProfileScreen(
                        mainNavController = navController
                    )
                }

                composable("${Route.ViewUserProfile.route}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                        ?: return@composable

                    ViewUserProfileScreen(
                        userId = userId,
                        navController = navController
                    )
                }

                composable(Route.CreateEvent.route) {
                    CreateEvent(
                        onExit = {
                            navController.popBackStack()
                        },
                    )
                }

                composable("${Route.EditEvent.route}/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId")
                        ?: return@composable

                    EditEvent(
                        eventId = eventId,
                        onExit = { navController.popBackStack() },
                    )
                }

                composable(
                    route = "${Route.EventDetail.route}/{eventId}?readonly={readonly}",
                ) { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId")
                        ?: return@composable
                    val readonly = backStackEntry.arguments?.getString("readonly")?.toBoolean() ?: false

                    EventDetailScreen(
                        eventId = eventId,
                        onNavigateBack = { navController.navigateUp() },
                        onEditEvent = { id ->
                            navController.navigate("${Route.EditEvent.route}/$id")
                        },
                        allowEditDelete = !readonly,
                        navController = navController
                    )
                }

                composable(Route.SwipeDecider.route) {
                    SwipeEventsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onEventClick = { eventId ->
                            navController.navigate("${Route.EventDetail.route}/$eventId")
                        }
                    )
                }
            }
        }
    }
}
