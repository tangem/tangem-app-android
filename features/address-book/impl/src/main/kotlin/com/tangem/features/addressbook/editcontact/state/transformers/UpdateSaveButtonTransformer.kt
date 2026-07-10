package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class UpdateSaveButtonTransformer(
    private val isEnabled: Boolean,
    private val isLoading: Boolean,
    private val isColdWallet: Boolean,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            saveButton = prevState.saveButton.copy(
                isEnabled = isEnabled,
                isLoading = isLoading,
                tangemIconUM = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24).takeIf { isColdWallet },
            ),
        )
    }
}