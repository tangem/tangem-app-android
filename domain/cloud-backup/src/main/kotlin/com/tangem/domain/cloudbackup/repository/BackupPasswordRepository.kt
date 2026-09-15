package com.tangem.domain.cloudbackup.repository

/**
 * Storage of the cloud backup password in the device password manager — the system one or a third-party
 * provider, whichever the device has.
 */
interface BackupPasswordRepository {

    /**
     * Offers to store [password] under [username], showing the provider's own confirmation UI.
     *
     * Storing is never required for the backup flow to succeed: the user may decline it, the device may
     * have no provider at all, or the user may have password saving turned off. All of those return
     * `false` instead of failing, so a caller can go on regardless.
     *
     * @param username identifies the entry in the manager, and is what the user picks it by when several
     * entries are stored for the app.
     */
    suspend fun savePassword(username: String, password: CharArray): Boolean

    /**
     * Counterpart of [savePassword], asked on the same channel: waiting for the autofill provider to
     * volunteer a suggestion instead would work only on the devices whose provider bothers to.
     *
     * `null` for everything that is not a password to work with — nothing stored, no provider, the user
     * dismissed the picker. The returned array is the caller's to wipe.
     */
    suspend fun getPassword(): CharArray?
}