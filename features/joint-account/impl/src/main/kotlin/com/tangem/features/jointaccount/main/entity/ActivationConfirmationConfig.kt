package com.tangem.features.jointaccount.main.entity

import androidx.annotation.DrawableRes

/**
 * Slot config for the "Activate account" confirmation sheet.
 *
 * [cryptoAccountId] is the account this confirmation belongs to — it is threaded back into
 * [com.tangem.features.jointaccount.main.model.JointAccountMembersModel.onConfirmActivationClick] rather
 * than re-read off the screen state, so the confirm action always targets the account the sheet was

 * process death, it just closes.
 */
internal data class ActivationConfirmationConfig(
    val cryptoAccountId: String,
    @DrawableRes val walletInteractionIconRes: Int?,
)