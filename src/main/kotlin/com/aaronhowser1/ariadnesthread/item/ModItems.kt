package com.aaronhowser1.ariadnesthread.item

import com.aaronhowser1.ariadnesthread.AriadnesThread
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object ModItems {
    val REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, AriadnesThread.MOD_ID)

    val THREAD_ITEM by REGISTRY.registerObject("ariadnes_thread") {
        ThreadItem()
    }



}