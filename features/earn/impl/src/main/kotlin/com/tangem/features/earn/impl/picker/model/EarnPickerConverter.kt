package com.tangem.features.earn.impl.picker.model

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM.ContentUM
import com.tangem.features.earn.impl.picker.ui.state.EarnProductCardUM
import com.tangem.features.earn.impl.ui.toIncentiveBadge
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnPickerConverter @Inject constructor(
    private val registry: EarnProductsRegistry,
    private val cta: EarnPickerCta,
) {

    fun initial(intents: EarnPickerClickIntents): EarnPickerUM = placeholder(ContentUM.Loading, intents)

    fun unavailable(intents: EarnPickerClickIntents): EarnPickerUM =
        placeholder(ContentUM.Unavailable(onRetryClick = intents::onOverviewRetryClick), intents)

    @Suppress("LongParameterList")
    fun convert(
        product: EarnProduct,
        state: EarnProductState,
        summaries: List<EarnProductSummary>,
        selectedId: EarnProductId,
        ethBalance: BigDecimal?,
        intents: EarnPickerClickIntents,
    ): EarnPickerUM {
        val button = cta.resolve(
            state = state,
            ethBalance = ethBalance,
            onActionClick = { intents.onActionClick(product, it) },
            onBlocked = intents::onBlockedClick,
            onAddFunds = intents::onAddFundsClick,
        )

        return EarnPickerUM(
            title = EarnPickerTexts.title,
            subtitle = EarnPickerTexts.subtitle,
            barTitle = product.title,
            products = products(summaries, selectedId, intents),
            content = content(product, state, intents),
            cta = button.button,
            ctaHint = button.hint,
            onClose = intents::onCloseClick,
        )
    }

    private fun placeholder(content: ContentUM, intents: EarnPickerClickIntents): EarnPickerUM {
        val button = cta.resolve(
            state = EarnProductState.Loading,
            ethBalance = null,
            onActionClick = {},
            onBlocked = {},
            onAddFunds = {},
        )

        return EarnPickerUM(
            title = EarnPickerTexts.title,
            subtitle = EarnPickerTexts.subtitle,
            barTitle = EarnPickerTexts.title,
            products = persistentListOf(),
            content = content,
            cta = button.button,
            ctaHint = button.hint,
            onClose = intents::onCloseClick,
        )
    }

    private fun products(
        summaries: List<EarnProductSummary>,
        selectedId: EarnProductId,
        intents: EarnPickerClickIntents,
    ): ImmutableList<EarnProductCardUM> {
        if (summaries.size < MIN_SUMMARIES_FOR_CARDS) return persistentListOf()

        return summaries.mapNotNull { summary ->
            val product = registry.find(summary.productId) ?: return@mapNotNull null
            EarnProductCardUM(
                id = summary.productId.value,
                title = product.title,
                rate = summary.rateText,
                isSelected = summary.productId == selectedId,
                onClick = { intents.onProductClick(summary.productId) },
            )
        }.toImmutableList()
    }

    private fun content(product: EarnProduct, state: EarnProductState, intents: EarnPickerClickIntents): ContentUM {
        return when (state) {
            EarnProductState.Loading -> ContentUM.Loading
            EarnProductState.Unavailable -> ContentUM.Unavailable(onRetryClick = { intents.onRetryClick(product) })
            is EarnProductState.Ready -> ContentUM.Ready(
                description = state.description,
                incentive = state.incentiveRate.toIncentiveBadge(iconEnd = TangemIconUM.Icon(Icons.ic_info_16)),
                sections = EarnPickerTab.entries.filter { it.isAvailableIn(state) }.toImmutableList(),
                benefits = state.benefits.map { block -> block.toBenefitUM() }.toImmutableList(),
                details = state.details.map {
                    it.toUM(onInfoClick = intents::onInfoClick, onLinkClick = intents::onLinkClick)
                }.toImmutableList(),
            )
        }
    }

    private fun EarnTextBlock.toBenefitUM() = EarnBenefitUM(icon = icon, title = title, description = description)

    private fun EarnPickerTab.isAvailableIn(state: EarnProductState.Ready): Boolean = when (this) {
        EarnPickerTab.Calculator -> state.projection != null
        EarnPickerTab.Benefits -> state.benefits.isNotEmpty()
        EarnPickerTab.Details -> state.details.isNotEmpty()
    }

    private companion object {

        const val MIN_SUMMARIES_FOR_CARDS = 2
    }
}