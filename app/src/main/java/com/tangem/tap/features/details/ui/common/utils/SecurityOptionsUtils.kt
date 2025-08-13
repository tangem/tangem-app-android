package com.tangem.tap.features.details.ui.common.utils

import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.features.details.redux.SecurityOption
import java.util.EnumSet

internal fun getCurrentSecurityOption(card: CardDTO): SecurityOption = when {
    card.isAccessCodeSet -> SecurityOption.AccessCode
    card.isPasscodeSet == true -> SecurityOption.PassCode
    else -> SecurityOption.LongTap
}

internal fun getAllowedSecurityOptions(
    card: CardDTO,
    cardTypesResolver: CardTypesResolver,
    currentSecurityOption: SecurityOption,
): EnumSet<SecurityOption> = when {
    cardTypesResolver.isStart2Coin() || cardTypesResolver.isTangemNote() -> EnumSet.of(SecurityOption.LongTap)
    card.settings.isBackupAllowed -> EnumSet.of(currentSecurityOption)
    else -> prepareAllowedSecurityOptions(
        cardTypesResolver = cardTypesResolver,
        currentSecurityOption = currentSecurityOption,
    )
}

private fun prepareAllowedSecurityOptions(
    cardTypesResolver: CardTypesResolver,
    currentSecurityOption: SecurityOption?,
): EnumSet<SecurityOption> {
    val allowedSecurityOptions = EnumSet.of(SecurityOption.LongTap)

    if (cardTypesResolver.isTangemTwins()) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    if (currentSecurityOption == SecurityOption.AccessCode) {
        allowedSecurityOptions.add(SecurityOption.AccessCode)
    }
    if (currentSecurityOption == SecurityOption.PassCode) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }

    return allowedSecurityOptions
}