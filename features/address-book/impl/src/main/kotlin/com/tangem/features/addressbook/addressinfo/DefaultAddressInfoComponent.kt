package com.tangem.features.addressbook.addressinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.addressbook.addressinfo.model.AddressInfoModel
import com.tangem.features.addressbook.addressinfo.ui.AddressInfoBottomSheet

internal class DefaultAddressInfoComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: AddressInfoModel = getOrCreateModel(params)

    override fun dismiss() = params.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        AddressInfoBottomSheet(state = state, onDismiss = ::dismiss)
    }

    data class Params(
        val address: String,
        val networkCount: Int,
        val onEditAddress: () -> Unit,
        val onDeleteAddress: () -> Unit,
        val onDismiss: () -> Unit,
    )
}