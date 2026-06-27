package com.tangem.features.send.subcomponents.destination.model.converter

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.utils.converter.Converter

internal object ContactIconConverter : Converter<Contact, AccountIconUM.CryptoPortfolio> {

    override fun convert(value: Contact): AccountIconUM.CryptoPortfolio = AccountIconUM.CryptoPortfolio(
        value = CryptoPortfolioIcon.Icon.Letter,
        color = CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == value.iconColor }
            ?: CryptoPortfolioIcon.Color.Azure,
    )
}