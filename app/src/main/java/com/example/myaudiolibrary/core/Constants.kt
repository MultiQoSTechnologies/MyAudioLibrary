package com.example.myaudiolibrary.core

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

typealias Anim = AnimationConstants

private const val LONG_DURATION_TIME = 500
val Anim.LongDurationMills get() = LONG_DURATION_TIME

private const val MEDIUM_DURATION_TIME = 400
val Anim.MediumDurationMills get() = MEDIUM_DURATION_TIME

