package com.tangem.feature.wallet.presentation.wallet.state.model

/**
 * The pending moment when the promo cards hidden by a system card may come back (TWI-1841). Outside these
 * moments they can only be hidden, so a card that comes and goes does not make the promo cards blink.
 */
internal enum class PromoCardsCheck {

    None,

    /** The wallet page was opened: decided once its cards are built from a resolved balance, cache included */
    OnOpen,

    /** A refresh finished: decided once the refreshed balance reaches the cards, not at the end of the spinner */
    AfterRefresh,
}