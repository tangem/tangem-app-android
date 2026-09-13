package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.features.feed.earn.model.converter.EarnTokenWithCurrencyToMostlyUsedUMConverter
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class UpdateOpportunitiesStateTransformer(
    private val earnResult: EarnTopToken?,
    private val onItemClick: (EarnTokenWithCurrency, source: EarnSource) -> Unit,
    private val onRetryClick: () -> Unit,
) : Transformer<EarnFeedTabUM> {

    private val converter = EarnTokenWithCurrencyToMostlyUsedUMConverter(
        onItemClick = { token -> onItemClick(token, EarnSource.MOSTLY_USED_SOURCE) },
    )

    override fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM {
        return when (earnResult) {
            null -> prevState.copy(mostlyUsed = EarnListUM.Loading)
            else -> earnResult.fold(
                ifLeft = { prevState.copy(mostlyUsed = EarnListUM.Error(onRetryClicked = onRetryClick)) },
                ifRight = { list ->
                    val newItems = list
                        .sortedWith(
                            compareByDescending<EarnTokenWithCurrency> {
                                it.earnToken.apy.parseBigDecimalOrNull() ?: BigDecimal.ZERO
                            }.thenBy { it.earnToken.tokenName },
                        )
                        .map(converter::convert)
                        .toPersistentList()
                    prevState.copy(mostlyUsed = EarnListUM.Content(items = newItems))
                },
            )
        }
    }
}