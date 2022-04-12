package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.Field
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.AddCustomTokenError
import com.tangem.domain.features.addCustomToken.AddCustomTokenWarning
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId
import com.tangem.network.api.tangemTech.Coins
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenAction : Action {
    // from user, ui
    object OnCreate : AddCustomTokenAction()
    object OnDestroy : AddCustomTokenAction()
    data class OnTokenFieldChanged(val id: FieldId, val value: Field.Data<String>) : AddCustomTokenAction()
    data class OnTokenContractAddressChanged(val value: Field.Data<String>) : AddCustomTokenAction()
    data class OnTokenNetworkChanged(val value: Field.Data<Blockchain>) : AddCustomTokenAction()
    data class OnTokenDerivationPathChanged(val value: Field.Data<Blockchain>) : AddCustomTokenAction()
    data class OnTokenDecimalsChanged(val value: Field.Data<String>) : AddCustomTokenAction()
    data class OnCustomTokenSelected(val any: Any = Unit) : AddCustomTokenAction()


    data class UpdateForm(val state: AddCustomTokenState) : AddCustomTokenAction()
    data class FillTokenFields(
        val token: Coins.CheckAddressResponse.Token,
        val contract: Coins.CheckAddressResponse.Token.Contract,
    ) : AddCustomTokenAction()

    sealed class Error : AddCustomTokenAction() {
        data class Add(val id: CustomTokenFieldId, val error: AddCustomTokenError) : Error()
        data class Remove(val id: CustomTokenFieldId) : Error()
    }

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