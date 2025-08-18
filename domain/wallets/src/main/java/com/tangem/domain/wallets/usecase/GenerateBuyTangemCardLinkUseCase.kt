package com.tangem.domain.wallets.usecase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GenerateBuyTangemCardLinkUseCase {

    suspend operator fun invoke(): String = suspendCoroutine { cont ->
        Firebase.analytics.appInstanceId
            .addOnSuccessListener { id ->
                cont.resume("$NEW_BUY_WALLET_URL&app_instance_id=$id")
            }
            .addOnFailureListener {
                cont.resume(NEW_BUY_WALLET_URL)
            }
    }

    companion object {
        private const val NEW_BUY_WALLET_URL = "https://buy.tangem.com/?utm_source=tangem-app&utm_medium=app"
    }
}