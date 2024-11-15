package com.capztone.admin.models


data class DeletedAccount(
    val userId: String = "",
    val contactInfo: String = "", // This will hold either email or phoneNumber
    val userUid: String = ""
)

