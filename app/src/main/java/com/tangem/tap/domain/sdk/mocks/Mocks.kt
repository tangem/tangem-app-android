package com.tangem.tap.domain.sdk.mocks

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.derivation.DerivationTaskResponse

interface Mocks {

    val scanResponse: ScanResponse

    val derivationTaskResponse: DerivationTaskResponse

    val cardDto: CardDTO
}
