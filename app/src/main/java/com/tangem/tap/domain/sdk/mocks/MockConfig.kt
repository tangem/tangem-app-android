package com.tangem.tap.domain.sdk.mocks

import arrow.core.Either
import com.tangem.domain.models.scan.ProductType

data class MockConfig(
    val content: Either<ProductType, Mocks>,
)