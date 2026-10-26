package com.example.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    val color: Color,
    val rotation: Float = 0f
)

class WuxiaParticleSystem(private val maxParticles: Int = 40) {
    private val particles = mutableListOf<Particle>()

    fun initPetals(width: Float, height: Float) {
        particles.clear()
        val petalColors = listOf(
            Color(0xFFFFB7B2), // Sakura pink
            Color(0xFFFF9EAA),
            Color(0xFFFDE2E4),
            Color(0xFFE2ECE9)
        )
        for (i in 0 until maxParticles) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    vx = -0.5f - Random.nextFloat() * 1.5f,
                    vy = 0.8f + Random.nextFloat() * 1.2f,
                    size = 4f + Random.nextFloat() * 6f,
                    alpha = 0.4f + Random.nextFloat() * 0.5f,
                    color = petalColors.random(),
                    rotation = Random.nextFloat() * 360f
                )
            )
        }
    }

    fun initQiSparks(width: Float, height: Float) {
        particles.clear()
        val qiColors = listOf(
            Color(0xFF10B981), // Jade
            Color(0xFF34D399),
            Color(0xFF38BDF8), // Cyan
            Color(0xFFF59E0B)  // Gold
        )
        for (i in 0 until maxParticles) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    vx = (Random.nextFloat() - 0.5f) * 0.8f,
                    vy = -0.5f - Random.nextFloat() * 1.5f,
                    size = 3f + Random.nextFloat() * 4f,
                    alpha = 0.3f + Random.nextFloat() * 0.6f,
                    color = qiColors.random()
                )
            )
        }
    }

    fun updateAndDraw(drawScope: DrawScope, width: Float, height: Float, timeSec: Float) {
        if (particles.isEmpty()) {
            initPetals(width, height)
        }

        particles.forEach { p ->
            p.x += p.vx + sin(timeSec + p.y * 0.02f) * 0.4f
            p.y += p.vy

            // Wrap around edges
            if (p.x < -20f) p.x = width + 20f
            if (p.x > width + 20f) p.x = -20f
            if (p.y > height + 20f) {
                p.y = -10f
                p.x = Random.nextFloat() * width
            }

            drawScope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x, p.y)
            )
        }
    }
}
