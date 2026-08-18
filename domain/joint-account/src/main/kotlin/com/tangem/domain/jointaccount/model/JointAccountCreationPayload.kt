package com.tangem.domain.jointaccount.model

/**
 * The object signed on joint account creation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts`.
 * Strings go in exactly as the user entered them, never normalized or trimmed.
 */
data class JointAccountCreationPayload(
    val config: Config,
    val creator: Creator,
) {

    /**
     * @property name         account name shared by all participants, 1..20 chars
     * @property icon         icon name, from the same set as crypto accounts
     * @property iconColor    color name, from the same set as crypto accounts
     * @property membersCount slots including the creator, 2..5
     * @property threshold    signatures required to execute an operation, 1..[membersCount]
     */
    data class Config(
        val name: String,
        val icon: String,
        val iconColor: String,
        val membersCount: Int,
        val threshold: Int,
    )

    /**
     * @property walletId   64 hex chars, must equal the `walletId` of the request path — the signature is bound
     * to the wallet on purpose
     * @property name       the creator's display name, 1..25 chars
     * @property address    owner address derived at `m/44'/60'/888888'/0/{derivation}`, EIP-55 checksummed (EVM)
     * @property derivation derivation index of the owner key
     */
    data class Creator(
        val walletId: String,
        val name: String,
        val address: String,
        val derivation: Int,
    )

    fun toCanonicalMap(): Map<String, Any> = mapOf(
        "config" to mapOf(
            "name" to config.name,
            "icon" to config.icon,
            "iconColor" to config.iconColor,
            "membersCount" to config.membersCount,
            "threshold" to config.threshold,
        ),
        "creator" to mapOf(
            "walletId" to creator.walletId,
            "name" to creator.name,
            "address" to creator.address,
            "derivation" to creator.derivation,
        ),
    )
}