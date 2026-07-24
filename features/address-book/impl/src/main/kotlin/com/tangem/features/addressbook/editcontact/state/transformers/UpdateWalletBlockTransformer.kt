package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class UpdateWalletBlockTransformer(
    private val walletName: String,
    private val isChangeable: Boolean,
    private val onClick: () -> Unit,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            walletBlock = EditContactUM.WalletBlockUM(
                walletName = walletName,
                isChangeable = isChangeable,
                onClick = onClick,
            ),
        )
    }
}