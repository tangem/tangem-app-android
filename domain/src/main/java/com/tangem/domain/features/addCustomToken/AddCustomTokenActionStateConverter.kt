package com.tangem.domain.features.addCustomToken

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.form.FieldToJsonConverter
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.state.StringActionConverter
import org.rekotlin.Action

class AddCustomTokenActionStateConverter : StringActionConverter<DomainState> {
    private val jsonConverter: MoshiJsonConverter = MoshiJsonConverter.INSTANCE
    private var builder: StringBuilder = StringBuilder()

    override fun convert(action: Action, stateHolder: DomainState): String? {
        val action = (action as? AddCustomTokenAction) ?: return null

        val state = stateHolder.addCustomTokensState
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