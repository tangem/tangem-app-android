package com.tangem.google.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.google.GoogleServicesHelper
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [GoogleIdTokenProvider] backed by Credential Manager and the Sign in with Google credential option.
 */
@Singleton
internal class CredentialManagerGoogleIdTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    googleAuthConfig: GoogleAuthConfig,
    private val foregroundActivityProvider: ForegroundActivityProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : GoogleIdTokenProvider {

    private val webClientId: String? = googleAuthConfig.webClientId

    private val logger = TangemLogger.withTag(TAG)

    private val credentialManager by lazy { CredentialManager.create(context) }

    /** Without a web client id there is no audience to issue the token for, so the flow cannot finish */
    override suspend fun isAvailable(): Boolean {
        if (webClientId.isNullOrEmpty()) return false

        return withContext(dispatchers.io) { GoogleServicesHelper.checkGoogleServicesAvailability(context) }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getIdToken(nonce: String?): Either<GoogleIdTokenError, GoogleIdToken> {
        if (!isAvailable()) {
            logger.e("Google sign-in is unavailable: no Google services or no web client id")
            return GoogleIdTokenError.Unavailable.left()
        }

        val serverClientId = webClientId ?: return GoogleIdTokenError.Unavailable.left()

        val option = GetSignInWithGoogleOption.Builder(serverClientId)
            .apply { if (nonce != null) setNonce(nonce) }
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            withContext(dispatchers.main) {
                // read on the main thread right before the call, so the activity cannot go away in between
                val activity = foregroundActivityProvider.foregroundActivity
                    ?: return@withContext GoogleIdTokenError.Unknown(NoForegroundActivityException()).left()

                val response = credentialManager.getCredential(context = activity, request = request)
                extractGoogleIdToken(response.credential)
                    .onLeft { logger.e("Google id token extraction failed: $it") }
            }
        } catch (e: GetCredentialException) {
            logger.e("Google sign-in failed: type=${e.type}")
            googleIdTokenErrorForException(e).left()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("Google sign-in unexpected error", e)
            GoogleIdTokenError.Unknown(e).left()
        }
    }

    override suspend fun signOut() {
        try {
            withContext(dispatchers.main) {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: ClearCredentialException) {
            logger.e("Google credential state clear failed: type=${e.type}")
        }
    }

    private companion object {
        const val TAG = "CredentialManagerGoogleIdTokenProvider"
    }
}

internal class NoForegroundActivityException : IllegalStateException("No foreground activity to show Google sign-in")

internal fun extractGoogleIdToken(credential: Credential): Either<GoogleIdTokenError, GoogleIdToken> {
    val isGoogleIdToken = credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    if (!isGoogleIdToken) return GoogleIdTokenError.NoCredential.left()

    return try {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        GoogleIdToken(value = googleCredential.idToken, accountName = googleCredential.id).right()
    } catch (e: GoogleIdTokenParsingException) {
        GoogleIdTokenError.Unknown(e).left()
    }
}

internal fun googleIdTokenErrorForException(e: GetCredentialException): GoogleIdTokenError = when (e) {
    is GetCredentialCancellationException -> GoogleIdTokenError.Canceled
    is NoCredentialException -> GoogleIdTokenError.NoCredential
    is GetCredentialProviderConfigurationException,
    is GetCredentialUnsupportedException,
    -> GoogleIdTokenError.Unavailable
    else -> GoogleIdTokenError.Unknown(e)
}