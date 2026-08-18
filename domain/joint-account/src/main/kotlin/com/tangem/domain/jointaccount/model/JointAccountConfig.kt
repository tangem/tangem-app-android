package com.tangem.domain.jointaccount.model

/**
 * Shared account config, immutable once the account exists — part of every signed payload: creation and joining
 * carry it as is, activation extends it with the resolved Safe address.
 *
 * @property name         account name shared by all participants, 1..20 chars
 * @property icon         icon name, from the same set as crypto accounts
 * @property iconColor    color name, from the same set as crypto accounts
 * @property membersCount slots including the creator, 2..5
 * @property threshold    signatures required to execute an operation, 1..[membersCount]
 */
data class JointAccountConfig(
    val name: String,
    val icon: String,
    val iconColor: String,
    val membersCount: Int,
    val threshold: Int,
) {

    internal fun toCanonicalMap(): Map<String, Any> = mapOf(
        "name" to name,
        "icon" to icon,
        "iconColor" to iconColor,
        "membersCount" to membersCount,
        "threshold" to threshold,
    )
}