package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer
import com.tangem.core.res.R as CoreResR

internal class SetTopBarTitleTransformer(
    private val cryptoCurrency: CryptoCurrency,
    private val hasMultipleWallets: Boolean,
    private val hasMultipleAccounts: Boolean,
    private val walletName: String,
    private val deviceIconUM: DeviceIconUM,
    private val account: Account.CryptoPortfolio?,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM = prevState.copy(
        topAppBarUM = prevState.topAppBarUM.copy(
            titleState = createTitleState(),
            subtitle = createSubtitle(),
        ),
    )

    private fun createTitleState(): TitleState {
        val tokenName = cryptoCurrency.name

        return when {
            hasMultipleAccounts && account != null -> {
                val accountNameUM = account.accountName.toUM()
                TitleState.WithAccount(
                    tokenName = tokenName,
                    accountName = accountNameUM.value,
                    accountIconUM = AccountIconUM.CryptoPortfolio(
                        value = account.icon.value,
                        color = account.icon.color,
                    ),
                )
            }
            hasMultipleWallets -> TitleState.WithWallet(
                tokenName = tokenName,
                walletName = walletName,
                deviceIconUM = deviceIconUM,
            )
            else -> TitleState.Simple(tokenName = tokenName)
        }
    }

    private fun createSubtitle(): TextReference {
        val networkName = cryptoCurrency.network.name
        return when (cryptoCurrency) {
            is CryptoCurrency.Token -> {
                val standardName = cryptoCurrency.network.standardType
                    .takeIf { it !is Network.StandardType.Unspecified }
                    ?.name
                if (standardName != null) {
                    resourceReference(
                        CoreResR.string.token_details_toolbar_subtitle_standard,
                        wrappedList(standardName, networkName),
                    )
                } else {
                    resourceReference(CoreResR.string.token_details_toolbar_subtitle_network, wrappedList(networkName))
                }
            }
            is CryptoCurrency.Coin -> {
                resourceReference(CoreResR.string.token_details_toolbar_subtitle_network, wrappedList(networkName))
            }
        }
    }
}