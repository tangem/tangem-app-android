package com.tangem.tap.features.shop.redux

import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.scope
import com.tangem.tap.shopService
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

class ShopMiddleware {

    val shopMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                handle(action)
                next(action)
            }
        }
    }
}

private fun handle(action: Action) {

    val shopState = store.state.shopState

    if (action is NavigationAction.NavigateTo && action.screen == AppScreen.Shop) {
        store.dispatch(ShopAction.LoadProducts)
    }

    if (action !is ShopAction) return

    when (action) {
        is ShopAction.ApplyPromoCode -> {
            scope.launch {
                if (action.promoCode.isBlank() && shopState.promoCode == null) {
                    store.dispatchOnMain(ShopAction.ApplyPromoCode.InvalidPromoCode)
                    return@launch
                }

                val result = shopService.applyPromoCode(action.promoCode)

                result.onSuccess { products ->
                    store.dispatchOnMain(
                        ShopAction.ApplyPromoCode.Success(
                            promoCode = products.first { it.type == shopState.selectedProduct }.appliedDiscount,
                            products = products
                        )
                    )
                }
                result.onFailure { store.dispatchOnMain(ShopAction.ApplyPromoCode.InvalidPromoCode) }
            }
        }
        ShopAction.BuyWithGooglePay -> {
            shopService.buyWithGooglePay(shopState.selectedProduct)
//            shopService.subscribeToGooglePayResult(productType = shopState.selectedProduct) { result ->
//                result.onSuccess {
//                    store.dispatch(ShopAction.BuyWithGooglePay.Success)
//                }
//                result.onFailure { error ->
//                    if (error is TangemSdkError.UserCancelled) {
//                        store.dispatch(ShopAction.BuyWithGooglePay.UserCancelled)
//                    } else {
//                       store.dispatch(ShopAction.BuyWithGooglePay.Failure(error))
//                    }
//                }
//            }
        }
        is ShopAction.BuyWithGooglePay.HandleGooglePayResponse -> {
            scope.launch {
                val result = shopService.handleGooglePayResult(
                    action.resultCode,
                    action.data,
                    shopState.selectedProduct
                )
                result.onSuccess {
                    store.dispatchOnMain(ShopAction.BuyWithGooglePay.Success)
                }
                result.onFailure {
                    store.dispatchOnMain(ShopAction.BuyWithGooglePay.Failure(it))
                }
            }

        }
        ShopAction.LoadProducts -> {
            scope.launch {
                shopService.getProducts().fold(
                    onSuccess = { store.dispatchOnMain(ShopAction.LoadProducts.Success(it)) },
                    onFailure = {
                        Timber.e(it)
                        store.dispatchOnMain(ShopAction.LoadProducts.Failure)
                    }
                )
            }
        }
        is ShopAction.CheckIfGooglePayAvailable -> {
            scope.launch {
                val isAvailable =
                    shopService.checkIfGooglePayAvailable(action.googlePayService).getOrNull()
                        ?: false
                val newAction = if (isAvailable) {
                    ShopAction.CheckIfGooglePayAvailable.Success
                } else {
                    ShopAction.CheckIfGooglePayAvailable.Failure
                }
                store.dispatchOnMain(newAction)

            }
        }
        ShopAction.StartWebCheckout -> {
            store.dispatchOpenUrl(shopService.getCheckoutUrl(shopState.selectedProduct))
            store.dispatch(ShopAction.FinishSuccessfulOrder)
        }

        is ShopAction.FinishSuccessfulOrder -> {
            scope.launch {
                shopService.waitForCheckout(shopState.selectedProduct)
            }
        }
    }
}
