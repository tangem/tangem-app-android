package com.tangem.devkit.ucase.variants.responses

import com.tangem.devkit._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
interface ResponseId : Id

enum class CardId : ResponseId {
    cardId,
    manufacturerName,
    status,
    firmwareVersion,
    cardPublicKey,
    settingsMask,
    cardData,
    issuerPublicKey,
    curve,
    maxSignatures,
    signingMethod,
    pauseBeforePin2,
    walletPublicKey,
    walletRemainingSignatures,
    walletSignedHashes,
    health,
    isActivated,
    activationSeed,
    paymentFlowVersion,
    userCounter,
    userProtectedCounter,
    empty
}

enum class CardDataId : ResponseId {
    batchId,
    manufactureDateTime,
    issuerName,
    blockchainName,
    manufacturerSignature,
    productMask,
    tokenSymbol,
    tokenContractAddress,
    tokenDecimal,
}

enum class SignId : ResponseId {
    cid,
    walletSignedHashes,
    walletRemainingSignatures,
    signature,
}

enum class DepersonalizeId: ResponseId {
    isSuccess
}

enum class CreateWalletId : ResponseId {
    cid,
    cardStatus,
    walletPublicKey,
}

enum class PurgeWalletId : ResponseId {
    cid,
    cardStatus
}

enum class ReadIssuerDataId : ResponseId {
    cid,
    issuerData,
    issuerDataSignature,
    issuerDataCounter
}

enum class ReadIssuerExtraDataId : ResponseId {
    cid,
    size,
    issuerData,
    issuerDataSignature,
    issuerDataCounter
}

enum class ReadUserDataId : ResponseId {
    cid,
    userData,
    userProtectedData,
    userCounter,
    userProtectedCounter
}