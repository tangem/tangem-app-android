package com.tangem.core.error

/**
 * Universal error interface for all errors in the app.
 * Each error code must follow this format: xxxyyyzzz where
 * xxx - Feature code
 * yyy - Subsystem code
 * zzz - Specific error code
 * If you need to add new feature add it to list below incrementing last code.
 *
 * Features:
 * `100` - App error
 * `101` - TangemSdkError
 * `102` - BlockchainSdkError
 * `103` - Express
 * `104` - Visa
 * `105` - Staking
 * `106` - NFT
 * `107` - WalletConnect
 */
interface UniversalError {
    val errorCode: Int
}