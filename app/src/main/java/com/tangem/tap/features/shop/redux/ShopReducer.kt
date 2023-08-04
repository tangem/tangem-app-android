package com.tangem.tap.features.shop.redux

import org.rekotlin.Action

object ShopReducer {
    fun reduce(action: Action, state: ShopState): ShopState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: ShopState): ShopState {
    if (action !is ShopAction) return state

    return when (action) {
        is ShopAction.ApplyPromoCode -> state.copy(promoCode = action.promoCode, promoCodeLoading = true)
        is ShopAction.LoadProducts.Success -> state.copy(availableProducts = action.products)
        is ShopAction.ApplyPromoCode.InvalidPromoCode -> state.copy(promoCode = null, promoCodeLoading = false)
        is ShopAction.ApplyPromoCode.Success -> {
            state.copy(
                promoCode = action.promoCode,
                availableProducts = action.products,
                promoCodeLoading = false,
            )
        }
        is ShopAction.SelectProduct -> state.copy(selectedProduct = action.productType)
// [REDACTED_TODO_COMMENT]
        is ShopAction.CheckIfGooglePayAvailable.Failure -> state.copy(isGooglePayAvailable = false)
        is ShopAction.CheckIfGooglePayAvailable.Success -> state.copy(isGooglePayAvailable = false)

        is ShopAction.ResetState -> ShopState()
        is ShopAction.SetOrderingDelayBlockVisibility -> state.copy(isOrderingDelayBlockVisible = action.visibility)
        is ShopAction.BuyWithGooglePay,
        is ShopAction.LoadProducts,
        is ShopAction.StartWebCheckout,
        is ShopAction.CheckIfGooglePayAvailable,
        is ShopAction.BuyWithGooglePay.Failure,
        is ShopAction.BuyWithGooglePay.HandleGooglePayResponse,
        is ShopAction.BuyWithGooglePay.Success,
        is ShopAction.BuyWithGooglePay.UserCancelled,
        is ShopAction.FinishSuccessfulOrder,
        is ShopAction.LoadProducts.Failure,
        -> state
        is ShopAction.SalesProductsLoaded -> state.copy(
            salesProducts = action.salesProducts,
        )
        is ShopAction.SalesProductsError -> state.copy(
            salesProducts = emptyList(),
        )
    }
}
