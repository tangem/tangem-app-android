package com.tangem.tap.domain.visa

private const val COMMON_DESCRIPTION = "Error occurred. Please contact support."

internal enum class VisaCardScanHandlerError(val errorDescription: String) {
    FailedToCreateDerivationPath(COMMON_DESCRIPTION),
    FailedToFindWallet(COMMON_DESCRIPTION),
    FailedToFindDerivedWalletKey(COMMON_DESCRIPTION),
}