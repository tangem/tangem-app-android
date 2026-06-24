package com.tangem.features.addressbook.block.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.block.state.ContactsBlockStateController
import com.tangem.features.addressbook.block.state.transformers.UpdateContactsBlockStateTransformer
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.features.addressbook.common.ContactMatcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class ContactsBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: ContactsBlockStateController,
    getContactsUseCase: GetContactsUseCase,
) : Model() {

    private val params = paramsContainer.require<AddressBookContactsBlockComponent.Params>()

    val state: StateFlow<ContactsBlockUM> get() = stateController.uiState

    init {
        params.queryFlow
            .flatMapLatest { query -> getContactsUseCase(query = query, userWalletId = params.userWalletId) }
            .onEach { contacts ->
                val matched = ContactMatcher.match(contacts = contacts, networkId = params.network.rawId)
                stateController.update(
                    UpdateContactsBlockStateTransformer(
                        matched = matched,
                        onSeeAllClick = params.onSeeAllClick,
                        onContactClick = params.onContactClick,
                    ),
                )
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }
}