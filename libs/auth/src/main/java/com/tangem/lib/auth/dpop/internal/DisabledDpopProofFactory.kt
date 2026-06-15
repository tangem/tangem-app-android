package com.tangem.lib.auth.dpop.internal

import arrow.core.None
import arrow.core.Option
import com.tangem.lib.auth.dpop.DpopProofFactory

internal object DisabledDpopProofFactory : DpopProofFactory {

    override suspend fun create(httpMethod: String, httpUri: String, accessToken: String?): Option<String> = None
}