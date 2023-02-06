package com.tangem.tap.features.details.ui.utils

import com.tangem.tap.features.details.redux.CardInfo
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

internal fun CardInfo.toResetCardDescriptionText(): TextReference {
    return if (!this.hasBackup || this.isTwin) {
        TextReference.Res(R.string.reset_card_without_backup_to_factory_message)
    } else {
        TextReference.Res(R.string.reset_card_with_backup_to_factory_message)
    }
}
