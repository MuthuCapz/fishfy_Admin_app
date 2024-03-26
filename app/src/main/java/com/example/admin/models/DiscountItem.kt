package com.example.admin.models

data class DiscountItem(
    val foodNames:String? = null,
    val key:String? = null,
    val foodPrices:String? = null,
    val foodImages:String? = null,
    val categorys:String? = null,
    var stocks: String? = null,
    val quantitys:String? = null,
    val foodDescriptions:String?=null,
    val discounts:String? = null,
)
