package com.aaronhowser1.ariadnesthread.client

import com.aaronhowser1.ariadnesthread.utils.Location
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import thedarkcolour.kotlinforforge.forge.vectorutil.toVec3

@OnlyIn(Dist.CLIENT)
data class LineSegment(
    val start: Location,
    val end: Location,
    val level: String,
    val colorHex: String
) {
    // TODO:
    //  - Render a line between these two points
    //  - The line should be visible through blocks
    //  - Lerp the color based on its position in the list
    //  - If the distance between is beyond the configured teleport distance, either don't render at all or render with like a spiral or something

    fun spawnParticles(player: Player) {
        val deltaVector = end.toVec3().vectorTo(start.toVec3())

        val distance = deltaVector.length().toInt()

        repeat(distance) {
            val position = start.toVec3().add(deltaVector.scale(it.toDouble() / distance))
            player.level.addParticle(
                ParticleTypes.BUBBLE,
                position.x,
                position.y,
                position.z,
                0.0,
                0.0,
                0.0
            )
        }

    }

}