package com.tangem.core.analytics.models

/**
 * An event about a wallet that is not the selected one by the time it is sent: deleted, logged out or replaced by
 * a card. The selected wallet's context would describe another wallet, or nothing at all when none is left, so
 * such an event names its product type itself and the context params are not added to it.
 */
interface DetachedWalletEvent