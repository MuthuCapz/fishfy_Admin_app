package com.capztone.admin.models

data class OrderDetails(
    val foodNames: List<String> = emptyList(),
    val skuUnitQuantities: List<String> = emptyList(),
    val foodPrices: List<Any?> = emptyList(),
    val itemPushKey: String = "",
    val userUid: String = "",
    val address: String = "",
    var selectedSlot: String = "",
    var orderDate: String = "",
    val phone: String = "",
    val userName: String = "",
    var cancellationMessage: String ="",
    val shopNames: List<String> = emptyList(),
    val foodImage: ArrayList<String>? = null,
    val skuList: List<String> = emptyList(),


)