package com.capztone.admin.models

data class DiscountItem(

    val foodPrices:String? = null,
    val key:String? = null,
    val foodNames:String? = null,
    val foodDescriptions:String?=null,
    val quantitys:String? = null,
    val foodImages:String? = null,
    val categorys:String? = null,
    var stocks: String? = null,
    val discounts:String? = null,
    val adminId:String? = null
)
