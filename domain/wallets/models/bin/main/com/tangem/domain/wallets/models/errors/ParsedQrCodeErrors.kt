package com.tangem.domain.wallets.models.errors

sealed class ParsedQrCodeErrors : Throwable() {

    object InvalidUriError : ParsedQrCodeErrors()
}