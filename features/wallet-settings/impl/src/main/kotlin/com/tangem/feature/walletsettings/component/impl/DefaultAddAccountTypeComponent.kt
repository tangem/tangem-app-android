package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.feature.walletsettings.ui.AddAccountTypeBS
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddAccountTypeComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddAccountTypeComponent.Params,
) : AddAccountTypeComponent, AppComponentContext by context {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AddAccountTypeBS(
            onCryptoAccountClick = params.onCryptoAccountClick,
            onJointAccountClick = params.onJointAccountClick,
            onDismiss = ::dismiss,
        )
    }

    @AssistedFactory
    interface Factory : AddAccountTypeComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddAccountTypeComponent.Params,
        ): DefaultAddAccountTypeComponent
    }
}