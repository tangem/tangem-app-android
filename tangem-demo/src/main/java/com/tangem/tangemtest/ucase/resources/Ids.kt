package com.tangem.tangemtest.ucase.resources

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