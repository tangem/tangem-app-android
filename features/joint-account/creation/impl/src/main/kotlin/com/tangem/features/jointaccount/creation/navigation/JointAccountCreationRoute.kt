package com.tangem.features.jointaccount.creation.navigation

import com.tangem.core.decompose.navigation.Route

internal sealed interface JointAccountCreationRoute : Route {

    data object Promo : JointAccountCreationRoute
    data object Config : JointAccountCreationRoute

    // TODO([REDACTED_TASK_KEY]): Composition — total members and required signatures
    // TODO([REDACTED_TASK_KEY]): DisplayName — the creator's own name, then the card signature
}