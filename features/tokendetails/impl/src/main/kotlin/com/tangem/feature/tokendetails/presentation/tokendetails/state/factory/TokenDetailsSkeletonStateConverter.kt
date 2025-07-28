package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.getOrElse
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.GetStakingIntegrationIdUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isBitcoin
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

internal class TokenDetailsSkeletonStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val getStakingIntegrationIdUseCase: GetStakingIntegrationIdUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletId: UserWalletId,
) : Converter<CryptoCurrency, TokenDetailsState> {

    private val iconStateConverter by lazy { TokenDetailsIconStateConverter() }

    override fun convert(value: CryptoCurrency): TokenDetailsState {
        val iconState = iconStateConverter.convert(value)
        val isSupportedInMobileApp = getStakingIntegrationIdUseCase(value.id).isNullOrBlank().not()

        return TokenDetailsState(
            topAppBarConfig = TokenDetailsTopAppBarConfig(
                onBackClick = clickIntents::onBackClick,
                tokenDetailsAppBarMenuConfig = createMenu(value),
            ),
            tokenInfoBlockState = TokenInfoBlockState(
                name = value.name,
                iconState = iconState,
                currency = when (value) {
                    is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                    is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                        standardName = value.network.standardType.getSpecifiedNameOrNull(),
                        networkName = value.network.name,
                        networkIcon = value.networkIconResId,
                    )
                },
            ),
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Loading(
                actionButtons = createButtons(),
                balanceSegmentedButtonConfig = createBalanceSegmentedButtonConfig(),
                selectedBalanceType = BalanceType.ALL,
            ),
            marketPriceBlockState = MarketPriceBlockState.Loading(value.symbol),
            stakingBlocksState = StakingBlockUM.Loading(iconState).takeIf { isSupportedInMobileApp },
            notifications = persistentListOf(),
            pendingTxs = persistentListOf(),
            expressTxs = persistentListOf(),
            expressTxsToDisplay = persistentListOf(),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
            dialogConfig = null,
            pullToRefreshConfig = createPullToRefresh(),
            bottomSheetConfig = null,
            isBalanceHidden = true,
            isMarketPriceAvailable = value.id.rawCurrencyId != null,
        )
    }

    private fun Network.StandardType.getSpecifiedNameOrNull(): String? =
        name.takeIf { this !is Network.StandardType.Unspecified }

    private fun createMenu(cryptoCurrency: CryptoCurrency): TokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
        items = buildList {
            addGenerateXPubMenuItem(cryptoCurrency)
            TangemDropdownMenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = clickIntents::onHideClick,
            ).let(::add)
        }.toImmutableList(),
    )

    private fun MutableList<TangemDropdownMenuItem>.addGenerateXPubMenuItem(cryptoCurrency: CryptoCurrency) {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return

        val isBitcoin = isBitcoin(cryptoCurrency.network.rawId)
        val hasDerivations = networkHasDerivationUseCase(
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).getOrElse { false }

        if (isBitcoin && hasDerivations) {
            add(
                TangemDropdownMenuItem(
                    title = resourceReference(R.string.token_details_generate_xpub),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = clickIntents::onGenerateExtendedKey,
                ),
            )
        }
    }

    private fun createButtons(): ImmutableList<TokenDetailsActionButton> {
        return persistentListOf(
            TokenDetailsActionButton.Buy(dimContent = false, onClick = {}),
            TokenDetailsActionButton.Send(dimContent = false, onClick = {}),
            TokenDetailsActionButton.Receive(onClick = {}, onLongClick = null),
            TokenDetailsActionButton.Sell(dimContent = false, onClick = {}),
            TokenDetailsActionButton.Swap(dimContent = false, onClick = {}, showBadge = false),
        )
    }

    private fun createBalanceSegmentedButtonConfig(): ImmutableList<TokenBalanceSegmentedButtonConfig> {
        return persistentListOf(
            TokenBalanceSegmentedButtonConfig(
                title = resourceReference(R.string.common_all),
                type = BalanceType.ALL,
            ),
            TokenBalanceSegmentedButtonConfig(
                title = resourceReference(R.string.staking_details_available),
                type = BalanceType.AVAILABLE,
            ),
        )
    }

    private fun createPullToRefresh(): PullToRefreshConfig = PullToRefreshConfig(
        isRefreshing = false,
        onRefresh = { clickIntents.onRefreshSwipe(it.value) },
    )
}