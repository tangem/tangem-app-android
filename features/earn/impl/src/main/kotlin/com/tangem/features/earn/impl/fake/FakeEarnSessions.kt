package com.tangem.features.earn.impl.fake

import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnSession

fun createFakeEarnSession(
    preset: FakeEarnSessionPreset,
    behavior: FakeEarnSessionBehavior = FakeEarnSessionBehavior(),
): EarnSession = FakeEarnSession(preset, behavior)

val fakeEarnSessionFactories: Map<String, EarnSession.Factory> =
    FakeEarnSessionPreset.entries.associate { preset ->
        preset.productId to EarnSession.Factory { createFakeEarnSession(preset) }
    }

fun fakeClaimAction(): EarnFlowAction = FakeClaim