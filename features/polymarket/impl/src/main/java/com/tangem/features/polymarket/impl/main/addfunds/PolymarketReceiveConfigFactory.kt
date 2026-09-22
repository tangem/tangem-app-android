package com.tangem.features.polymarket.impl.main.addfunds

import com.tangem.core.res.R
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount

internal object PolymarketReceiveConfigFactory {

    fun create(
        cryptoCurrency: CryptoCurrency,
        userWalletId: UserWalletId,
        target: PolymarketDepositTarget,
    ): TokenReceiveConfig = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = cryptoCurrency,
        userWalletId = userWalletId,
        showMemoDisclaimer = false,
        receiveAddress = listOf(
            ReceiveAddressModel(
                displayType = ReceiveAddressModel.DisplayType.Default,
                value = target.address,
            ),
        ),
        tokenReceiveNotification = listOfNotNull(
            target.token.minCheckoutUsd?.let { minimum ->
                TokenReceiveNotification(
                    title = R.string.prediction_add_funds_notification_limits_title,
                    subtitle = R.string.prediction_add_funds_notification_limits_description,
                    formatArgs = listOf(minimum.formatPolymarketEnteredAmount()),
                )
            },
        ),
    )
}