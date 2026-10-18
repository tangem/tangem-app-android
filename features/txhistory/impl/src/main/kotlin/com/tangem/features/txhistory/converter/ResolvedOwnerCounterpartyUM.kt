package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner

/** The "From …" label of the counterparty card when the transfer came in from this owner. */
@get:StringRes
internal val ResolvedOwner.incomingLabelRes: Int
    get() = when (this) {
        is ResolvedOwner.OwnAccount, is ResolvedOwner.OwnPaymentAccount -> R.string.common_from_account
        is ResolvedOwner.OwnWallet -> R.string.common_from_wallet
        is ResolvedOwner.SavedContact, is ResolvedOwner.External -> R.string.common_from_address
    }

/**
 * Counterparty card of a resolved owner: an own account, own wallet or saved contact renders its name and avatar,
 * anything else the full external address with an identicon.
 */
internal fun ResolvedOwner.toCounterpartyUM(label: TextReference, onCopyClick: () -> Unit) =
    TxHistoryDetailsUM.CounterpartyUM(
        label = label,
        title = when (this) {
            is ResolvedOwner.OwnAccount -> TxHistoryDetailsUM.CounterpartyTitle.Name(account.accountName.toUM().value)
            is ResolvedOwner.OwnPaymentAccount ->
                TxHistoryDetailsUM.CounterpartyTitle.Name(account.accountName.toUM().value)
            is ResolvedOwner.OwnWallet -> TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference(walletInfo.name))
            is ResolvedOwner.SavedContact ->
                TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference(contact.name.value))
            is ResolvedOwner.External -> TxHistoryDetailsUM.CounterpartyTitle.Address(address)
        },
        avatar = when (this) {
            is ResolvedOwner.OwnAccount -> TxHistoryDetailsUM.CounterpartyAvatar.Account(
                name = account.accountName.toUM().value,
                icon = CryptoPortfolioIconConverter.convert(account.icon),
            )
            is ResolvedOwner.OwnPaymentAccount -> TxHistoryDetailsUM.CounterpartyAvatar.PaymentAccount
            is ResolvedOwner.OwnWallet -> TxHistoryDetailsUM.CounterpartyAvatar.Wallet(
                deviceIconUM = walletInfo.deviceIconUM,
            )
            is ResolvedOwner.SavedContact -> TxHistoryDetailsUM.CounterpartyAvatar.Contact(
                name = contact.name.value,
                icon = CryptoPortfolioIconConverter.convert(contact.portfolioIcon()),
            )
            is ResolvedOwner.External -> TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = address)
        },
        onCopyClick = onCopyClick,
    )