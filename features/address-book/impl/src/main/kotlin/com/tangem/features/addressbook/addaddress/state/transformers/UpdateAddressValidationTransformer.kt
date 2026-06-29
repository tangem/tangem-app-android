package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.addaddress.state.transformers.converter.ChosenNetworkConverter
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Reflects the result of validating an address (and its memo) in the UI.
 *
 * [matchedBlockchains] are all supported networks the address resolves to. [displayedBlockchains] is what the network
 * block shows — all matched networks until the user narrows them down on the SelectNetworks screen, then the picked
 * subset. [selectedBlockchains] is what is actually chosen for saving (a single match is auto-selected; for several
 * matches the user must pick explicitly). While the address is blank or matches nothing the network selector stays
 * [ChosenNetworkStateUM.Hidden]; an invalid (non-empty, matching nothing) address surfaces the error in the field label.
 *
 * The confirm button is enabled only once at least one network is actually selected (and the memo, if any, is valid) —
 * showing the available networks is not the same as selecting them. The memo field is shown when a selected network
 * supports transaction extras; [isMemoInvalid] marks a malformed memo.
 */
internal class UpdateAddressValidationTransformer(
    private val address: String,
    private val matchedBlockchains: List<Blockchain>,
    private val displayedBlockchains: List<Blockchain>,
    private val selectedBlockchains: List<Blockchain>,
    private val isMemoInvalid: Boolean,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        val hasMatch = matchedBlockchains.isNotEmpty()
        val isError = address.isNotBlank() && !hasMatch

        val chosenNetworkState = if (hasMatch) {
            ChosenNetworkStateUM.Result(
                networkUMList = displayedBlockchains.map(ChosenNetworkConverter()::convert).toImmutableList(),
                // A single matched network leaves nothing to choose, so the selection screen is not opened.
                isClickable = matchedBlockchains.size > 1,
            )
        } else {
            ChosenNetworkStateUM.Hidden
        }

        val label = if (isError) {
            resourceReference(R.string.address_book_invalid_address_error)
        } else {
            resourceReference(R.string.common_address)
        }
        return prevState.copy(
            addressField = prevState.addressField.copy(isError = isError, label = label),
            chosenNetworkStateUM = chosenNetworkState,
            memoField = resolveMemoField(prevState.memoField),
            buttonUM = prevState.buttonUM.copy(isEnabled = selectedBlockchains.isNotEmpty() && !isMemoInvalid),
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