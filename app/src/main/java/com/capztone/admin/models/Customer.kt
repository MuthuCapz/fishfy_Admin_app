package com.capztone.admin.models

data class Customer(
    val userid: String = "",
    val phoneNumber: String? = null,
    val email: String? = null,
    val profileImage: String? = null,
    val Status:String?=null,
    var uid: String = ""
)
