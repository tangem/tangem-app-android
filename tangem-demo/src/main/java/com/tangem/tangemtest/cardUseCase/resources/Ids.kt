package com.tangem.tangemtest.cardUseCase.resources

import com.tangem.tangemtest._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
enum class ActionType : Id {
    Scan,
    Sign,
    CreateWallet,
    PurgeWallet,
    ReadIssuerData,
    WriteIssuerData,
    ReadIssuerExData,
    WriteIssuerExData,
    ReadUserData,
    WriteUserData,
    Personalize,
    Depersonalize,
    Unknown,
}