package com.tangem.store.features.cardsettings.impl.common.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.card.CardTypesResolver

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