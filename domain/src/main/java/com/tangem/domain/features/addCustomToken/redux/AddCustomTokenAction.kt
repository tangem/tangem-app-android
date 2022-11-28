package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.form.Field
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId
import com.tangem.datasource.api.tangemTech.CoinsResponse
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 23/09/2021.
 */
sealed class AddCustomTokenAction : Action {
    sealed class Init : AddCustomTokenAction() {
        data class SetAddedCurrencies(val addedCurrencies: List<DomainWrapped.Currency>) : AddCustomTokenAction()
        data class SetOnAddTokenCallback(val callback: (CustomCurrency) -> Unit) : AddCustomTokenAction()
    }

    object OnCreate : AddCustomTokenAction() {
        data class SetDerivationStyle(val derivationStyle: DerivationStyle?) : AddCustomTokenAction()
    }

    object OnDestroy : AddCustomTokenAction()

    // from user, ui
    data class OnTokenContractAddressChanged(val contractAddress: Field.Data<String>) : AddCustomTokenAction()
    data class OnTokenNetworkChanged(val blockchainNetwork: Field.Data<Blockchain>) : AddCustomTokenAction()
    data class OnTokenNameChanged(val tokenName: Field.Data<String>) : AddCustomTokenAction()
    data class OnTokenSymbolChanged(val tokenSymbol: Field.Data<String>) : AddCustomTokenAction()
    data class OnTokenDerivationPathChanged(val blockchainDerivationPath: Field.Data<Blockchain>) : AddCustomTokenAction()
    data class OnTokenDecimalsChanged(val tokenDecimals: Field.Data<String>) : AddCustomTokenAction()
    object OnAddCustomTokenClicked : AddCustomTokenAction()

    data class SetFoundTokenInfo(val foundToken: CoinsResponse.Coin?) : AddCustomTokenAction()

    // form fields
    data class UpdateForm(val state: AddCustomTokenState) : AddCustomTokenAction()

    sealed class FieldError : AddCustomTokenAction() {
        data class Add(val id: CustomTokenFieldId, val error: AddCustomTokenError) : FieldError()
        data class Remove(val id: CustomTokenFieldId) : FieldError()
    }

    // warnings
    sealed class Warning : AddCustomTokenAction() {
        data class Add(val warnings: Set<AddCustomTokenError.Warning>) : Warning()
        data class Remove(val warnings: Set<AddCustomTokenError.Warning>) : Warning()
        data class Replace(
            val remove: Set<AddCustomTokenError.Warning>,
            val add: Set<AddCustomTokenError.Warning>
        ) : Warning()
    }

    // To change the screenState
    sealed class Screen : AddCustomTokenAction() {
        data class UpdateTokenFields(val pairs: List<Pair<FieldId, ViewStates.TokenField>>) : Screen()
        data class UpdateAddButton(val addButton: ViewStates.AddButton) : Screen()
    }
}
