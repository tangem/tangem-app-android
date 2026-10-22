package com.tangem.features.jointaccount.main.navigation

import com.tangem.core.decompose.navigation.Route

internal sealed interface JointAccountMembersRoute : Route {

    data object Members : JointAccountMembersRoute

    data class ConfirmAddress(val cryptoAccountId: String) : JointAccountMembersRoute
}