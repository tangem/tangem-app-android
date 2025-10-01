package com.tangem.features.kyc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Mocking it for release/external builds to exclude SumSub dependency
 * This will never be called if the FT [isTangemPayEnabled] is off
 */
@Suppress("UnusedPrivateProperty")
internal class MockKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: KycComponent.Params,
) : KycComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) { /* no op */ }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(context: AppComponentContext, params: KycComponent.Params): MockKycComponent
    }
}