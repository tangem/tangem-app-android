package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.jointaccount.main.JointAboutSignaturesComponent
import com.tangem.features.jointaccount.main.ui.AboutSignaturesBS
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAboutSignaturesComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAboutSignaturesComponent.Params,
) : JointAboutSignaturesComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AboutSignaturesBS(threshold = params.threshold, total = params.total, onDismiss = ::dismiss)
    }

    @AssistedFactory
    interface Factory : JointAboutSignaturesComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAboutSignaturesComponent.Params,
        ): DefaultJointAboutSignaturesComponent
    }
}