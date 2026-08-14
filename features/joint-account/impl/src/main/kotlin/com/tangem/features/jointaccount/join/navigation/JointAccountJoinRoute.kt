package com.tangem.features.jointaccount.join.navigation

import com.tangem.core.decompose.navigation.Route

internal sealed interface JointAccountJoinRoute : Route {

    data object InvitePreview : JointAccountJoinRoute

    data object DisplayName : JointAccountJoinRoute
}