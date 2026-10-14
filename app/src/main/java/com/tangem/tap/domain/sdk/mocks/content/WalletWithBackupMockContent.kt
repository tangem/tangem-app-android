package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

object WalletWithBackupMockContent : MockContent by WalletMockContent {

    override val cardDto: CardDTO = WalletMockContent.cardDto.copy(
        backupStatus = CardDTO.BackupStatus.Active(cardCount = 1),
    )

    override val scanResponse: ScanResponse = WalletMockContent.scanResponse.copy(
        card = cardDto,
        derivedKeys = WalletMockContent.derivationTaskResponse.entries,
    )
}