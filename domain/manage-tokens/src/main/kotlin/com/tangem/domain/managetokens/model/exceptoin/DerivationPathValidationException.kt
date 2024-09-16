package com.tangem.domain.managetokens.model.exceptoin

sealed class DerivationPathValidationException {

    data object Empty : DerivationPathValidationException()

    data object Invalid : DerivationPathValidationException()
}