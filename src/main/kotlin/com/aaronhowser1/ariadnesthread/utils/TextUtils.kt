package com.aaronhowser1.ariadnesthread.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object TextUtils {

    fun tooltipTranslatable(tooltipComponents: MutableList<Component>, translatable: String, block: (MutableComponent) -> Unit = {}) {
        tooltipComponents.add(
            Component.translatable(translatable).apply {
                block(this)
            }
        )
    }

}