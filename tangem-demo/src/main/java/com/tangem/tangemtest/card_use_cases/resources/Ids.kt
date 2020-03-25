package com.tangem.tangemtest.card_use_cases.resources

import com.tangem.tangemtest._arch.structure.base.Id

/**
[REDACTED_AUTHOR]
 */
enum class ActionType: Id {
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