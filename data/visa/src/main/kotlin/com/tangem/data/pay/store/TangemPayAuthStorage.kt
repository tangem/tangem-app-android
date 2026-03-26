package com.tangem.data.pay.store

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.payment.auth.PaymentAuthStorage
import com.tangem.domain.payment.models.auth.PaymentAuthTokens
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.text.decodeToString
import com.tangem.sdk.storage.AndroidSecureStorageV2

private const val AUTH_TOKENS_DEFAULT_KEY = "tangem_pay_default_key"

internal class TangemPayAuthStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @NetworkMoshi moshi: Moshi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
) : PaymentAuthStorage {

    private val secureStorage by lazy {
        AndroidSecureStorageV2(
            appContext = applicationContext,
            useStrongBox = false,
            name = "tangem_pay_storage",
        )
    }

    private val tokensAdapter by lazy { moshi.adapter(PaymentAuthTokens::class.java) }

    override suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String) {
        appPreferencesStore
            .store(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId), customerWalletAddress)
    }

    override suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String? {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId),
        )
            .takeIf { !it.isNullOrEmpty() }
    }

    override suspend fun storeAuthTokens(customerWalletAddress: String, tokens: PaymentAuthTokens) {
        return withContext(dispatcherProvider.io) {
            val json = tokensAdapter.toJson(tokens)

            secureStorage.store(
                json.encodeToByteArray(throwOnInvalidSequence = true),
                createAuthTokensKey(customerWalletAddress),
            )
        }
    }

    override suspend fun getAuthTokens(customerWalletAddress: String): PaymentAuthTokens? {
        return withContext(dispatcherProvider.io) {
            val authTokens = secureStorage.get(createAuthTokensKey(customerWalletAddress))
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(tokensAdapter::fromJson)

            authTokens?.let { tokens ->
                if (tokens.idempotencyKey == null) {
                    val newAuthTokens = tokens.copy(idempotencyKey = UUID.randomUUID().toString())
                    storeAuthTokens(customerWalletAddress, newAuthTokens)
                    newAuthTokens
                } else {
                    tokens
                }
            }
        }
    }

    private fun createAuthTokensKey(address: String): String = "${AUTH_TOKENS_DEFAULT_KEY}_$address"
}