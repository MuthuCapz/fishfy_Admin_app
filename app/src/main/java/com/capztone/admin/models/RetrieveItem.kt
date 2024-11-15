package com.capztone.admin.models

data class RetrieveItem(
    val foodName:ArrayList<String>? = null,
    val key:String? = null,
    val foodPrice:String? = null,
    val foodImage:String? = null,
    val category:String? = null,
    var stock: String? = null,
    val quantity:String? = null,
    val foodDescription:String?=null,
    val discounts:String? = null,
    val productQuantity: String?= null,
    val adminId: String? = null
)