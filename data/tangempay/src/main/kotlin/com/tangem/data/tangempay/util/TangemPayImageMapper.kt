package com.tangem.data.tangempay.util

import com.tangem.domain.models.tangempay.TangemPayImage

internal fun tangemPayImageOrNull(type: String?, url: String?): TangemPayImage? = url?.let { presentUrl ->
    TangemPayImage(type = type.orEmpty(), url = presentUrl)
}