package com.tangem.features.tangempay.withdraw.bank.ui

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry

/**
 * Temporary stub: gives the wizard host a title until the wizard steps exist. The real title is
 * computed by the wizard model from its draft state (entry, current step, recipient), and this
 * mapping is removed together with the stub screen.
 */
internal fun BankWithdrawEntry.toScreenTitle(): TextReference = when (this) {
    BankWithdrawEntry.NewPersonal -> resourceReference(R.string.tangempay_withdraw_hub_to_personal_account)
    BankWithdrawEntry.NewBusiness -> resourceReference(R.string.tangempay_withdraw_hub_to_business_account)
    is BankWithdrawEntry.Saved,
    BankWithdrawEntry.SeeAll,
    -> resourceReference(R.string.tangempay_card_details_withdraw)
}