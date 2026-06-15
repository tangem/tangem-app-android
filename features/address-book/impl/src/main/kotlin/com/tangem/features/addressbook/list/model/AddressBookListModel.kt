package com.tangem.features.addressbook.list.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.R.drawable.ic_plus_24
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.addressbook.list.AddressBookListComponent
import com.tangem.features.addressbook.list.contract.AddressBookListUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressBookListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<AddressBookListComponent.Params>()

    val state: StateFlow<AddressBookListUM> = MutableStateFlow(
        AddressBookListUM.Empty(
            tangemButtonUM = TangemButtonUM(
                text = TextReference.Res(R.string.address_book_new_contact),
                tangemIconUM = TangemIconUM.Icon(
                    iconRes = ic_plus_24,
                    tintReference = { TangemTheme.colors3.text.inverse.primary },
                ),
                iconPosition = TangemButtonIconPosition.End,
                type = TangemButtonType.Primary,
                onClick = params.onAddContactClick,
            ),
        ),
    )
}