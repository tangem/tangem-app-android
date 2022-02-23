package com.tangem.tap.features.shop.redux

import android.content.Intent
import com.tangem.tap.common.shop.GooglePayService
import com.tangem.tap.common.shop.data.ProductType
import com.tangem.tap.common.shop.data.TangemProduct
import org.rekotlin.Action

sealed class ShopAction : Action {

    object LoadProducts : ShopAction() {
        data class Success(val products: List<TangemProduct>) : ShopAction()
    }

    data class ApplyPromoCode(val promoCode: String) : ShopAction() {
        data class Success(val promoCode: String?, val products: List<TangemProduct>) : ShopAction()
        object InvalidPromoCode : ShopAction()
    }

    object BuyWithGooglePay : ShopAction() {
        object UserCancelled : ShopAction()
        data class HandleGooglePayResponse(val resultCode: Int, val data: Intent?) : ShopAction()

        data class Failure(val exception: Throwable) : ShopAction()
        object Success : ShopAction()
    }

    object StartWebCheckout : ShopAction()

    data class CheckIfGooglePayAvailable(val googlePayService: GooglePayService) : ShopAction() {
        object Success : ShopAction()
        object Failure : ShopAction()
    }

    data class SelectProduct(val productType: ProductType) : ShopAction()

    object ResetState : ShopAction()
}