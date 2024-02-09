package com.aaronhowser1.ariadnesthread.item

import com.aaronhowser1.ariadnesthread.client.ModRenderer
import com.aaronhowser1.ariadnesthread.config.ServerConfig
import com.aaronhowser1.ariadnesthread.utils.Location
import com.aaronhowser1.ariadnesthread.utils.Location.Companion.toLocation
import com.aaronhowser1.ariadnesthread.utils.TextUtils.tooltipTranslatable
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.level.Level

class ThreadItem : Item(
    Properties()
        .tab(CreativeModeTab.TAB_MISC)
        .stacksTo(1)
        .rarity(Rarity.UNCOMMON)
) {

    companion object {
        const val IS_RECORDING = "ariadnesthread.isRecording"
        const val HISTORY = "ariadnesthread.history"
        const val STARTING_DIMENSION = "ariadnesthread.startingDimension"
    }

    // Recording functions

    private fun isRecording(itemStack: ItemStack): Boolean {
        val tag = itemStack.tag ?: return false

        return tag.getBoolean(IS_RECORDING)
    }

    private fun startRecording(itemStack: ItemStack, player: Player) {

        if (itemStack.tag?.contains(IS_RECORDING) == true) return

        itemStack.tag = itemStack.tag ?: CompoundTag()
        itemStack.tag?.apply {
            putBoolean(IS_RECORDING, true)
            putString(STARTING_DIMENSION, player.level.dimension().location().toString())

            val hasHistory = itemStack.tag?.contains(HISTORY) ?: false
            if (!hasHistory) {
                val emptyList = ListTag()
                put(HISTORY, emptyList)
            }
        }

        addLocation(itemStack, player.eyePosition.toLocation())
    }

    private fun stopRecording(itemStack: ItemStack) {
        itemStack.tag = itemStack.tag ?: CompoundTag()
        itemStack.tag?.putBoolean(IS_RECORDING, false)
    }

    // History functions

    private fun hasHistory(itemStack: ItemStack): Boolean {
        return itemStack.tag?.contains(HISTORY) ?: false
    }

    private fun getHistory(itemStack: ItemStack): List<Location> {
        val list = itemStack.tag?.getList(HISTORY, 10) ?: return emptyList()

        return list.map { Location(it as CompoundTag) }
    }

    private fun clearHistory(itemStack: ItemStack) {
        itemStack.tag = itemStack.tag ?: CompoundTag()

        itemStack.tag?.apply {
            remove(IS_RECORDING)
            remove(HISTORY)
            remove(STARTING_DIMENSION)
        }

        ModRenderer.locations = emptyList()
    }

    private fun showHistory(itemStack: ItemStack) {
        ModRenderer.locations = getHistory(itemStack)
    }

    private fun addLocation(itemStack: ItemStack, location: Location) {
        val list = itemStack.tag?.getList(HISTORY, 10) ?: return

        if (list.size >= ServerConfig.MAX_LOCATIONS) {
//            list.removeAt(0)
            stopRecording(itemStack)
        }

        if (list.isNotEmpty()) {
            val mostRecentTag = list.lastOrNull() ?: error("List is not empty, but has no last element.")
            val mostRecentLocation = Location(mostRecentTag as CompoundTag)

            val tooClose = mostRecentLocation.closerThan(location, ServerConfig.MIN_DISTANCE)
            if (tooClose) return
        }

        list.add(location.toTag())
    }


    // Override functions

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {

        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))

        val itemStack = player.getItemInHand(hand)

        val isSneaking = player.isShiftKeyDown
        val isRecording = isRecording(itemStack)

        if (!isRecording) {
            if (isSneaking) {
                clearHistory(itemStack)
                return InteractionResultHolder.success(itemStack)
            }
            startRecording(itemStack, player)
            return InteractionResultHolder.success(itemStack)
        }

        if (isSneaking) {
            stopRecording(itemStack)
            return InteractionResultHolder.success(itemStack)
        }

        return InteractionResultHolder.pass(itemStack)
    }

    override fun onDroppedByPlayer(item: ItemStack?, player: Player?): Boolean {
        stopRecording(item!!)
        return super.onDroppedByPlayer(item, player)
    }

    private fun getStartingDimension(itemStack: ItemStack): String {
        return itemStack.tag?.getString(STARTING_DIMENSION) ?: error("ItemStack has no starting dimension.")
    }

    private fun inStartingDimension(itemStack: ItemStack, level: Level?): Boolean {
        return getStartingDimension(itemStack) == level?.dimension()?.location()?.toString()
    }

    override fun inventoryTick(itemStack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {

        if (entity !is Player) return

        if (isRecording(itemStack)) {
            // nbt space in bytes
            val nbtSize = itemStack.tag?.toString()?.length ?: 0
        }

        if (inStartingDimension(itemStack, level)) {

            if (level.isClientSide) {
                showHistory(itemStack)
                return
            }

            val goodTick = level.gameTime % ServerConfig.CHECK_INTERVAL == 0L
            if (goodTick && isRecording(itemStack)) {
                addLocation(itemStack, entity.eyePosition.toLocation())
            }
        }

        super.inventoryTick(itemStack, level, entity, slotId, isSelected)
    }

    override fun isFoil(itemStack: ItemStack): Boolean {
        return isRecording(itemStack)
    }

    override fun appendHoverText(
        itemStack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {

        tooltipTranslatable(
            tooltipComponents,
            if (isRecording(itemStack)) "tooltip.ariadnesthread.recording_1" else "tooltip.ariadnesthread.not_recording_1"
        )
        tooltipTranslatable(
            tooltipComponents,
            if (isRecording(itemStack)) "tooltip.ariadnesthread.recording_2" else "tooltip.ariadnesthread.not_recording_2"
        )

        if (!isRecording(itemStack) && hasHistory(itemStack)) tooltipTranslatable(
            tooltipComponents,
            "tooltip.ariadnesthread.clear"
        ) {
            it.withStyle(ChatFormatting.RED)
        }

        if (isRecording(itemStack) && !inStartingDimension(itemStack, level)) {
            tooltipTranslatable(tooltipComponents, "tooltip.ariadnesthread.not_in_starting_dimension") {
                it.withStyle(ChatFormatting.RED)
            }
        }

        if (tooltipFlag.isAdvanced) tooltipComponents.add(
            Component.literal(itemStack.tag.toString())
        )

        super.appendHoverText(itemStack, level, tooltipComponents, tooltipFlag)
    }

}