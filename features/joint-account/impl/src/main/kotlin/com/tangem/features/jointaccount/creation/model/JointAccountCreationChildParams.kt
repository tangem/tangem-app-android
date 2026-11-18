package com.tangem.features.jointaccount.creation.model

import com.tangem.common.routing.AppRoute

/** What the flow container hands to every step: the flow input plus the shared draft */
internal data class JointAccountCreationChildParams(
    val source: AppRoute.JointAccountCreation.Source,
    val draftHolder: JointAccountCreationDraftHolder,
)