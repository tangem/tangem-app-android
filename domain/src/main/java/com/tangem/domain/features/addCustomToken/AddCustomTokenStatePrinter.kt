package com.tangem.domain.features.addCustomToken

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.form.FieldToJsonConverter
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.state.StatePrinter
import org.rekotlin.Action

class AddCustomTokenStatePrinter : StatePrinter<AddCustomTokenAction, AddCustomTokenState> {
    private val jsonConverter: MoshiJsonConverter = MoshiJsonConverter.INSTANCE
    private var builder: StringBuilder = StringBuilder()

    override fun print(action: Action, domainState: DomainState): String? {
        val action = (action as? AddCustomTokenAction) ?: return null
        val state = domainState.addCustomTokensState

        val fieldConverter = FieldToJsonConverter(listOf(
            CustomTokenFieldId.ContractAddress,
            CustomTokenFieldId.Network,
            CustomTokenFieldId.Name,
            CustomTokenFieldId.Symbol,
            CustomTokenFieldId.Decimals,
            CustomTokenFieldId.DerivationPath,
        ), jsonConverter)
        state.visitDataConverter(fieldConverter)
        val errors = state.formErrors.map {
            "${it.key}: ${it.value::class.java.simpleName}"
        }
        val warnings = state.warnings.map { it::class.java.simpleName }

        printAction(action, state)
        printStateValue("fields", fieldConverter.getConvertedData())
        printStateValue("fieldErrors", toJson(errors))
        printStateValue("warnings", toJson(warnings))
        printStateValue("screenState", toJson(state.screenState))
        printMessage("------------------------------------------------------")

        val printed = builder.toString()
        builder = StringBuilder()

        return printed
    }

    override fun getStateObject(domainState: DomainState): AddCustomTokenState = domainState.addCustomTokensState

    private fun printStateValue(name: String, value: String) {
        printMessage("$name: $value")
    }

    private fun printAction(action: AddCustomTokenAction, state: AddCustomTokenState) {
        printMessage("action: $action, state: ${state::class.java.simpleName}")
    }

    private fun toJson(value: Any): String {
        return jsonConverter.prettyPrint(value)
    }

    private fun printMessage(message: String) {
        builder.append("$message\n")
    }
}