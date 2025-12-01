package com.example.cmpt362group1.database

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val id: String = "",

    @ServerTimestamp
    val createdAt: Date? = null,

    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val description: String = "I have a bio now!",
    val username: String = "",
    val pronouns: String = "",
    val link: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val followersList: ArrayList<String> = arrayListOf(),
    val followingList: ArrayList<String> = arrayListOf(),
    val firstName: String = "",
    val lastName: String = "",


    val eventsCreated: ArrayList<String> = arrayListOf(),
    val eventsJoined: ArrayList<String> = arrayListOf(),
)
