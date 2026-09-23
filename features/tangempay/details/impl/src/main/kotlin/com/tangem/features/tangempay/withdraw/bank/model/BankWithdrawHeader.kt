package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute

internal data class BankWithdrawHeaderUM(
    val title: TextReference,
    val subtitle: TextReference?,
    val onCloseClick: () -> Unit,
)

internal object BankWithdrawHeaderFactory {

    fun create(
        draft: BankWithdrawDraft,
        step: TangemPayBankWithdrawInnerRoute,
        onCloseClick: () -> Unit,
    ): BankWithdrawHeaderUM {
        val counter = BankWithdrawFlow.stepCounter(draft = draft, step = step)
        return BankWithdrawHeaderUM(
            title = title(draft = draft, step = step),
            subtitle = counter?.let {
                resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(it.current, it.total))
            },
            onCloseClick = onCloseClick,
        )
    }

    /** Figma titles the account type step by its action, the preview by its purpose, other steps by the recipient. */
    private fun title(draft: BankWithdrawDraft, step: TangemPayBankWithdrawInnerRoute): TextReference = when (step) {
        TangemPayBankWithdrawInnerRoute.AccountType -> {
            resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title)
        }
        TangemPayBankWithdrawInnerRoute.Preview -> resourceReference(R.string.tangempay_withdraw_bank_confirm_title)
        else -> recipientTitle(draft)
    }

    private fun recipientTitle(draft: BankWithdrawDraft): TextReference {
        val recipient = draft.recipient
        if (recipient != null) return stringReference(recipient.displayName)
        return when (draft.entry) {
            BankWithdrawEntry.NewPersonal -> resourceReference(R.string.tangempay_withdraw_hub_to_personal_account)
            BankWithdrawEntry.NewBusiness -> resourceReference(R.string.tangempay_withdraw_hub_to_business_account)
            is BankWithdrawEntry.Saved,
            BankWithdrawEntry.SeeAll,
            -> resourceReference(R.string.tangempay_withdraw_hub_bank_section_title)
        }
    }
}