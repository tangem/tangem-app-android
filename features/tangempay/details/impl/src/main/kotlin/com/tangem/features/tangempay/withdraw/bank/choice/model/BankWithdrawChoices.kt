package com.tangem.features.tangempay.withdraw.bank.choice.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The two single-select steps of the wizard and the mapping between their option ids and draft values. */
internal object BankWithdrawChoices {

    private const val OWN_ACCOUNT_ID = "own_account"
    private const val THIRD_PARTY_ID = "third_party"

    fun transferType(isThirdParty: Boolean?): BankWithdrawChoiceConfig = BankWithdrawChoiceConfig(
        sectionTitle = resourceReference(R.string.tangempay_withdraw_bank_transfer_type_title),
        options = persistentListOf(
            BankWithdrawChoiceOption(
                id = OWN_ACCOUNT_ID,
                title = resourceReference(R.string.tangempay_withdraw_bank_transfer_type_own),
            ),
            BankWithdrawChoiceOption(
                id = THIRD_PARTY_ID,
                title = resourceReference(R.string.tangempay_withdraw_bank_transfer_type_third_party),
            ),
        ),
        selectedId = isThirdParty?.let { if (it) THIRD_PARTY_ID else OWN_ACCOUNT_ID },
    )

    fun isThirdParty(selectedId: String): Boolean = selectedId == THIRD_PARTY_ID

    fun accountType(selected: BankAccountType?): BankWithdrawChoiceConfig = BankWithdrawChoiceConfig(
        sectionTitle = resourceReference(R.string.tangempay_withdraw_bank_account_type_title),
        options = BankAccountType.entries
            .map { type -> BankWithdrawChoiceOption(id = type.name, title = type.title()) }
            .toImmutableList(),
        selectedId = selected?.name,
    )

    fun accountTypeOf(selectedId: String): BankAccountType = BankAccountType.valueOf(selectedId)

    private fun BankAccountType.title(): TextReference = when (this) {
        BankAccountType.CHECKING -> resourceReference(R.string.tangempay_withdraw_bank_account_type_checking)
        BankAccountType.SAVINGS -> resourceReference(R.string.tangempay_withdraw_bank_account_type_saving)
    }
}