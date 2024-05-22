package com.capztone.admin.models

data class OrderDetails(
    val foodNames: List<String> = emptyList(),
    val foodQuantities: List<Int> = emptyList(),
    val foodPrices: List<Any?> = emptyList(),
    val itemPushKey: String = "",
    val userUid: String = "",
    val address: String = "",
    val phone: String = "",
    val userName: String = "",
    var cancellationMessage: String ="",
    val shopNames: List<String> = emptyList(),
)
