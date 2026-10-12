package com.tangem.features.introduction.impl.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.introduction.impl.ui.state.WelcomeSceneUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.res.R as CoreResR

/** The order is the video timeline: the index of a scene is what the animation moves. */
internal val welcomeScenes: ImmutableList<WelcomeSceneUM> = persistentListOf(
    WelcomeSceneUM(
        title = resourceReference(CoreResR.string.introduction_title),
        subtitle = resourceReference(CoreResR.string.introduction_subtitle),
    ),
    WelcomeSceneUM(
        title = resourceReference(CoreResR.string.introduction_scene_two_title),
        subtitle = resourceReference(CoreResR.string.introduction_scene_two_subtitle),
    ),
    WelcomeSceneUM(
        title = resourceReference(CoreResR.string.introduction_scene_three_title),
        subtitle = resourceReference(CoreResR.string.introduction_scene_three_subtitle),
    ),
)