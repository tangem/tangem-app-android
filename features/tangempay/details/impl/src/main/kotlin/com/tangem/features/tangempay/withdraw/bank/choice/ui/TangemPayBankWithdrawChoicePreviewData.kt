package com.tangem.features.tangempay.withdraw.bank.choice.ui

import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoiceConfig
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoices
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUM
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUMFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute

internal object TangemPayBankWithdrawChoicePreviewData {

    private val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)

    private val callbacks = TangemPayBankWithdrawChoiceUMFactory.Callbacks(
        onBackClick = {},
        onCloseClick = {},
        onOptionClick = {},
        onContinueClick = {},
    )

    val transferTypeUnanswered: TangemPayBankWithdrawChoiceUM = create(
        config = BankWithdrawChoices.transferType(isThirdParty = null),
        step = TangemPayBankWithdrawInnerRoute.TransferType,
    )

    val transferType: TangemPayBankWithdrawChoiceUM = create(
        config = BankWithdrawChoices.transferType(isThirdParty = false),
        step = TangemPayBankWithdrawInnerRoute.TransferType,
    )

    val accountType: TangemPayBankWithdrawChoiceUM = create(
        config = BankWithdrawChoices.accountType(selected = BankAccountType.SAVINGS),
        step = TangemPayBankWithdrawInnerRoute.AccountType,
    )

    private fun create(config: BankWithdrawChoiceConfig, step: TangemPayBankWithdrawInnerRoute) =
        TangemPayBankWithdrawChoiceUMFactory(config = config, step = step, callbacks = callbacks)
            .create(draft = draft, selectedId = config.selectedId)
}