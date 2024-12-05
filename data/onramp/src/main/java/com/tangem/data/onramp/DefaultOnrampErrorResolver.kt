package com.tangem.data.onramp

import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver

// TODO: https://tangem.atlassian.net/browse/AND-8405
internal class DefaultOnrampErrorResolver : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        return OnrampError.UnknownError
    }
}
