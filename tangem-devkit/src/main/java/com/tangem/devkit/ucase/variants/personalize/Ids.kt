package com.tangem.devkit.ucase.variants.personalize

import com.tangem.devkit._arch.structure.Id

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

enum class CardNumberId : PersonalizationId {
    Series,
    Number,
    BatchId,
}

enum class CommonId : PersonalizationId {
    Curve,
    Blockchain,
    BlockchainCustom,
    MaxSignatures,
    CreateWallet,
}

enum class SigningMethodId : PersonalizationId {
    SignTx,
    SignTxRaw,
    SignValidatedTx,
    SignValidatedTxRaw,
    SignValidatedTxIssuer,
    SignValidatedTxRawIssuer,
    SignExternal,
}

enum class SignHashExPropId : PersonalizationId {
    PinLessFloorLimit,
    CryptoExKey,
    RequireTerminalCertSig,
    RequireTerminalTxSig,
    CheckPin3,
}

enum class TokenId : PersonalizationId {
    ItsToken,
    Symbol,
    ContractAddress,
    Decimal
}

enum class ProductMaskId : PersonalizationId {
    Note,
    Tag,
    IdCard,
    IdIssuerCard
}

enum class SettingsMaskId : PersonalizationId {
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

enum class SettingsMaskProtocolEncId : PersonalizationId {
    AllowUnencrypted,
    AllowStaticEncryption
}

enum class SettingsMaskNdefId : PersonalizationId {
    UseNdef,
    DynamicNdef,
    DisablePrecomputedNdef,
    Aar,
    AarCustom,
    Uri,
}

enum class PinsId : PersonalizationId {
    Pin,
    Pin2,
    Pin3,
    Cvc,
    PauseBeforePin2
}