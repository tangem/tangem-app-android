package com.tangem.features.addressbook.block.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.usecase.SyncAddressBooksUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.block.state.ContactsBlockStateController
import com.tangem.features.addressbook.block.state.transformers.UpdateContactsBlockStateTransformer
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
import com.tangem.features.addressbook.common.ContactMatcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
@Suppress("LongParameterList")
internal class ContactsBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: ContactsBlockStateController,
    private val analyticsSender: AddressBookAnalyticsSender,
    private val syncAddressBooksUseCase: SyncAddressBooksUseCase,
    getVerifiedContactsInteractor: GetVerifiedContactsInteractor,
    getWalletsUseCase: GetWalletsUseCase,
) : Model() {

    private var isWidgetShownReported = false
    private val params = paramsContainer.require<AddressBookContactsBlockComponent.Params>()
    val state: StateFlow<ContactsBlockUM> get() = stateController.uiState

    init {
        modelScope.launch(context = dispatchers.default) { syncAddressBooksUseCase() }

        combine(
            params.queryFlow.flatMapLatest { query ->
                getVerifiedContactsInteractor.getVerifiedContacts(query = query, userWalletId = null)
            },
            getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = false, filterLocked = true),
        ) { contacts, wallets -> contacts to wallets.values.toList() }
            .onEach { (contacts, wallets) ->
                val matched = ContactMatcher.match(contacts = contacts, networkId = params.network.rawId)
                reportWidgetShownIfNeeded(matched.isNotEmpty())
                stateController.update(
                    UpdateContactsBlockStateTransformer(
                        matched = matched,
                        walletNamesById = wallets.associate { it.walletId.stringValue to it.name },
                        shouldShowWalletName = matched.mapTo(HashSet()) { it.walletId }.size > 1,
                        onSeeAllClick = params.onSeeAllClick,
                        onContactClick = ::onContactClick,
                    ),
                )
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun reportWidgetShownIfNeeded(isVisible: Boolean) {
        if (isVisible && !isWidgetShownReported) {
            isWidgetShownReported = true
            analyticsSender.sendSendFlowWidgetShown(scope = modelScope)
        }
    }

    private fun onContactClick(contact: MatchedContact) {
        analyticsSender.sendContactSelectedInSend(contactId = contact.contactId, scope = modelScope)
        params.onContactClick(contact)
    }
}