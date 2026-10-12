package com.tangem.features.tangempay.withdraw.bank.choice.state

import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoiceConfig
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUM.OptionUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayBankWithdrawChoiceUMFactory(
    private val config: BankWithdrawChoiceConfig,
    private val step: TangemPayBankWithdrawInnerRoute,
    private val callbacks: Callbacks,
) {

    fun create(draft: BankWithdrawDraft, selectedId: String?): TangemPayBankWithdrawChoiceUM =
        TangemPayBankWithdrawChoiceUM(
            header = BankWithdrawHeaderFactory.create(
                draft = draft,
                step = step,
                onCloseClick = callbacks.onCloseClick,
            ),
            sectionTitle = config.sectionTitle,
            options = config.options
                .map { option ->
                    OptionUM(
                        title = option.title,
                        isSelected = option.id == selectedId,
                        onClick = { callbacks.onOptionClick(option.id) },
                    )
                }
                .toImmutableList(),
            isContinueEnabled = selectedId != null,
            onBackClick = callbacks.onBackClick,
            onContinueClick = callbacks.onContinueClick,
        )

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onOptionClick: (id: String) -> Unit,
        val onContinueClick: () -> Unit,
    )
}