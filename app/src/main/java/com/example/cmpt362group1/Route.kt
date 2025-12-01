package com.example.cmpt362group1

sealed class Route(val route: String) {
    object Login : Route("login")
    object Explore : Route("explore")
    object Planner : Route("planner")
    object Profile : Route("profile")
    object CreateEvent : Route("create_event")
    object CreateEventDetail : Route("create_event_detail")
    object CreateEventLocation : Route("create_event_location")
    object EventDetail : Route("event_detail")
    object EditEvent : Route("edit_event")
    object SwipeDecider : Route("swipe_decider")
    object Loading: Route("loading")
    object ViewUserProfile : Route("view_user_profile")
}