package com.tangem.features.addressbook.editcontact.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.common.AddressBookResultHolder
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.state.EditContactStateController
import com.tangem.features.addressbook.editcontact.state.transformers.AddValidatedAddressTransformer
import com.tangem.features.addressbook.editcontact.state.transformers.SelectContactColorTransformer
import com.tangem.features.addressbook.editcontact.state.transformers.UpdateContactNameTransformer
import com.tangem.features.addressbook.editcontact.state.transformers.UpdateEditContactInitialStateTransformer
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class EditContactModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: EditContactStateController,
    private val resultHolder: AddressBookResultHolder,
) : Model() {

    private val params: DefaultEditContactComponent.Params = paramsContainer.require()

    val state: StateFlow<EditContactUM> get() = stateController.uiState

    init {
        updateInitialState()
        prefillPredefinedAddress()
        subscribeToConfirmedAddresses()
    }

    /** In WithContactCreation mode the contact opens with the already-known address attached. */
    private fun prefillPredefinedAddress() {
        params.predefinedAddress?.let(::addAddress)
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateEditContactInitialStateTransformer(
                isExistingContact = params.contactId != null,
                onNameChange = ::onNameChange,
                onColorSelect = ::onColorSelect,
                onCloseClick = params.onBackClick,
                onAddAddressClick = params.onAddAddressClick,
            ),
        )
    }

    private fun subscribeToConfirmedAddresses() {
        resultHolder.confirmedAddress
            .filterNotNull()
            .onEach { address ->
                addAddress(address)
                resultHolder.clear()
            }
            .launchIn(modelScope)
    }

    private fun onNameChange(name: String) {
        stateController.update(UpdateContactNameTransformer(name = name))
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        stateController.update(SelectContactColorTransformer(color = color))
    }

    private fun addAddress(address: ValidatedAddress) {
        stateController.update(AddValidatedAddressTransformer(address = address))
    }
}