package com.tangem.tap.domain.walletregistration

import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.backup.PrimaryCard
import com.tangem.sdk.api.TangemSdkManager

/**
 * Re-registers the cold wallet on the tap that finalizes the [primaryCard] backup: the wallet was registered on its
 * creation, before the backup changed its status, so the backend gets the backed-up status. A card that doesn't attest
 * its wallet status would send the same registration again, so its [finalization] is left as is. An unknown firmware
 * (an interrupted backup) is re-registered, since a redundant registration is harmless.
 */
internal fun TangemSdkManager.withBackupColdWalletRegistration(
    primaryCard: PrimaryCard,
    finalization: CardSessionRunnable<Card>,
): CardSessionRunnable<Card> {
    val firmwareVersion = primaryCard.firmwareVersion
    if (firmwareVersion != null && firmwareVersion < FirmwareVersion.WalletStatusAttestationAvailable) {
        return finalization
    }

    return withColdWalletRegistration(finalization, shouldForceRegistration = true)
}