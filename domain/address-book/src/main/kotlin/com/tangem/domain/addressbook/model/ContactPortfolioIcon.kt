package com.tangem.domain.addressbook.model

import com.tangem.domain.models.account.CryptoPortfolioIcon

/**
 * The contact avatar as a typed [CryptoPortfolioIcon].
 *
 * [Contact.icon] and [Contact.iconColor] are stored as plain enum names inside the encrypted payload, so a value
 * written by another (or a future) client can be unknown here — it falls back to the letter monogram in the default
 * color instead of failing to render the contact.
 */
fun Contact.portfolioIcon(): CryptoPortfolioIcon = CryptoPortfolioIcon.ofCustomAccount(
    value = CryptoPortfolioIcon.Icon.entries.firstOrNull { it.name == icon } ?: CryptoPortfolioIcon.Icon.Letter,
    color = CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == iconColor } ?: CryptoPortfolioIcon.Color.Azure,
)