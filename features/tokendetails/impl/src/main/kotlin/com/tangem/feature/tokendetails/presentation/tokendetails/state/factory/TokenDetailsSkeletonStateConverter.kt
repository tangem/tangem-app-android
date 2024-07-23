package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.getOrElse
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.card.NetworkHasDerivationUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsPullToRefreshConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isBitcoin
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

internal class TokenDetailsSkeletonStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val featureToggles: TokenDetailsFeatureToggles,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletId: UserWalletId,
) : Converter<CryptoCurrency, TokenDetailsState> {

    private val iconStateConverter by lazy { TokenDetailsIconStateConverter() }

    override fun convert(value: CryptoCurrency): TokenDetailsState {
        val iconState = iconStateConverter.convert(value)
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
            stakingBlocksState = StakingBlockUM.Loading(iconState),
            notifications = persistentListOf(),
            pendingTxs = persistentListOf(),
            swapTxs = persistentListOf(),
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
            isStakingBlockShown = false,
            event = consumedEvent(),
        )
    }

    private fun Network.StandardType.getSpecifiedNameOrNull(): String? =
        name.takeIf { this !is Network.StandardType.Unspecified }

    private fun createMenu(cryptoCurrency: CryptoCurrency): TokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
        items = buildList {
            addGenerateXPubMenuItem(cryptoCurrency)
            TokenDetailsAppBarMenuConfig.MenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = clickIntents::onHideClick,
            ).let(::add)
        }.toImmutableList(),
    )

    private fun MutableList<TokenDetailsAppBarMenuConfig.MenuItem>.addGenerateXPubMenuItem(
        cryptoCurrency: CryptoCurrency,
    ) {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return
        val isGenerateXPubEnabled = featureToggles.isGenerateXPubEnabled()
        val isBitcoin = isBitcoin(cryptoCurrency.network.id.value)
        val hasDerivations =
            networkHasDerivationUseCase(userWallet.scanResponse, cryptoCurrency.network).getOrElse { false }

        if (isGenerateXPubEnabled && isBitcoin && hasDerivations) {
            add(
                TokenDetailsAppBarMenuConfig.MenuItem(
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
            TokenDetailsActionButton.Swap(dimContent = false, onClick = {}),
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

    private fun createPullToRefresh(): TokenDetailsPullToRefreshConfig = TokenDetailsPullToRefreshConfig(
        isRefreshing = false,
        onRefresh = clickIntents::onRefreshSwipe,
    )
}