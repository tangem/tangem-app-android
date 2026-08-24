package com.tangem.features.jointaccount

/**
 * Feature toggles of the joint account feature.
 *
 * The toggle gates creation and, with it, whether the app knows about joint accounts at all: no feature tells the two
 * kinds of portfolio account apart, so the decision is taken once, where the accounts response arrives. With the
 * toggle off joint records are dropped there — the app neither shows a joint account nor can send one back.
 *

 * previous build — so above the data layer an "account exists but the toggle is off" branch must not exist.
 */
interface JointAccountFeatureToggles {

    val isJointAccountCreationEnabled: Boolean
}