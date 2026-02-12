package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.features.feed.model.converter.EarnTokenWithCurrencyToListItemUMConverter
import com.tangem.features.feed.model.earn.analytics.MOSTLY_USED_SOURCE
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.earn.state.EarnUM
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class UpdateMostlyUsedStateTransformer(
    private val earnResult: EarnTopToken?,
    private val onItemClick: (EarnTokenWithCurrency, source: String) -> Unit,
    private val onRetryClick: () -> Unit,
) : EarnUMTransformer {

    private val converter = EarnTokenWithCurrencyToListItemUMConverter(
        onItemClick = { token -> onItemClick(token, MOSTLY_USED_SOURCE) },
    )

    override fun transform(prevState: EarnUM): EarnUM {
        return when (earnResult) {
            null -> prevState.copy(mostlyUsed = EarnListUM.Loading)
            else -> earnResult.fold(
                ifLeft = { prevState.copy(mostlyUsed = EarnListUM.Error(onRetryClicked = onRetryClick)) },
                ifRight = { list ->
                    val newItems = list
                        .sortedWith(
                            compareByDescending<EarnTokenWithCurrency> {
                                it.earnToken.apy.toBigDecimalOrNull() ?: BigDecimal.ZERO
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