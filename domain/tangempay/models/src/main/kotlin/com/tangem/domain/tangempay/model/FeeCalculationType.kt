package com.tangem.domain.tangempay.model

import java.util.Locale

enum class FeeCalculationType {
    FIXED,
    PERCENTAGE,
    ;

    companion object {
        fun fromString(value: String?): FeeCalculationType = when (value?.uppercase(Locale.US)) {
            "PERCENTAGE" -> PERCENTAGE
            else -> FIXED
        }
    }
}