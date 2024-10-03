package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsTxHistoryTransactionStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.utils.getBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class TokenDetailsLoadedBalanceConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val stakingEntryInfoProvider: Provider<StakingEntryInfo?>,
    private val pendingBalancesProvider: Provider<List<BalanceItem>>,
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: TokenDetailsClickIntents,
    private val stakingFeatureToggles: StakingFeatureToggles,
) : Converter<Either<CurrencyStatusError, CryptoCurrencyStatus>, TokenDetailsState> {

    private val txHistoryItemConverter by lazy {
        TokenDetailsTxHistoryTransactionStateConverter(symbol, decimals, clickIntents)
    }

    override fun convert(value: Either<CurrencyStatusError, CryptoCurrencyStatus>): TokenDetailsState {
        return value.fold(
            ifLeft = { convertError() },
            ifRight = { convert(it) },
        )
    }

    private fun convertError(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            isStakingBlockShown = false,
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Error(
                actionButtons = state.tokenBalanceBlockState.actionButtons,
                balanceSegmentedButtonConfig = state.tokenBalanceBlockState.balanceSegmentedButtonConfig,
                selectedBalanceType = state.tokenBalanceBlockState.selectedBalanceType,
            ),
            marketPriceBlockState = MarketPriceBlockState.Error(state.marketPriceBlockState.currencySymbol),
            notifications = persistentListOf(TokenDetailsNotification.NetworksUnreachable),
        )
    }

    private fun convert(status: CryptoCurrencyStatus): TokenDetailsState {
        val state = currentStateProvider()
        val currencyName = state.marketPriceBlockState.currencySymbol
        val pendingTxs = status.value.pendingTransactions.map(txHistoryItemConverter::convert).toPersistentList()

        return state.copy(
            tokenBalanceBlockState = getBalanceState(
                currentState = state.tokenBalanceBlockState,
                status = status,
            ),
            stakingBlocksState = getYieldBalance(status, state),
            marketPriceBlockState = getMarketPriceState(status = status.value, currencySymbol = currencyName),
            pendingTxs = pendingTxs,
            txHistoryState = if (state.txHistoryState is TxHistoryState.NotSupported) {
                state.txHistoryState.copy(pendingTransactions = pendingTxs)
            } else {
                state.txHistoryState
            },
        )
    }

    private fun getBalanceState(
        currentState: TokenDetailsBalanceBlockState,
        status: CryptoCurrencyStatus,
    ): TokenDetailsBalanceBlockState {
        val stakingCryptoAmount = (status.value.yieldBalance as? YieldBalance.Data)?.getTotalWithRewardsStakingBalance()
        val stakingFiatAmount = stakingCryptoAmount?.let { status.value.fiatRate?.multiply(it) }
        val isBalanceSelectorEnabled = stakingFeatureToggles.isStakingEnabled && !stakingCryptoAmount.isNullOrZero()
        return when (status.value) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            -> TokenDetailsBalanceBlockState.Content(
                actionButtons = currentState.actionButtons,
                cryptoBalance = status.value.amount,
                fiatBalance = status.value.fiatAmount,
                displayFiatBalance = formatFiatAmount(
                    status.value,
                    stakingFiatAmount,
                    currentState.selectedBalanceType,
                    appCurrencyProvider(),
                ),
                displayCryptoBalance = formatCryptoAmount(
                    status,
                    stakingCryptoAmount,
                    currentState.selectedBalanceType,
                ),
                balanceSegmentedButtonConfig = currentState.balanceSegmentedButtonConfig,
                onBalanceSelect = clickIntents::onBalanceSelect,
                selectedBalanceType = currentState.selectedBalanceType,
                isBalanceSelectorEnabled = isBalanceSelectorEnabled,
            )
            is CryptoCurrencyStatus.Loading -> TokenDetailsBalanceBlockState.Loading(
                actionButtons = currentState.actionButtons,
                balanceSegmentedButtonConfig = currentState.balanceSegmentedButtonConfig,
                selectedBalanceType = currentState.selectedBalanceType,
            )
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> TokenDetailsBalanceBlockState.Error(
                actionButtons = currentState.actionButtons,
                balanceSegmentedButtonConfig = currentState.balanceSegmentedButtonConfig,
                selectedBalanceType = currentState.selectedBalanceType,
            )
        }
    }

    private fun getYieldBalance(status: CryptoCurrencyStatus, state: TokenDetailsState): StakingBlockUM {
        val yieldBalance = status.value.yieldBalance as? YieldBalance.Data
        val stakingCryptoAmount = yieldBalance?.getTotalStakingBalance()

        val stakingEntryInfo = stakingEntryInfoProvider.invoke()
        val iconState = state.tokenInfoBlockState.iconState
        val pendingBalances = pendingBalancesProvider.invoke()
        val fiatRate = status.value.fiatRate

        return when {
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo != null && pendingBalances.isEmpty() -> {
                getStakeAvailableState(stakingEntryInfo, iconState)
            }
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo != null && pendingBalances.isNotEmpty() -> {
                val pendingBalancesCryptoAmount = pendingBalances.sumOf { it.amount }

                val stakingFiatAmount = fiatRate?.multiply(pendingBalancesCryptoAmount)
                getStakedState(
                    status = status,
                    stakingCryptoAmount = pendingBalancesCryptoAmount,
                    stakingFiatAmount = stakingFiatAmount,
                    stakingRewardAmount = null,
                )
            }
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo == null -> {
                StakingBlockUM.Error(iconState = iconState)
            }
            else -> {
                val stakingRewardAmount = yieldBalance?.getRewardStakingBalance()?.let { fiatRate?.multiply(it) }
                val stakingFiatAmount = stakingCryptoAmount?.let { fiatRate?.multiply(it) }

                getStakedState(
                    status = status,
                    stakingCryptoAmount = stakingCryptoAmount,
                    stakingFiatAmount = stakingFiatAmount,
                    stakingRewardAmount = stakingRewardAmount,
                )
            }
        }
    }

    private fun getMarketPriceState(
        status: CryptoCurrencyStatus.Value,
        currencySymbol: String,
    ): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencySymbol)
            is CryptoCurrencyStatus.NoQuote -> MarketPriceBlockState.Error(currencySymbol)
            is CryptoCurrencyStatus.NoAccount -> {
                if (status.fiatRate == null) {
                    MarketPriceBlockState.Error(currencySymbol)
                } else {
                    status.toContentConfig(currencySymbol)
                }
            }
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> status.toContentConfig(currencySymbol)
        }
    }

    private fun getStakeAvailableState(
        stakingEntryInfo: StakingEntryInfo,
        iconState: IconState,
    ): StakingBlockUM.StakeAvailable {
        val apr = BigDecimalFormatter.formatPercent(
            percent = stakingEntryInfo.apr,
            useAbsoluteValue = true,
        )
        return StakingBlockUM.StakeAvailable(
            titleText = resourceReference(
                id = R.string.token_details_staking_block_title,
                formatArgs = wrappedList(apr),
            ),
            subtitleText = resourceReference(
                id = R.string.staking_notification_earn_rewards_text,
                formatArgs = wrappedList(stakingEntryInfo.tokenSymbol),
            ),
            iconState = iconState,
            onStakeClicked = clickIntents::onStakeBannerClick,
        )
    }

    private fun getStakedState(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        stakingFiatAmount: BigDecimal?,
        stakingRewardAmount: BigDecimal?,
    ): StakingBlockUM.Staked {
        return StakingBlockUM.Staked(
            cryptoAmount = stakingCryptoAmount,
            fiatAmount = stakingFiatAmount,
            cryptoValue = stringReference(
                BigDecimalFormatter.formatCryptoAmount(stakingCryptoAmount, symbol, decimals),
            ),
            fiatValue = stringReference(
                BigDecimalFormatter.formatFiatAmount(
                    stakingFiatAmount,
                    appCurrencyProvider().code,
                    appCurrencyProvider().symbol,
                ),
            ),
            rewardValue = getRewardText(status, stakingRewardAmount),
            onStakeClicked = clickIntents::onStakeBannerClick,
        )
    }

    private fun CryptoCurrencyStatus.Value.toContentConfig(currencySymbol: String): MarketPriceBlockState.Content {
        return MarketPriceBlockState.Content(
            currencySymbol = currencySymbol,
            price = formatPrice(status = this, appCurrency = appCurrencyProvider()),
            priceChangeConfig = PriceChangeState.Content(
                valueInPercent = formatPriceChange(status = this),
                type = getPriceChangeType(status = this),
            ),
        )
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Value): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(status.priceChange)
    }

    private fun formatPriceChange(status: CryptoCurrencyStatus.Value): String {
        val priceChange = status.priceChange ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatPercent(
            percent = priceChange,
            useAbsoluteValue = true,
        )
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmountUncapped(
            fiatAmount = fiatRate,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatFiatAmount(
        status: CryptoCurrencyStatus.Value,
        stakingFiatAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
        appCurrency: AppCurrency,
    ): String {
        val fiatAmount = status.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN
        val totalAmount = fiatAmount.getBalance(selectedBalanceType, stakingFiatAmount)

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = totalAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatCryptoAmount(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
    ): String {
        val amount = status.value.amount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN
        val totalAmount = amount.getBalance(selectedBalanceType, stakingCryptoAmount)

        return BigDecimalFormatter.formatCryptoAmount(totalAmount, status.currency.symbol, status.currency.decimals)
    }

    private fun getRewardText(status: CryptoCurrencyStatus, stakingRewardAmount: BigDecimal?): TextReference {
        val isSolana = isSolana(status.currency.network.id.value)
        val rewardBlockType = when {
            isSolana -> RewardBlockType.RewardUnavailable
            stakingRewardAmount.isNullOrZero() -> RewardBlockType.NoRewards
            else -> RewardBlockType.Rewards
        }

        return when (rewardBlockType) {
            RewardBlockType.Rewards -> resourceReference(
                R.string.staking_details_rewards_to_claim,
                wrappedList(
                    BigDecimalFormatter.formatFiatAmount(
                        stakingRewardAmount,
                        appCurrencyProvider().code,
                        appCurrencyProvider().symbol,
                    ),
                ),
            )
            RewardBlockType.NoRewards -> resourceReference(R.string.staking_details_no_rewards_to_claim)
            RewardBlockType.RewardUnavailable -> TextReference.EMPTY
        }
    }
}