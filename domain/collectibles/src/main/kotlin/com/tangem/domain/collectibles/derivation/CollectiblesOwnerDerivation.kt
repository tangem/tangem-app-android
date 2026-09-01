package com.tangem.domain.collectibles.derivation

/**
 * The derivation path of the gacha vault owner key.
 *
 * Every node is hardened on purpose: the key is an ed25519 (SLIP-0010) one, and that curve cannot derive a
 * non-hardened node at all, so a path like `m/44'/60'/999700'/0/0` would yield no key and no address.
 *
 * `999700'` continues the app's range of reserved keys (`999999'` Visa, `999998'` virtual account, `999997'`
 * Polymarket owner, `888888'` joint account owners) rather than naming a chain account.
 *
 * Must match iOS byte-for-byte — the vault address is `PDA(["vault", owner])`, so a different path means a
 * different vault for the same wallet. Pinned by a unit test.
 */
const val COLLECTIBLES_OWNER_DERIVATION_PATH: String = "m/44'/60'/999700'/0'/0'"