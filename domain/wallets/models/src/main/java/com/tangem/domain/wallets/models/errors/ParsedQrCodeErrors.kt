package com.tangem.domain.wallets.models.errors

sealed class ParsedQrCodeErrors : Throwable() {

    class InvalidUriError : ParsedQrCodeErrors()
}