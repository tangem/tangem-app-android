package com.tangem.tap.features.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.tap.features.shop.domain.GetShopifySalesProductsUseCase
import com.tangem.tap.features.shop.domain.ShopifyOrderingAvailabilityUseCase
import com.tangem.tap.features.shop.redux.ShopAction
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shop screen view model
 *
 * @property shopifyOrderingAvailabilityUseCase use case to define shopify ordering availability
 * @property getShopifySalesProductsUseCase     use case to get actual sales info
 * @property dispatchers                        coroutine dispatchers provider
 * @property appStateHolder                     redux state holder
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class ShopViewModel @Inject constructor(
    private val shopifyOrderingAvailabilityUseCase: ShopifyOrderingAvailabilityUseCase,
    private val getShopifySalesProductsUseCase: GetShopifySalesProductsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val appStateHolder: AppStateHolder,
) : ViewModel() {

    /** Check ordering delay block visibility */
    fun checkOrderingDelayBlockVisibility() {
        viewModelScope.launch(dispatchers.main) {
            val visibility = runCatching(dispatchers.io) { shopifyOrderingAvailabilityUseCase() }
                .fold(onSuccess = { !it }, onFailure = { false })

            appStateHolder.mainStore?.dispatch(action = ShopAction.SetOrderingDelayBlockVisibility(visibility))
        }
    }

    /**
     * Get actual sales products info
     * to configure view dynamically
     */
    fun getActualSalesInfo() {
        viewModelScope.launch(dispatchers.main) {
            val action = getShopifySalesProductsUseCase().fold(
                ifLeft = {
                    ShopAction.SalesProductsError
                },
                ifRight = {
                    ShopAction.SalesProductsLoaded(it)
                },
            )
            appStateHolder.mainStore?.dispatch(action)
        }
    }
}