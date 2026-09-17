package com.tangem.data.cloudbackup.repository

import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.domain.cloudbackup.repository.BackupPasswordRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * [BackupPasswordRepository] backed by the Jetpack [CredentialManager], which fans the request out to
 * whatever credential providers the device has (Google Password Manager, a third-party manager).
 *
 * The provider UI is shown by the foreground activity, so a request made while none is around is
 * reported as "not stored" rather than crashing. The password never reaches the logs.
 */
internal class CredentialManagerPasswordRepository(
    private val credentialManager: CredentialManager,
    private val foregroundActivityProvider: ForegroundActivityProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : BackupPasswordRepository {

    private val logger = TangemLogger.withTag(TAG)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun savePassword(username: String, password: CharArray): Boolean {
        return withContext(dispatchers.main) {
            // read on the main thread right before the call: an activity resolved earlier can be gone by
            // the time the coroutine gets dispatched here, and the provider would then get a stale one
            val activity = foregroundActivityProvider.foregroundActivity
            if (activity == null) {
                logger.e("Cannot offer to save the password: no foreground activity")
                return@withContext false
            }

            try {
                credentialManager.createCredential(
                    context = activity,
                    // the request only takes a String, so this copy of the password cannot be wiped
                    request = CreatePasswordRequest(id = username, password = String(password)),
                )
                true
            } catch (e: CreateCredentialCancellationException) {
                logger.i("The user declined saving the password")
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // no provider on the device, saving turned off, provider failure — none of it is fatal.
                // Caught broadly on purpose: a device without Google services fails inside the provider
                // implementation, which is free to throw something other than a CreateCredentialException.
                logger.e("Saving the password to the password manager failed", e)
                false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getPassword(): CharArray? {
        return withContext(dispatchers.main) {
            val activity = foregroundActivityProvider.foregroundActivity
            if (activity == null) {
                logger.e("Cannot read the password: no foreground activity")
                return@withContext null
            }

            try {
                val response = credentialManager.getCredential(
                    context = activity,
                    request = GetCredentialRequest(credentialOptions = listOf(GetPasswordOption())),
                )
                (response.credential as? PasswordCredential)?.password?.toCharArray()
            } catch (e: NoCredentialException) {
                logger.i("The password manager holds no password for the app")
                null
            } catch (e: GetCredentialCancellationException) {
                logger.i("The user dismissed the password picker")
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("Reading the password from the password manager failed", e)
                null
            }
        }
    }

    private companion object {
        const val TAG = "BackupPassword"
    }
}