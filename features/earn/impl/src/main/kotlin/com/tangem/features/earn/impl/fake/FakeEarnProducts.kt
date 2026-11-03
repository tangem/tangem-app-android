package com.tangem.features.earn.impl.fake

import com.tangem.features.earn.product.api.product.EarnProduct

fun createFakeEarnProduct(
    preset: FakeEarnProductPreset,
    behavior: FakeEarnProductBehavior = FakeEarnProductBehavior(),
): EarnProduct = FakeEarnProduct(preset, behavior)