package com.tangem.domain.cloudbackup.models

/**
 * Cloud account that currently owns the Tangem backup files.
 *
 * @property email       account email, shown so the user can tell which account the backups live in
 * @property displayName human-readable account name, if the provider exposes one
 * @property photoUrl    account avatar url, if any
 */
data class CloudBackupAccount(
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
)