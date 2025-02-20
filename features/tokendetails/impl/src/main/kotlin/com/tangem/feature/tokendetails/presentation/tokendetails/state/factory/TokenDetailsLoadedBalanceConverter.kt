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
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsTxHistoryTransactionStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.utils.getBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isBSC
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
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
    private val stakingAvailabilityProvider: Provider<StakingAvailability>,
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: TokenDetailsClickIntents,
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
        val isBalanceSelectorEnabled = !stakingCryptoAmount.isNullOrZero()
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
                isBalanceFlickering = status.value.isFlickering(),
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

    private fun getYieldBalance(status: CryptoCurrencyStatus, state: TokenDetailsState): StakingBlockUM? {
        return when (stakingAvailabilityProvider.invoke()) {
            StakingAvailability.TemporaryUnavailable -> StakingBlockUM.TemporaryUnavailable
            StakingAvailability.Unavailable -> null
            is StakingAvailability.Available -> getStakingInfoBlock(status, state)
        }
    }

    private fun getStakingInfoBlock(status: CryptoCurrencyStatus, state: TokenDetailsState): StakingBlockUM? {
        val yieldBalance = status.value.yieldBalance as? YieldBalance.Data

        val stakingCryptoAmount = yieldBalance?.getTotalStakingBalance()
        val pendingBalances = yieldBalance?.balance?.items ?: emptyList()

        val stakingEntryInfo = stakingEntryInfoProvider.invoke()
        val iconState = state.tokenInfoBlockState.iconState

        return when {
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo != null -> {
                if (pendingBalances.isEmpty()) {
                    getStakeAvailableState(stakingEntryInfo, iconState, isStakingButtonEnabled(status))
                } else {
                    getStakedBlockWithFiatAmount(status, pendingBalances.sumOf { it.amount }, null)
                }
            }
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo == null -> {
                null
            }
            else -> getStakedBlockWithFiatAmount(status, stakingCryptoAmount, yieldBalance?.getRewardStakingBalance())
        }
    }

    private fun isStakingButtonEnabled(status: CryptoCurrencyStatus): Boolean {
        return status.value is CryptoCurrencyStatus.Loaded ||
            status.value is CryptoCurrencyStatus.NoQuote ||
            status.value is CryptoCurrencyStatus.Custom
    }

    private fun getStakedBlockWithFiatAmount(
        status: CryptoCurrencyStatus,
        stakingAmount: BigDecimal?,
        rewardAmount: BigDecimal?,
    ): StakingBlockUM.Staked {
        val fiatRate = status.value.fiatRate
        return getStakedState(
            status = status,
            stakingCryptoAmount = stakingAmount,
            stakingFiatAmount = stakingAmount?.let { fiatRate?.multiply(it) },
            stakingRewardAmount = rewardAmount?.let { fiatRate?.multiply(it) },
        )
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
        isEnabled: Boolean,
    ): StakingBlockUM.StakeAvailable {
        val apr = stakingEntryInfo.apr.format { percent() }
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
            isEnabled = isEnabled,
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
                stakingCryptoAmount.format { crypto(symbol = symbol, decimals = decimals) },
            ),
            fiatValue = stringReference(
                stakingFiatAmount.format {
                    fiat(
                        appCurrencyProvider().code,
                        appCurrencyProvider().symbol,
                    )
                },
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
        val priceChange = status.priceChange ?: return DASH_SIGN

        return priceChange.format { percent() }
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return DASH_SIGN

        return fiatRate.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ).uncapped()
        }
    }

    private fun formatFiatAmount(
        status: CryptoCurrencyStatus.Value,
        stakingFiatAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
        appCurrency: AppCurrency,
    ): String {
        val fiatAmount = status.fiatAmount ?: return DASH_SIGN
        val totalAmount = fiatAmount.getBalance(selectedBalanceType, stakingFiatAmount)

        return totalAmount.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
    }

    private fun formatCryptoAmount(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
    ): String {
        val amount = status.value.amount ?: return DASH_SIGN
        val totalAmount = amount.getBalance(selectedBalanceType, stakingCryptoAmount)

        return totalAmount.format { crypto(status.currency) }
    }

    private fun getRewardText(status: CryptoCurrencyStatus, stakingRewardAmount: BigDecimal?): TextReference {
        val blockchainId = status.currency.network.id.value
        val rewardBlockType = when {
            isSolana(blockchainId) || isBSC(blockchainId) -> RewardBlockType.RewardUnavailable
            stakingRewardAmount.isNullOrZero() -> RewardBlockType.NoRewards
            else -> RewardBlockType.Rewards
        }

        return when (rewardBlockType) {
            RewardBlockType.Rewards -> resourceReference(
                R.string.staking_details_rewards_to_claim,
                wrappedList(
                    stakingRewardAmount.format {
                        fiat(
                            appCurrencyProvider().code,
                            appCurrencyProvider().symbol,
                        )
                    },
                ),
            )
            RewardBlockType.NoRewards -> resourceReference(R.string.staking_details_no_rewards_to_claim)
            RewardBlockType.RewardUnavailable -> TextReference.EMPTY
        }
    }

    private fun CryptoCurrencyStatus.Value.isFlickering(): Boolean = getStatusSource() == StatusSource.CACHE

    private fun CryptoCurrencyStatus.Value.getStatusSource(): StatusSource? {
        return when (this) {
            is CryptoCurrencyStatus.Loaded -> source
            is CryptoCurrencyStatus.NoAccount -> source
            else -> null
        }
    }
}