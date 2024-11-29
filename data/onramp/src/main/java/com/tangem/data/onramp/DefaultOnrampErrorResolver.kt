package com.tangem.data.onramp

import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver

// TODO: [REDACTED_JIRA]
internal class DefaultOnrampErrorResolver : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        return OnrampError.UnknownError
    }
}