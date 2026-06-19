package com.tangem.features.addressbook.list

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.addressbook.list.model.AddressBookListModel
import com.tangem.features.addressbook.list.ui.AddressBookEmptyScreen
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM

internal class DefaultAddressBookListComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: AddressBookListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        when (val addressBookListUM = state) {
            is AddressBookListUM.Empty -> AddressBookEmptyScreen(
                onAddContactClick = addressBookListUM.onAddClick,
                onBackClick = router::pop,
                modifier = modifier.background(TangemTheme.colors3.bg.primary),
            )
            is AddressBookListUM.AddressList -> TODO("[REDACTED_TASK_KEY]")
        }
    }

    data class Params(
        val onContactClick: (String) -> Unit,
        val onAddContactClick: () -> Unit,
    )
}