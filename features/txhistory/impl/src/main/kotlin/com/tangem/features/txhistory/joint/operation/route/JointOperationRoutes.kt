package com.tangem.features.txhistory.joint.operation.route

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal sealed interface JointOperationRoutes : Route {

    @Serializable
    data object Operation : JointOperationRoutes

    @Serializable
    data object Signers : JointOperationRoutes
}