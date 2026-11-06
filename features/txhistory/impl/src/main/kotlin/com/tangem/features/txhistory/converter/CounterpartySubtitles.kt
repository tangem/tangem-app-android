package com.tangem.features.txhistory.converter

import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.txhistory.model.ResolvedOwner

/**
 * How a resolved counterparty is named in a row subtitle, shared by every row that names one — the history rows, the
 * joint cosign requests — so the same owner always reads the same way.
 */
internal fun ResolvedOwner.toSubtitle(direction: ContentSubtitle.Direction): ContentSubtitle = when (this) {
    is ResolvedOwner.OwnAccount -> ContentSubtitle.OwnAccount(
        direction = direction,
        accountName = account.accountName.toUM().value,
        avatar = account.icon.toPortfolioAvatar(name = account.accountName.toUM().value),
    )
    is ResolvedOwner.OwnPaymentAccount -> ContentSubtitle.OwnPaymentAccount(
        direction = direction,
        accountName = account.accountName.toUM().value,
    )
    is ResolvedOwner.OwnMoneyAccount -> ContentSubtitle.OwnAccount(
        direction = direction,
        accountName = moneyAccountOwnerName,
        avatar = moneyAccountOwnerIcon.toPortfolioAvatar(name = moneyAccountOwnerName),
    )
    is ResolvedOwner.OwnWallet -> ContentSubtitle.OwnWallet(
        direction = direction,
        walletName = walletInfo.name,
        deviceIconUM = walletInfo.deviceIconUM,
    )
    is ResolvedOwner.SavedContact -> ContentSubtitle.Contact(
        direction = direction,
        contactName = contact.name.value,
        avatar = contact.toContactAvatar(),
    )
    is ResolvedOwner.External -> ContentSubtitle.ExternalAddress(direction = direction, rawAddress = address)
}

/**
 * A portfolio still on the default `Letter` icon renders the monogram of [name]; an icon picked in the account or
 * contact editor renders as its own drawable. The same rule the design-system `AccountIcon` applies wherever a
 * portfolio avatar is drawn full-size — here for the inline row avatar, which has its own container.
 *
 * The letter itself is left to the composition: [name] may be a string resource (the main account's title), so its
 * first character is only known once the reference resolves.
 */
internal fun CryptoPortfolioIcon.toPortfolioAvatar(name: TextReference): ContentSubtitle.PortfolioAvatar =
    if (value == CryptoPortfolioIcon.Icon.Letter) {
        ContentSubtitle.PortfolioAvatar.Monogram(name = name, color = color.getUiColor())
    } else {
        ContentSubtitle.PortfolioAvatar.Resource(resId = value.getResId(), color = color.getUiColor())
    }

/**
 * A contact's name is a plain string, so an empty one is caught here rather than rendering a blank circle: it falls
 * back to the letter drawable, as it did before the monogram existed.
 */
internal fun Contact.toContactAvatar(): ContentSubtitle.PortfolioAvatar {
    val icon = portfolioIcon()

    return if (name.value.isEmpty()) {
        ContentSubtitle.PortfolioAvatar.Resource(resId = icon.value.getResId(), color = icon.color.getUiColor())
    } else {
        icon.toPortfolioAvatar(name = stringReference(name.value))
    }
}