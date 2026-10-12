package com.tangem.features.tangempay.withdraw

import com.tangem.domain.pay.model.bankofframp.Beneficiary

/** The recipient as a confirmation names them: an individual by both names, a business by its registered name. */
internal fun Beneficiary.toFullName(): String = when (this) {
    is Beneficiary.Individual -> listOf(firstName, lastName)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(separator = " ")
    is Beneficiary.Business -> name.trim()
}