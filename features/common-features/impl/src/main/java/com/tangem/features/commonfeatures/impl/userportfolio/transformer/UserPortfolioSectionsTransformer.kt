package com.tangem.features.commonfeatures.impl.userportfolio.transformer

import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentConverter
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.impl.userportfolio.model.UserPortfolioUM

@Suppress("LongParameterList")
internal class UserPortfolioSectionsTransformer(
    private val availableData: AvailableToAddData,
    private val rawCurrencyId: CryptoCurrency.RawID,
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
        ).convert(generateEntries(availableData))

        return UserPortfolioUM(
            content = content,
            isAddEnabled = availableData.isAvailableToAdd,
            isAddedEverywhere = availableData.isAddedEverywhere,
        )
    }

    private fun generateEntries(data: AvailableToAddData): List<TokenSelectorEntry> {
        return data.availableToAddWallets.values.flatMap { wallet ->
            wallet.accounts.filterCryptoPortfolio().flatMap { accountStatus ->
                accountStatus.tokenList.flattenCurrencies()
                    .filter { status -> status.currency.matchesRawId(rawCurrencyId) }
                    .map { status ->
                        TokenSelectorEntry(
                            wallet = wallet.userWallet,
                            account = accountStatus,
                            currencyStatus = status,
                        )
                    }
            }
        }
    }

    private fun CryptoCurrency.matchesRawId(target: CryptoCurrency.RawID): Boolean {
        val rawId = id.rawCurrencyId ?: return false
        return getTokenIdIfL2Network(rawId.value) == target.value
    }
}