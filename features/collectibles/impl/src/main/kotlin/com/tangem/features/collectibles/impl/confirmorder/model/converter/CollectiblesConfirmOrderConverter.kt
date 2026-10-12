package com.tangem.features.collectibles.impl.confirmorder.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.ButtonUM
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.OrderUM
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.PriceRowUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/** The backend sends no total, so it is summed from the price and the fee. */
internal class CollectiblesConfirmOrderConverter(
    private val onBackClick: () -> Unit,
    private val onApproveClick: () -> Unit,
) : Converter<CollectiblesConfirmOrderConverter.Input, CollectiblesConfirmOrderUM.Content> {

    override fun convert(value: Input): CollectiblesConfirmOrderUM.Content {
        return CollectiblesConfirmOrderUM.Content(
            onBackClick = onBackClick,
            order = createOrder(value),
            onApproveClick = onApproveClick,
        )
    }

    private fun createOrder(value: Input): OrderUM {
        val machine = value.machine
        val fee = machine.feeAmount
        val total = machine.price + fee

        return OrderUM(
            rows = persistentListOf(
                PriceRowUM(
                    labelRef = stringReference("${machine.shortName.ifBlank { machine.name }} x1"),
                    value = machine.price.toFiat(),
                ),
                PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_platform_fee),
                    value = fee.toFiat(),
                ),
                PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_total),
                    value = total.format { crypto(cryptoCurrency = value.vaultBalance.usdcToken) },
                    isEmphasised = true,
                    hasDivider = false,
                ),
            ),
            button = createButton(value = value, total = total),
            packImageUrl = machine.imageUrl,
        )
    }

    private fun createButton(value: Input, total: BigDecimal): ButtonUM {
        val hasEnoughFunds = value.vaultBalance.usdcAmount >= total

        return when {
            // TODO handle and localize all possible states
            value.isConfirming -> ButtonUM(
                textRef = stringReference("Opening pack…"),
                isEnabled = false,
            )
            !hasEnoughFunds -> ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button),
                isEnabled = false,
            )
            else -> ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_approve_button),
                isEnabled = true,
            )
        }
    }

    data class Input(
        val machine: CollectibleGachaMachine,
        val vaultBalance: CollectiblesVaultBalance,
        val isConfirming: Boolean,
    )

    private fun BigDecimal.toFiat(): String = format {
        fiat(
            fiatCurrencyCode = AppCurrency.Default.code,
            fiatCurrencySymbol = AppCurrency.Default.symbol,
        ).optionalDecimals()
    }
}