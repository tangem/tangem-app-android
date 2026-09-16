@file:Suppress("MagicNumber")

package com.tangem.domain.collectibles.vault

import com.tangem.core.error.UniversalError

sealed class GachaVaultError(override val errorCode: Int) : UniversalError {

    data object NetworkUnsupported : GachaVaultError(errorCode = 109001001)

    data object WalletUnavailable : GachaVaultError(errorCode = 109001002)

    data class Unexpected(val cause: Throwable) : GachaVaultError(errorCode = 109001003)
}