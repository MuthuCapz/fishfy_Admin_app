package com.capztone.admin.models

data class Order(

    val foodNames: List<String> = emptyList(),
    val foodQuantities: List<String> = emptyList(),
    val adminId: String? = null,
    val itemPushKey: String? = null,
    )
