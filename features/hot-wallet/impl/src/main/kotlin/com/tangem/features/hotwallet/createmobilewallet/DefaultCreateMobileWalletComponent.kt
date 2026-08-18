package com.tangem.features.hotwallet.createmobilewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetConfig
import com.tangem.features.hotwallet.createmobilewallet.ui.CreateMobileWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCreateMobileWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: CreateMobileWalletComponent.Params,
    private val importOptionsBottomSheetComponentFactory: ImportOptionsBottomSheetComponent.Factory,
) : CreateMobileWalletComponent, AppComponentContext by context {

    private val model: CreateMobileWalletModel = getOrCreateModel(params)

    private val importOptionsSlot = childSlot(
        source = model.importOptionsBottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "importOptionsSlot",
        childFactory = ::importOptionsChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val importOptions by importOptionsSlot.subscribeAsState()

        CreateMobileWalletContent(
            state = state,
            modifier = modifier,
        )

        importOptions.child?.instance?.BottomSheet()
    }

    @Suppress("UnusedPrivateMember")
    private fun importOptionsChild(
        config: ImportOptionsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = importOptionsBottomSheetComponentFactory.create(
        context = childByContext(componentContext),
        params = ImportOptionsBottomSheetComponent.Params(
            onRecoveryPhrase = model::onImportRecoveryPhrase,
            onCloudBackupsResolved = { model.onImportCloudBackupsResolved() },
            onDismiss = model::onImportBottomSheetDismiss,
        ),
    )

    @AssistedFactory
    interface Factory : CreateMobileWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CreateMobileWalletComponent.Params,
        ): DefaultCreateMobileWalletComponent
    }
}