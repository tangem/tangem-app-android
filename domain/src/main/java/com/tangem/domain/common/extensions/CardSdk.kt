package com.tangem.domain.common.extensions

import com.tangem.common.card.FirmwareVersion

/**
[REDACTED_AUTHOR]
 */
val FirmwareVersion.Companion.SolanaAvailable
    get() = FirmwareVersion(4, 12)

val FirmwareVersion.Companion.SolanaTokensAvailable
    get() = FirmwareVersion(4, 52)