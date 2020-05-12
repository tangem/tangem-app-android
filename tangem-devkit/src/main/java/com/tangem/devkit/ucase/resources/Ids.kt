package com.tangem.devkit.ucase.resources

import com.tangem.devkit._arch.structure.Id

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
    WriteProtectedUserData,
    Personalize,
    Depersonalize,
    Unknown,
}