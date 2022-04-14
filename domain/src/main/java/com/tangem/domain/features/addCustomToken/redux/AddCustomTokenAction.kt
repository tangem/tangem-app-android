package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.common.form.Field
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.AddCustomTokenError
import com.tangem.domain.features.addCustomToken.AddCustomTokenWarning
import com.tangem.domain.features.addCustomToken.CompleteData
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId
import com.tangem.network.api.tangemTech.Coins
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenAction : Action {
    sealed class Init : AddCustomTokenAction() {
        data class SetAddedCurrencies(val addedCurrencies: AddedCurrencies) : AddCustomTokenAction()

        data class SetOnAddTokenCallback(val callback: (CompleteData) -> Unit) : AddCustomTokenAction()
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

    // form fields
    data class UpdateForm(val state: AddCustomTokenState) : AddCustomTokenAction()
    object ClearTokenFields : AddCustomTokenAction()

    data class FillTokenFields(
        val token: Coins.CheckAddressResponse.Token,
        val contract: Coins.CheckAddressResponse.Token.Contract,
    ) : AddCustomTokenAction()

    sealed class FieldError : AddCustomTokenAction() {
        data class Add(val id: CustomTokenFieldId, val error: AddCustomTokenError) : FieldError()
        data class Remove(val id: CustomTokenFieldId) : FieldError()
    }

    data class SetTokenId(val id: String) : AddCustomTokenAction()

    // warnings
    sealed class Warning : AddCustomTokenAction() {
        data class Add(val warnings: Set<AddCustomTokenWarning>) : Warning()
        data class Remove(val warnings: Set<AddCustomTokenWarning>) : Warning()
        data class Replace(val remove: Set<AddCustomTokenWarning>, val add: Set<AddCustomTokenWarning>) : Warning()
    }

    // To change the screenState
    sealed class Screen : AddCustomTokenAction() {
        data class UpdateTokenFields(val pairs: List<Pair<FieldId, ViewStates.TokenField>>) : Screen()
        data class UpdateAddButton(val addButton: ViewStates.AddButton) : Screen()
    }
}