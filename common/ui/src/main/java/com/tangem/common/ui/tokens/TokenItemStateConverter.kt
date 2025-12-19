package com.tangem.common.ui.tokens

import com.tangem.common.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.utils.getTotalWithRewardsStakingBalance
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Token item state converter from [CryptoCurrencyStatus] to [TokenItemState]
 *
 * @property appCurrency           app currency
 * @property titleStateProvider    title state provider
 * @property subtitleStateProvider subtitle state provider
 * @property onItemClick           callback is invoked when item is clicked
 * @property onItemLongClick       callback is invoked when item is long clicked
 */
class TokenItemStateConverter(
    private val appCurrency: AppCurrency,
    private val yieldModuleApyMap: Map<String, BigDecimal> = emptyMap(),
    private val stakingApyMap: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
    private val yieldSupplyPromoBannerKey: String? = null,
    private val iconStateProvider: (CryptoCurrencyStatus) -> CurrencyIconState = {
        CryptoCurrencyToIconStateConverter().convert(it)
    },
    private val onApyLabelClick: ((CryptoCurrencyStatus, ApySource, String) -> Unit)? = null,
    private val onYieldPromoCloseClick: (() -> Unit)? = null,
    private val titleStateProvider: (CryptoCurrencyStatus) -> TokenItemState.TitleState = { currencyStatus ->
        createTitleState(
            currencyStatus = currencyStatus,
            yieldModuleApyMap = yieldModuleApyMap,
            stakingApyMap = stakingApyMap,
            onApyLabelClick = onApyLabelClick,
        )
    },
    private val subtitleStateProvider: (CryptoCurrencyStatus) -> TokenItemState.SubtitleState? = {
        createSubtitleState(it, appCurrency)
    },
    private val subtitle2StateProvider: (CryptoCurrencyStatus) -> TokenItemState.Subtitle2State? = {
        createSubtitle2State(status = it)
    },
    private val fiatAmountStateProvider: (CryptoCurrencyStatus) -> TokenItemState.FiatAmountState? = {
        createFiatAmountState(status = it, appCurrency = appCurrency)
    },
    private val promoBannerProvider: (CryptoCurrencyStatus) -> TokenItemState.PromoBannerState = { status ->
        createPromoBannerState(
            status = status,
            yieldModuleApyMap = yieldModuleApyMap,
            yieldSupplyPromoBannerKey = yieldSupplyPromoBannerKey,
            onApyLabelClick = onApyLabelClick,
            onYieldPromoCloseClick = onYieldPromoCloseClick,
        )
    },
    private val onItemClick: ((TokenItemState, CryptoCurrencyStatus) -> Unit)? = null,
    private val onItemLongClick: ((TokenItemState, CryptoCurrencyStatus) -> Unit)? = null,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        return when (value.value) {
            is CryptoCurrencyStatus.Loading -> value.mapToLoadingState()
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> value.mapToTokenItemState()
            is CryptoCurrencyStatus.MissedDerivation -> value.mapToNoAddressTokenItemState()
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> value.mapToUnreachableTokenItemState()
        }
    }

    private fun CryptoCurrencyStatus.mapToLoadingState(): TokenItemState.Loading {
        return TokenItemState.Loading(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this) as TokenItemState.TitleState.Content,
            subtitleState = requireNotNull(subtitleStateProvider(this)),
        )
    }

    private fun CryptoCurrencyStatus.mapToTokenItemState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = requireNotNull(subtitleStateProvider(this)),
            fiatAmountState = requireNotNull(fiatAmountStateProvider(this)),
            subtitle2State = requireNotNull(subtitle2StateProvider(this)),
            promoBannerState = promoBannerProvider(this),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(it, this) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState(): TokenItemState.Unreachable {
        return TokenItemState.Unreachable(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = subtitleStateProvider(this),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(it, this) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    private fun CryptoCurrencyStatus.mapToNoAddressTokenItemState(): TokenItemState.NoAddress {
        return TokenItemState.NoAddress(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = subtitleStateProvider(this),
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    companion object {

        fun CryptoCurrencyStatus.getFormattedFiatAmount(appCurrency: AppCurrency): String {
            val fiatAmount = value.fiatAmount ?: return DASH_SIGN

            val fiatYieldBalance = value.fiatRate?.times(getStakedBalance()).orZero()
            val totalAmount = fiatAmount.plus(fiatYieldBalance)

            return totalAmount.format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            }
        }

        fun CryptoCurrencyStatus.getFormattedCryptoAmount(): String {
            val cryptoAmount = value.amount ?: return DASH_SIGN

            val totalAmount = cryptoAmount.plus(getStakedBalance())

            return totalAmount.format { crypto(currency) }
        }

        private fun CryptoCurrencyStatus.getStakedBalance() = (value.stakingBalance as? StakingBalance.Data)
            ?.getTotalWithRewardsStakingBalance(blockchainId = currency.network.rawId).orZero()

        private fun createTitleState(
            currencyStatus: CryptoCurrencyStatus,
            yieldModuleApyMap: Map<String, BigDecimal>,
            stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
            onApyLabelClick: ((CryptoCurrencyStatus, ApySource, String) -> Unit)?,
        ): TokenItemState.TitleState {
            return when (val value = currencyStatus.value) {
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> {
                    TokenItemState.TitleState.Content(text = stringReference(currencyStatus.currency.name))
                }
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    val apyInfo = resolveEarnApy(
                        cryptoCurrencyStatus = currencyStatus,
                        yieldModuleApyMap = yieldModuleApyMap,
                        stakingApyMap = stakingApyMap,
                    )
                    TokenItemState.TitleState.Content(
                        text = stringReference(currencyStatus.currency.name),
                        hasPending = value.hasCurrentNetworkTransactions,
                        earnApy = apyInfo?.text,
                        earnApyIsActive = apyInfo?.isActive == true,
                        onApyLabelClick = if (apyInfo?.apy != null && onApyLabelClick != null) {
                            { onApyLabelClick.invoke(currencyStatus, apyInfo.source, apyInfo.apy) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        // polygon-pos_0xc2132d05d31c914a87c6611c10748aeb04b58e8f
        private fun resolveEarnApy(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            yieldModuleApyMap: Map<String, BigDecimal>,
            stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
        ): EarnApyInfo? {
            val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
            if (token != null && yieldModuleApyMap.isNotEmpty()) {
                val yieldSupplyApy = yieldModuleApyMap.entries.firstOrNull {
                    it.key.equals(
                        other = token.yieldSupplyKey(),
                        ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId),
                    )
                }?.value
                if (yieldSupplyApy != null) {
                    val isActive = cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive ?: false
                    return EarnApyInfo(
                        text = resourceReference(
                            R.string.yield_module_earn_badge,
                            wrappedList(yieldSupplyApy),
                        ),
                        isActive = isActive,
                        apy = yieldSupplyApy.toString(),
                        source = ApySource.YIELD_SUPPLY,
                    )
                }
            }

            if (stakingApyMap.isNotEmpty()) {
                val stakingInfo = findStakingRate(
                    currencyStatus = cryptoCurrencyStatus,
                    stakingApyMap = stakingApyMap,
                )
                val rewardTypeRes = when (stakingInfo.rewardType) {
                    Yield.RewardType.APR -> R.string.staking_apr_earn_badge
                    Yield.RewardType.UNKNOWN,
                    Yield.RewardType.APY,
                    null,
                    -> R.string.yield_module_earn_badge
                }
                if (stakingInfo.rate != null) {
                    val apyString = stakingInfo.rate.format { percent(withPercentSign = false) }
                    return EarnApyInfo(
                        text = resourceReference(
                            rewardTypeRes,
                            wrappedList(apyString),
                        ),
                        isActive = stakingInfo.isActive,
                        apy = apyString,
                        source = ApySource.STAKING,
                    )
                }
            }

            return null
        }

        private fun findStakingRate(
            currencyStatus: CryptoCurrencyStatus,
            stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
        ): StakingLocalInfo {
            val stakingAvailability = stakingApyMap[currencyStatus.currency] as? StakingAvailability.Available
                ?: return StakingLocalInfo(rate = null, isActive = false, rewardType = null)

            val stakingBalance = currencyStatus.value.stakingBalance as? StakingBalance.Data
            val stakeKitBalance = stakingBalance as? StakingBalance.Data.StakeKit

            val rateInfo = when (val stakingOptions = stakingAvailability.option) {
                is StakingOption.P2P -> {
                    // P2P or no balance: use preferred validators
                    // TODO add p2p logic
                    null
                }
                is StakingOption.StakeKit -> if (stakeKitBalance != null) {
                    val validatorsByAddress = stakingOptions.yield.validators.associateBy { it.address }
                    stakeKitBalance.balance.items
                        .mapNotNull { it.validatorAddress }
                        .firstNotNullOfOrNull { address ->
                            validatorsByAddress[address]?.rewardInfo
                        } ?: stakingOptions.yield.validators
                        .filter { it.preferred }
                        .mapNotNull { validator ->
                            validator.rewardInfo
                        }
                        .maxByOrNull { it.rate }
                } else {
                    stakingOptions.yield.validators
                        .filter { it.preferred }
                        .mapNotNull { validator ->
                            validator.rewardInfo
                        }
                        .maxByOrNull { it.rate }
                }
            }

            return StakingLocalInfo(
                rate = rateInfo?.rate,
                isActive = stakeKitBalance != null, // todo add p2p check
                rewardType = rateInfo?.type,
            )
        }

        private fun createSubtitleState(
            currencyStatus: CryptoCurrencyStatus,
            appCurrency: AppCurrency,
        ): TokenItemState.SubtitleState? {
            return when (currencyStatus.value) {
                is CryptoCurrencyStatus.Loading -> TokenItemState.SubtitleState.Loading
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> currencyStatus.getCryptoPriceState(appCurrency)
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> null
            }
        }

        private fun createSubtitle2State(status: CryptoCurrencyStatus): TokenItemState.Subtitle2State? {
            return when (status.value) {
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.Subtitle2State.TextContent(
                        text = status.getFormattedCryptoAmount(),
                        isFlickering = status.value.isFlickering(),
                    )
                }
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> null
            }
        }

        private fun createFiatAmountState(
            status: CryptoCurrencyStatus,
            appCurrency: AppCurrency,
        ): TokenItemState.FiatAmountState? {
            return when (status.value) {
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.FiatAmountState.Content(
                        text = status.getFormattedFiatAmount(appCurrency = appCurrency),
                        isFlickering = status.value.isFlickering(),
                        icons = buildList {
                            if (status.value.yieldSupplyStatus?.isActive == true &&
                                status.value.yieldSupplyStatus?.isAllowedToSpend == false
                            ) {
                                TokenItemState.FiatAmountState.Content.IconUM(
                                    iconRes = R.drawable.ic_alert_triangle_20,
                                    tint = IconTint.Warning,
                                ).let(::add)
                            }
                            if (status.value.sources.total == StatusSource.ONLY_CACHE) {
                                add(
                                    TokenItemState.FiatAmountState.Content.IconUM(
                                        iconRes = R.drawable.ic_error_sync_24,
                                    ),
                                )
                            }
                        }.toImmutableList(),
                    )
                }
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Loading,
                -> null
            }
        }

        private fun createPromoBannerState(
            status: CryptoCurrencyStatus,
            yieldModuleApyMap: Map<String, BigDecimal>,
            yieldSupplyPromoBannerKey: String?,
            onApyLabelClick: ((CryptoCurrencyStatus, ApySource, String) -> Unit)?,
            onYieldPromoCloseClick: (() -> Unit)?,
        ): TokenItemState.PromoBannerState {
            val token = status.currency as? CryptoCurrency.Token ?: return TokenItemState.PromoBannerState.Empty
            if (yieldSupplyPromoBannerKey == null || yieldSupplyPromoBannerKey != token.yieldSupplyKey() ||
                yieldModuleApyMap[token.yieldSupplyKey()] == null
            ) {
                return TokenItemState.PromoBannerState.Empty
            }
            val yieldSupplyApy =
                yieldModuleApyMap[token.yieldSupplyKey()] ?: return TokenItemState.PromoBannerState.Empty

            return TokenItemState.PromoBannerState.Content(
                title = resourceReference(
                    R.string.yield_module_main_screen_promo_banner_message,
                    wrappedList(yieldSupplyApy),
                ),
                onPromoBannerClick = {
                    onApyLabelClick?.invoke(status, ApySource.YIELD_SUPPLY, yieldSupplyApy.toString())
                },
                onCloseClick = {
                    onYieldPromoCloseClick?.invoke()
                },
            )
        }

        private fun CryptoCurrencyStatus.getCryptoPriceState(appCurrency: AppCurrency): TokenItemState.SubtitleState {
            val fiatRate = value.fiatRate
            val priceChange = value.priceChange

            return if (fiatRate != null && priceChange != null) {
                TokenItemState.SubtitleState.CryptoPriceContent(
                    price = fiatRate.getFormattedCryptoPrice(appCurrency),
                    priceChangePercent = priceChange.format { percent() },
                    type = priceChange.getPriceChangeType(),
                    isFlickering = value.isFlickering(),
                )
            } else {
                TokenItemState.SubtitleState.Unknown
            }
        }

        private fun BigDecimal.getFormattedCryptoPrice(appCurrency: AppCurrency): String {
            return format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            }
        }

        private fun BigDecimal.getPriceChangeType(): PriceChangeType {
            return PriceChangeConverter.fromBigDecimal(value = this)
        }

        fun CryptoCurrencyStatus.Value.isFlickering(): Boolean = sources.total == StatusSource.CACHE
    }

    private data class StakingLocalInfo(
        val rate: BigDecimal?,
        val isActive: Boolean,
        val rewardType: Yield.RewardType?,
    )

    private data class EarnApyInfo(
        val text: TextReference?,
        val isActive: Boolean,
        val apy: String?,
        val source: ApySource,
    )

    enum class ApySource {
        STAKING,
        YIELD_SUPPLY,
    }
}