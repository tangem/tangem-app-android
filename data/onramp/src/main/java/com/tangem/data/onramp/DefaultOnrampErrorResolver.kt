package com.tangem.data.onramp

import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
// [REDACTED_TODO_COMMENT]
internal class DefaultOnrampErrorResolver : OnrampErrorResolver {

    override fun resolve(throwable: Throwable): OnrampError {
        return OnrampError.UnknownError
    }
}
