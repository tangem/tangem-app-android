package com.tangem.features.jointaccount

/**
 * Feature toggles of the joint account feature.
 *
 * The toggle gates creation and whether the app knows about joint accounts at all. It also picks the version of the
 * accounts endpoints: with it off the app speaks `v1`, which carries no record type and knows only the wallet's own
 * accounts, and any joint record that still reaches it is dropped where the accounts response arrives; with it on the

 */
interface JointAccountFeatureToggles {

    val isJointAccountCreationEnabled: Boolean
}