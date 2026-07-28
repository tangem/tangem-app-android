package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.addaddress.state.transformers.converter.ChosenNetworkConverter
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Reflects the result of validating an address (and its memo) in the UI.
 *
 * [matchedBlockchains] are all supported networks the address resolves to. [selectedBlockchains] is what is actually
 * chosen for saving (a single match is auto-selected; for several matches the user must pick explicitly on the
 * SelectNetworks screen). The network block shows only the selected networks — while several match but none is picked
 * yet it prompts with [ChosenNetworkStateUM.SelectNetwork] rather than showing every matched network. While the address
 * is blank or matches nothing the network selector stays [ChosenNetworkStateUM.Hidden]; an invalid (non-empty, matching
 * nothing) address surfaces the error in the field label.
 *
 * The confirm button is enabled only once at least one network is actually selected (and the memo, if any, is valid).
 * The memo field is shown when a selected network supports transaction extras; [isMemoInvalid] marks a malformed memo.
 */
internal class UpdateAddressValidationTransformer(
    private val address: String,
    private val matchedBlockchains: List<Blockchain>,
    private val selectedBlockchains: List<Blockchain>,
    private val isMemoInvalid: Boolean,
    private val duplicateName: String?,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        val hasMatch = matchedBlockchains.isNotEmpty()
        val isInvalidAddress = address.isNotBlank() && !hasMatch
        val isDuplicate = duplicateName != null
        val isError = isInvalidAddress || isDuplicate

        val chosenNetworkState = when {
            !hasMatch -> ChosenNetworkStateUM.Hidden
            selectedBlockchains.isEmpty() -> ChosenNetworkStateUM.SelectNetwork
            else -> ChosenNetworkStateUM.Result(
                networkUMList = selectedBlockchains.map(ChosenNetworkConverter()::convert).toImmutableList(),
                // A single matched network leaves nothing to choose, so the selection screen is not opened.
                isClickable = matchedBlockchains.size > 1,
            )
        }

        val label = when {
            isDuplicate -> resourceReference(R.string.address_book_address_taken_error, wrappedList(duplicateName))
            isInvalidAddress -> resourceReference(R.string.address_book_invalid_address_error)
            else -> resourceReference(R.string.common_address)
        }
        val isConfirmEnabled = selectedBlockchains.isNotEmpty() && !isMemoInvalid && !isDuplicate
        return prevState.copy(
            addressField = prevState.addressField.copy(isError = isError, label = label),
            chosenNetworkStateUM = chosenNetworkState,
            memoField = resolveMemoField(prevState.memoField),
            buttonUM = prevState.buttonUM.copy(isEnabled = isConfirmEnabled),
        )
    }

    /**
     * Shows the memo field with the right label when a chosen network supports transaction extras; hides it and clears
     * the value otherwise (e.g. the supporting network was deselected or the address changed). A malformed memo
     * ([isMemoInvalid]) turns the field label into an error.
     */
    private fun resolveMemoField(prevMemoField: AddAddressUM.MemoFieldUM): AddAddressUM.MemoFieldUM {
        val extrasType = selectedBlockchains
            .map { it.getSupportedTransactionExtras() }
            .firstOrNull { it.isTxExtrasSupported() }
            ?: return prevMemoField.copy(isVisible = false, value = "", isError = false)

        val fieldLabelRes = when (extrasType) {
            Network.TransactionExtrasType.DESTINATION_TAG -> R.string.send_destination_tag_field
            else -> R.string.send_extras_hint_memo
        }
        val labelRes = if (isMemoInvalid) R.string.send_memo_destination_tag_error else fieldLabelRes
        return prevMemoField.copy(
            isVisible = true,
            label = resourceReference(labelRes),
            isError = isMemoInvalid,
        )
    }
}