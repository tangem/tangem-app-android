package com.tangem.features.kyc

import com.tangem.core.decompose.context.AppComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Mocking it for release/external builds to exclude SumSub dependency
 */
internal class MockKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
) : KycComponent, AppComponentContext by appComponentContext {

    override fun launch() {
        /* no op */
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(appComponentContext: AppComponentContext): MockKycComponent
    }
}