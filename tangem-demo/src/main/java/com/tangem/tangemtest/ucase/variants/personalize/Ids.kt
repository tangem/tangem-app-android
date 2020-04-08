package com.tangem.tangemtest.ucase.variants.personalize

import com.tangem.tangemtest._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */

interface PersonalizationId : Id

enum class BlockId : PersonalizationId {
    CardNumber,
    Common,
    SigningMethod,
    SignHashExProp,
    Denomination,
    Token,
    ProdMask,
    SettingsMask,
    SettingsMaskProtocolEnc,
    SettingsMaskNdef,
    Pins,
}

enum class CardNumber : PersonalizationId {
    Series,
    Number,
    BatchId,
}

enum class Common : PersonalizationId {
    Curve,
    Blockchain,
    BlockchainCustom,
    MaxSignatures,
    CreateWallet,
}

enum class SigningMethod : PersonalizationId {
    SignTx,
    SignTxRaw,
    SignValidatedTx,
    SignValidatedTxRaw,
    SignValidatedTxIssuer,
    SignValidatedTxRawIssuer,
    SignExternal,
}

enum class SignHashExProp : PersonalizationId {
    PinLessFloorLimit,
    CryptoExKey,
    RequireTerminalCertSig,
    RequireTerminalTxSig,
    CheckPin3,
}

enum class Denomination : PersonalizationId {
    WriteOnPersonalize,
    Denomination,
}

enum class Token : PersonalizationId {
    ItsToken,
    Symbol,
    ContractAddress,
    Decimal
}

enum class ProductMask : PersonalizationId {
    Note,
    Tag,
    IdCard,
    IdIssuerCard
}

enum class SettingsMask : PersonalizationId {
    IsReusable,
    NeedActivation,
    ForbidPurge,
    AllowSelectBlockchain,
    UseBlock,
    OneApdu,
    UseCvc,
    AllowSwapPin,
    AllowSwapPin2,
    ForbidDefaultPin,
    SmartSecurityDelay,
    ProtectIssuerDataAgainstReplay,
    SkipSecurityDelayIfValidated,
    SkipPin2CvcIfValidated,
    SkipSecurityDelayOnLinkedTerminal,
    RestrictOverwriteExtraIssuerData,
}

enum class SettingsMaskProtocolEnc : PersonalizationId {
    AllowUnencrypted,
    AllowStaticEncryption
}

enum class SettingsMaskNdef : PersonalizationId {
    UseNdef,
    DynamicNdef,
    DisablePrecomputedNdef,
    Aar,
    AarCustom,
    Uri,
}

enum class Pins : PersonalizationId {
    Pin,
    Pin2,
    Pin3,
    Cvc,
    PauseBeforePin2
}