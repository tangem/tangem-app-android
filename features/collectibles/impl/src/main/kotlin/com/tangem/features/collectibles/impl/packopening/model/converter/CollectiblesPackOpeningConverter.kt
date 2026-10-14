package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.collectibles.account.covers
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.features.collectibles.impl.common.formatter.formatUsd
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.ButtonUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.OrderUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.PriceRowUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

/** The backend sends no total, so it is summed from the price and the fee. */
internal class CollectiblesPackOpeningConverter(
    private val onBackClick: () -> Unit,
    private val onCloseClick: () -> Unit,
    private val onApproveClick: () -> Unit,
    private val reveal: CollectiblesPackOpeningUM.RevealUM,
) : Converter<CollectiblesPackOpeningConverter.Input, CollectiblesPackOpeningUM.Content> {

    override fun convert(value: Input): CollectiblesPackOpeningUM.Content {
        return CollectiblesPackOpeningUM.Content(
            onBackClick = onBackClick,
            onCloseClick = onCloseClick,
            order = createOrder(value),
            reveal = reveal,
            onApproveClick = onApproveClick,
            isNavigationVisible = value.step is OpenPackStep.NotStarted || value.step is OpenPackStep.Failed,
        )
    }

    private fun createOrder(value: Input): OrderUM {
        val machine = value.machine
        val fee = machine.feeAmount
        val total = machine.total

        return OrderUM(
            rows = persistentListOf(
                PriceRowUM(
                    // Always one pack: phase one sells packs one at a time
                    labelRef = stringReference("${machine.shortName.ifBlank { machine.name }} x1"),
                    value = machine.price.formatUsd(),
                ),
                PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_platform_fee),
                    value = fee.formatUsd(),
                ),
                PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_total),
                    value = total.format { crypto(cryptoCurrency = value.vaultBalance.usdcToken) },
                    isEmphasised = true,
                    hasDivider = false,
                ),
            ),
            button = createButton(value = value),
            packImageUrl = machine.imageUrl,
        )
    }

    private fun createButton(value: Input): ButtonUM {
        val hasEnoughFunds = value.vaultBalance.covers(value.machine)

        return when (value.step) {
            OpenPackStep.Generating -> ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_generating_pack_button),
                isEnabled = false,
            )
            OpenPackStep.Confirming -> ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_confirming_transaction_button),
                isEnabled = false,
            )
            OpenPackStep.Opening,
            is OpenPackStep.Opened,
            -> ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_opening_pack_button),
                isEnabled = false,
            )
            OpenPackStep.NotStarted,
            is OpenPackStep.Failed,
            -> when {
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
    }

    data class Input(
        val machine: CollectibleGachaMachine,
        val vaultBalance: CollectiblesVaultBalance,
        val step: OpenPackStep,
    )
}