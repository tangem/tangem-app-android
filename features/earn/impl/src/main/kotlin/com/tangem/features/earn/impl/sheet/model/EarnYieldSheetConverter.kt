package com.tangem.features.earn.impl.sheet.model

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.sheet.EarnYieldSheetTexts
import com.tangem.features.earn.impl.sheet.ui.state.EarnYieldRowUM
import com.tangem.features.earn.impl.sheet.ui.state.EarnYieldSheetUM
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnYieldSheetConverter @Inject constructor(
    private val registry: EarnProductsRegistry,
) {

    fun convert(
        summaries: List<EarnProductSummary>,
        currency: CryptoCurrency,
        fiatRate: BigDecimal?,
        appCurrency: AppCurrency,
        intents: EarnYieldSheetClickIntents,
    ): EarnYieldSheetUM {
        val (active, inactive) = summaries.partition { it.position != null }
        val toRow = { summary: EarnProductSummary ->
            row(
                summary = summary,
                currency = currency,
                fiatRate = fiatRate,
                appCurrency = appCurrency,
                intents = intents,
            )
        }

        return EarnYieldSheetUM(
            title = EarnYieldSheetTexts.title,
            inactiveTitle = EarnYieldSheetTexts.inactiveTitle,
            active = active.mapNotNull(toRow).toImmutableList(),
            inactive = inactive.mapNotNull(toRow).toImmutableList(),
            onDismiss = intents::onDismissClick,
        )
    }

    fun empty(intents: EarnYieldSheetClickIntents): EarnYieldSheetUM = EarnYieldSheetUM(
        title = EarnYieldSheetTexts.title,
        inactiveTitle = EarnYieldSheetTexts.inactiveTitle,
        active = persistentListOf(),
        inactive = persistentListOf(),
        onDismiss = intents::onDismissClick,
    )

    private fun row(
        summary: EarnProductSummary,
        currency: CryptoCurrency,
        fiatRate: BigDecimal?,
        appCurrency: AppCurrency,
        intents: EarnYieldSheetClickIntents,
    ): EarnYieldRowUM? {
        val product = registry.find(summary.productId) ?: return null
        val position = summary.position
        val amount = position?.cryptoAmount

        return EarnYieldRowUM(
            id = summary.productId,
            title = product.title,
            icon = product.provider.icon,
            rate = summary.rateText,
            isActive = position != null,
            amount = amount?.let { stringReference(it.format { crypto(currency) }) },
            amountFiat = if (amount != null && fiatRate != null) {
                stringReference((amount * fiatRate).format { fiat(appCurrency.code, appCurrency.symbol) })
            } else {
                null
            },
            caption = EarnYieldSheetTexts.startEarning.takeIf { position == null },
            risk = summary.risk?.toBadge(),
            onClick = { intents.onActionClick(summary.productId, summary.onClickAction) },
        )
    }

    private fun EarnRisk.toBadge() = TangemBadgeUM(
        text = EarnYieldSheetTexts.risk(this),
        status = when (this) {
            EarnRisk.Low -> TangemBadge.Status.Success
            EarnRisk.Medium -> TangemBadge.Status.Warning
            EarnRisk.High -> TangemBadge.Status.Error
        },
        size = TangemBadge.Size.X4,
    )
}