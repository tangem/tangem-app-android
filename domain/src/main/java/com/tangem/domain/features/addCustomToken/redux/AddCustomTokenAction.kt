package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.AddCustomTokenError
import com.tangem.domain.features.addCustomToken.AddCustomTokenWarning
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId
import com.tangem.network.api.tangemTech.CoinsCheckAddressResponse
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenAction : Action {
    // initializing actions
    data class SetTangemTechAuthHeader(val cardPublicKeyHex: String) : AddCustomTokenAction()


    // from user, ui
    object OnBackPressed : AddCustomTokenAction()
    data class OnTokenContractAddressChanged(val value: String) : AddCustomTokenAction()
    data class OnTokenNetworkChanged(val value: Blockchain) : AddCustomTokenAction()
    data class OnTokenDerivationPathChanged(val value: String) : AddCustomTokenAction()
    data class OnTokenFieldChanged(val id: FieldId, val value: String) : AddCustomTokenAction()


    // from redux
    object UpdateForm : AddCustomTokenAction()
    data class FillTokenFields(
        val token: CoinsCheckAddressResponse.Token,
        val contract: CoinsCheckAddressResponse.Token.Contract,
    ) : AddCustomTokenAction()

    sealed class Error : AddCustomTokenAction() {
        data class Add(val id: CustomTokenFieldId, val error: AddCustomTokenError) : Error()
        data class Remove(val id: CustomTokenFieldId) : Error()
    }

    sealed class Warning : AddCustomTokenAction() {
        data class Add(val warning: AddCustomTokenWarning) : Warning()
        data class Remove(val warning: AddCustomTokenWarning) : Warning()
    }
}