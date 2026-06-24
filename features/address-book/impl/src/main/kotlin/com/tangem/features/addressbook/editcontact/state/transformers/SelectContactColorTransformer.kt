package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class SelectContactColorTransformer(
    private val color: CryptoPortfolioIcon.Color,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            colors = prevState.colors.copy(selected = color),
            portfolioIcon = prevState.portfolioIcon.copy(color = color),
        )
    }
}