package com.tangem.tap.features.details.ui.common.utils

import com.tangem.domain.card.CardTypesResolver
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

internal fun getResetToFactoryDescription(
    isActiveBackupStatus: Boolean,
    typesResolver: CardTypesResolver,
): TextReference {
    return if (!isActiveBackupStatus || typesResolver.isTangemTwins()) {
        TextReference.Res(R.string.reset_card_without_backup_to_factory_message)
    } else {
        TextReference.Res(R.string.reset_card_with_backup_to_factory_message)
    }
}