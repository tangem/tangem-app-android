package com.tangem.features.jointaccount.creation.navigation

import com.tangem.core.decompose.navigation.Route

internal sealed interface JointAccountCreationRoute : Route {

    data object Promo : JointAccountCreationRoute
    data object Config : JointAccountCreationRoute
    data object Composition : JointAccountCreationRoute
    data object DisplayName : JointAccountCreationRoute
}