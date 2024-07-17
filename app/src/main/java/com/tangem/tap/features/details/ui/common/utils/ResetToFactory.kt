package com.tangem.tap.features.details.ui.common.utils

import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

internal fun getResetToFactoryDescription(card: CardDTO, typesResolver: CardTypesResolver): TextReference {
    return if (card.backupStatus?.isActive != true || typesResolver.isTangemTwins()) {
        TextReference.Res(R.string.reset_card_without_backup_to_factory_message)
    } else {
        TextReference.Res(R.string.reset_card_with_backup_to_factory_message)
    }
}