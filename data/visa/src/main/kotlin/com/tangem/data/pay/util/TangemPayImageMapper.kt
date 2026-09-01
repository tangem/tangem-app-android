package com.tangem.data.pay.util

import com.tangem.domain.models.pay.TangemPayImage

internal fun tangemPayImageOrNull(type: String?, url: String?): TangemPayImage? = url?.let { presentUrl ->
    TangemPayImage(type = type.orEmpty(), url = presentUrl)
}