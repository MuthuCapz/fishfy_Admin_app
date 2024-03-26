package com.example.admin.models

data class OrderDetails(
    val foodNames: List<String> = emptyList(),
    val foodQuantities: List<Int> = emptyList(),
    val itemPushKey: String = "",
    val userUid: String = "",
    val address: String = "",
    val phone: String = "",
    val totalPrice: String = "",
    val userName: String = "",
    var cancellationMessage:String="",

)
