package com.tangem.features.tangempay.components.express

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import javax.inject.Inject

internal class ExpressTransactionsComponentProvider @Inject constructor(
    private val expressTransactionsComponentFactory: ExpressTransactionsComponent.Factory,
) {

    fun create(
        appComponentContext: AppComponentContext,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency?,
    ): ExpressTransactionsComponent = if (cryptoCurrency != null) {
        expressTransactionsComponentFactory.create(
            context = appComponentContext,
            params = ExpressTransactionsComponent.Params(userWalletId = userWalletId, currency = cryptoCurrency),
        )
    } else {
        EmptyExpressTransactionsComponent(context = appComponentContext)
    }
}