package com.tangem.tap.features.shop.redux

import org.rekotlin.Action

object ShopReducer {
    fun reduce(action: Action, state: ShopState): ShopState = internalReduce(action, state)
}

@Suppress("ComplexMethod")
private fun internalReduce(action: Action, state: ShopState): ShopState {
    if (action !is ShopAction) return state

    return when (action) {
        is ShopAction.ApplyPromoCode -> state.copy(
            promoCode = action.promoCode,
            promoCodeLoading = true,
        )
        ShopAction.BuyWithGooglePay -> state
        ShopAction.LoadProducts -> state
        is ShopAction.LoadProducts.Success -> {
            state.copy(
                availableProducts = action.products,
            )
        }
        ShopAction.StartWebCheckout -> state
        ShopAction.ApplyPromoCode.InvalidPromoCode -> state.copy(
            promoCode = null,
            promoCodeLoading = false,
        )
        is ShopAction.ApplyPromoCode.Success -> {
            state.copy(
                promoCode = action.promoCode,
                availableProducts = action.products,
                promoCodeLoading = false,

            )
        }
        is ShopAction.SelectProduct -> {
            state.copy(
                selectedProduct = action.productType,
            )
        }
        is ShopAction.CheckIfGooglePayAvailable -> {
            state
        }
        ShopAction.CheckIfGooglePayAvailable.Failure -> {
            state.copy(isGooglePayAvailable = false)
        }
        ShopAction.CheckIfGooglePayAvailable.Success -> {
            state.copy(isGooglePayAvailable = false) // TODO: change when we add support for GPay
        }
        is ShopAction.BuyWithGooglePay.Failure -> {
            state
        }
        is ShopAction.BuyWithGooglePay.HandleGooglePayResponse -> {
            state
        }
        ShopAction.BuyWithGooglePay.Success -> {
            state
        }
        ShopAction.BuyWithGooglePay.UserCancelled -> {
            state
        }
        ShopAction.FinishSuccessfulOrder -> state
        ShopAction.ResetState -> ShopState()
        ShopAction.LoadProducts.Failure -> state
    }
}
