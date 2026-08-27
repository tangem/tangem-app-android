package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.R
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * Maps a top-earn token (an opportunity the user doesn't hold yet) to a For You list item:
 * network subtitle, APY top-end and earn-type bottom-end. Shared by the earn-opportunities
 * converters that surface suggestions ([ForYouEarnOpportunitiesNoTokensConverter],
 * [ForYouEarnOpportunitiesTokensActiveConverter]).
 */
internal class ForYouEarnOpportunitiesTopTokenRowConverter(
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
) : Converter<EarnTokenWithCurrency, ForYouTokenListItemUM> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: EarnTokenWithCurrency): ForYouTokenListItemUM {
        val (networkName, earnToken, cryptoCurrency) = value

        val type = earnToken.rewardType.name
        val apy = convertPercent(earnToken.apy)

        return ForYouTokenListItemUM(
            tokenRowUM = TangemTokenRowUM.Content(
                id = cryptoCurrency.id.value,
                headIconUM = TangemIconUM.Currency(iconConverter.convert(cryptoCurrency)),
                titleUM = TangemTokenRowUM.TitleUM.Content(
                    text = stringReference(cryptoCurrency.name),
                ),
                subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                    text = resourceReference(
                        R.string.wallet_network_group_title,
                        wrappedList(networkName),
                    ),
                ),
                topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = styledStringReference(
                        value = "$type $apy",
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
                    ),
                ),
                bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = resourceReference(
                        when (earnToken.type) {
                            EarnType.STAKING -> R.string.common_staking
                            EarnType.YIELD -> R.string.common_yield_mode
                        },
                    ),
                ),
                onItemClick = {
                    val earnType = when (earnToken.type) {
                        EarnType.STAKING -> {
                            val integrationID = StakingIntegrationID.create(currencyId = value.cryptoCurrency.id)
                                ?: return@Content

                            ForYouEarnOpportunitiesType.Staking(
                                integrationID = integrationID,
                                rewardType = earnToken.rewardType,
                            )
                        }
                        EarnType.YIELD -> ForYouEarnOpportunitiesType.YieldSupply(
                            apy = earnToken.apy,
                            rewardType = earnToken.rewardType,
                        )
                    }
                    onTokenClick(
                        null,
                        value.cryptoCurrency,
                        earnType,
                    )
                },
                onItemLongClick = { _, _ -> },
            ),
            tokenList = persistentListOf(),
            isExpanded = false,
            isExpandable = false,
            segmentColor = null,
        )
    }

    private fun convertPercent(value: String): String {
        return BigDecimal(value).format { percent(withPercentSign = false) }
    }
}