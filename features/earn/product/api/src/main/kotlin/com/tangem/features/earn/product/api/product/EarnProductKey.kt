package com.tangem.features.earn.product.api.product

import dagger.MapKey

/** Map key of an `EarnSession.Factory`: equals `EarnProductId.value` of the product the session belongs to. */
@MapKey
@Retention(AnnotationRetention.RUNTIME)
annotation class EarnProductKey(val value: String)