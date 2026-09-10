package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.toShortName

internal data class BankWithdrawHeaderUM(
    val title: TextReference,
    val subtitle: TextReference?,
)

internal object BankWithdrawHeaderFactory {

    fun create(draft: BankWithdrawDraft, step: TangemPayBankWithdrawInnerRoute): BankWithdrawHeaderUM {
        val counter = BankWithdrawFlow.stepCounter(entry = draft.entry, step = step)
        return BankWithdrawHeaderUM(
            title = title(draft),
            subtitle = counter?.let {
                resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(it.current, it.total))
            },
        )
    }

    private fun title(draft: BankWithdrawDraft): TextReference = when (draft.entry) {
        BankWithdrawEntry.NewPersonal -> resourceReference(R.string.tangempay_withdraw_hub_to_personal_account)
        BankWithdrawEntry.NewBusiness -> resourceReference(R.string.tangempay_withdraw_hub_to_business_account)
        is BankWithdrawEntry.Saved,
        BankWithdrawEntry.SeeAll,
        -> {
            val recipient = draft.recipient
            if (recipient == null) {
                resourceReference(R.string.tangempay_card_details_withdraw)
            } else {
                resourceReference(
                    R.string.tangempay_withdraw_bank_title_to_recipient,
                    wrappedList(recipient.details.beneficiary.toShortName()),
                )
            }
        }
    }
}