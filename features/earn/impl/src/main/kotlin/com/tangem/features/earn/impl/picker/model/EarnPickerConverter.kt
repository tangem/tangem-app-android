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
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnPickerConverter @Inject constructor(
    private val registry: EarnProductsRegistry,
    private val cta: EarnPickerCta,
) {

    fun initial(onClose: () -> Unit): EarnPickerUM = placeholder(ContentUM.Loading, onClose)

    fun unavailable(onRetryClick: () -> Unit, onClose: () -> Unit): EarnPickerUM =
        placeholder(ContentUM.Unavailable(onRetryClick), onClose)

    @Suppress("LongParameterList")
    fun convert(
        product: EarnProduct,
        state: EarnProductState,
        summaries: List<EarnProductSummary>,
        selectedId: EarnProductId,
        ethBalance: BigDecimal?,
        callbacks: Callbacks,
    ): EarnPickerUM {
        val button = cta.resolve(
            state = state,
            ethBalance = ethBalance,
            onActionClick = { callbacks.onActionClick(product, it) },
            onBlocked = callbacks.onBlocked,
            onAddFunds = callbacks.onAddFunds,
        )

        return EarnPickerUM(
            title = EarnPickerTexts.title,
            subtitle = EarnPickerTexts.subtitle,
            barTitle = product.title,
            products = products(summaries, selectedId, callbacks),
            content = content(product, state, callbacks),
            cta = button.button,
            ctaHint = button.hint,
            onClose = callbacks.onClose,
        )
    }

    private fun placeholder(content: ContentUM, onClose: () -> Unit): EarnPickerUM {
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
            onClose = onClose,
        )
    }

    private fun products(
        summaries: List<EarnProductSummary>,
        selectedId: EarnProductId,
        callbacks: Callbacks,
    ): ImmutableList<EarnProductCardUM> {
        if (summaries.size < MIN_SUMMARIES_FOR_CARDS) return persistentListOf()

        return summaries.mapNotNull { summary ->
            val product = registry.find(summary.productId) ?: return@mapNotNull null
            EarnProductCardUM(
                id = summary.productId.value,
                title = product.title,
                rate = summary.rateText,
                isSelected = summary.productId == selectedId,
                onClick = { callbacks.onProductClick(summary.productId) },
            )
        }.toImmutableList()
    }

    private fun content(product: EarnProduct, state: EarnProductState, callbacks: Callbacks): ContentUM {
        return when (state) {
            EarnProductState.Loading -> ContentUM.Loading
            EarnProductState.Unavailable -> ContentUM.Unavailable(onRetryClick = { callbacks.onRetryClick(product) })
            is EarnProductState.Ready -> ContentUM.Ready(
                description = state.description,
                incentive = state.incentiveRate.toIncentiveBadge(iconEnd = TangemIconUM.Icon(Icons.ic_info_16)),
                sections = EarnPickerTab.entries.filter { it.isAvailableIn(state) }.toImmutableList(),
                benefits = state.benefits.map { block -> block.toBenefitUM() }.toImmutableList(),
                details = state.details.map {
                    it.toUM(onInfoClick = callbacks.onInfoClick, onLinkClick = callbacks.onLinkClick)
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

    data class Callbacks(
        val onProductClick: (EarnProductId) -> Unit,
        val onActionClick: (EarnProduct, EarnAction) -> Unit,
        val onBlocked: (EarnBlock) -> Unit,
        val onAddFunds: () -> Unit,
        val onRetryClick: (EarnProduct) -> Unit,
        val onInfoClick: (EarnTextBlock) -> Unit,
        val onLinkClick: (String) -> Unit,
        val onClose: () -> Unit,
    )

    private companion object {

        const val MIN_SUMMARIES_FOR_CARDS = 2
    }
}