package com.tangem.features.addressbook.addressinfo.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.addressbook.addressinfo.DefaultAddressInfoComponent
import com.tangem.features.addressbook.addressinfo.ui.state.AddressInfoUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressInfoModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val messageSender: UiMessageSender,
) : Model() {

    private val params: DefaultAddressInfoComponent.Params = paramsContainer.require()

    val state: StateFlow<AddressInfoUM> = MutableStateFlow(
        AddressInfoUM(
            address = params.address,
            networkCount = params.networkCount,
            onCopy = ::onCopy,
            onEditAddress = params.onEditAddress,
            onDeleteAddress = params.onDeleteAddress,
        ),
    )

    private fun onCopy() {
        clipboardManager.setText(text = params.address, isSensitive = false)
        messageSender.send(SnackbarMessage(resourceReference(R.string.address_book_address_copied)))
        params.onDismiss()
    }
}