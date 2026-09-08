package com.tangem.features.tangempay.withdraw

import com.tangem.domain.pay.model.bankofframp.Beneficiary

/** Figma shows individuals as "First L." and businesses by their registered name. */
internal fun Beneficiary.toShortName(): String = when (this) {
    is Beneficiary.Individual -> {
        val lastInitial = lastName.trim().firstOrNull()?.uppercaseChar()
        if (lastInitial == null) firstName else "$firstName $lastInitial."
    }
    is Beneficiary.Business -> name
}