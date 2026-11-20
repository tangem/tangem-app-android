package com.tangem.features.earn.api

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface EarnBannerComponent : ComposableContentComponent {

    /**
     * The Yield sheet opened from the banner. Call it outside a lazy list: the modal host does not pick up content
     * updates of a sheet composed inside a lazy item, so the sheet could stay empty.
     */
    @Composable
    fun BottomSheet()

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
    )

    interface Factory : ComponentFactory<Params, EarnBannerComponent>
}