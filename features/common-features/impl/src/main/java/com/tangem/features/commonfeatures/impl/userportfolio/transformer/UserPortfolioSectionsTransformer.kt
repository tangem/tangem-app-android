package com.tangem.features.commonfeatures.impl.userportfolio.transformer

import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentConverter
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.impl.userportfolio.model.UserPortfolioUM

@Suppress("LongParameterList")
internal class UserPortfolioSectionsTransformer(
    private val availableData: AvailableToAddData,
    private val rawCurrencyId: CryptoCurrency.RawID,
    private val networkIds: Set<String>?,
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val isAccountsModeEnabled: Boolean,
    private val resolveWalletDeviceIcon: (UserWallet) -> DeviceIconUM,
    private val onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
) {

    fun transform(): UserPortfolioUM {
        val content = TokenSelectorContentConverter(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsModeEnabled = isAccountsModeEnabled,
            resolveWalletDeviceIcon = resolveWalletDeviceIcon,
            onEntryClick = { entry ->
                onTokenSelected(
                    AddToPortfolioManager.Result(
                        wallet = entry.wallet,
                        account = entry.account,
                        addedCurrency = entry.currencyStatus,
                    ),
                )
            },
        ).convert(availableData.heldCurrencyEntries(rawCurrencyId, networkIds))

        return UserPortfolioUM(
            content = content,
            isAddEnabled = availableData.isAvailableToAdd,
            isAddedEverywhere = availableData.isAddedEverywhere,
        )
    }
}