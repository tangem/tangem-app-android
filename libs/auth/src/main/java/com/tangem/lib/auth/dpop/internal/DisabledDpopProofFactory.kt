package com.tangem.lib.auth.dpop.internal

import arrow.core.None
import arrow.core.Option
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_15438_BACKEND_AUTHENTICATION_ENABLED")
internal object DisabledDpopProofFactory : DpopProofFactory {

    override suspend fun create(httpMethod: String, httpUri: String, accessToken: String?): Option<String> = None
}