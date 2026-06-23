package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.sqrt
import kotlin.random.Random

class AnimParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float
)

@Composable
fun InteractiveParticleBackground(
    enabled: Boolean,
    interactive: Boolean,
    palette: ParticlePalette,
    isDarkTheme: Boolean,
    count: Int,
    sizeScale: Float,
    speedScale: Float,
    touchX: Float?,
    touchY: Float?,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val density = LocalDensity.current
    val primaryColor = if (isDarkTheme) palette.darkPrimary else palette.lightPrimary
    val secondaryColor = if (isDarkTheme) palette.darkSecondary else palette.lightSecondary

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val maxLinesDistance = with(density) { 95.dp.toPx() }
        val activeLineMaxDistance = with(density) { 150.dp.toPx() }

        // Particles collection
        val particles = remember { mutableStateListOf<AnimParticle>() }

        // Initialize or recalculate particle positions if count or dimensions change
        val countValue = count.coerceIn(10, 100)
        LaunchedEffect(widthPx, heightPx, countValue, sizeScale, speedScale) {
            if (widthPx > 0 && heightPx > 0) {
                particles.clear()
                for (i in 0 until countValue) {
                    val baseSpeed = 40f * speedScale
                    particles.add(
                        AnimParticle(
                            x = Random.nextFloat() * (widthPx - 20f) + 10f,
                            y = Random.nextFloat() * (heightPx - 20f) + 10f,
                            vx = (Random.nextFloat() * 2f - 1f) * baseSpeed,
                            vy = (Random.nextFloat() * 2f - 1f) * baseSpeed,
                            r = (Random.nextFloat() * 3.5f + 3f) * sizeScale
                        )
                    )
                }
            }
        }

        // Tick loop driving position physics and collisions
        var frameTrigger by remember { mutableStateOf(0L) }
        LaunchedEffect(particles.size, speedScale, interactive, touchX, touchY) {
            var lastTime = System.nanoTime()
            while (isActive) {
                withFrameNanos { time ->
                    val elapsedSeconds = (time - lastTime) / 1_000_000_000f
                    lastTime = time
                    val dt = elapsedSeconds.coerceAtMost(0.033f) // Cap dt at ~30fps equivalent to maintain stability under any lag spikes

                    if (widthPx > 0f && heightPx > 0f) {
                        val particlesListSize = particles.size

                        // 1. Particle-to-Particle Elastic Collisions (Repotenciado)
                        for (i in 0 until particlesListSize) {
                            val p1 = particles[i]
                            for (j in i + 1 until particlesListSize) {
                                val p2 = particles[j]
                                val dx = p2.x - p1.x
                                val dy = p2.y - p1.y
                                val dist = sqrt(dx * dx + dy * dy)
                                val minDist = p1.r + p2.r
                                
                                if (dist < minDist && dist > 0.001f) {
                                    // Overlap resolution with mass approximation based on radius
                                    val overlap = minDist - dist
                                    val nx = dx / dist
                                    val ny = dy / dist

                                    val mass1 = p1.r * p1.r
                                    val mass2 = p2.r * p2.r
                                    val totalMass = mass1 + mass2
                                    
                                    val ratio1 = mass2 / totalMass
                                    val ratio2 = mass1 / totalMass

                                    p1.x -= nx * overlap * ratio1
                                    p1.y -= ny * overlap * ratio1
                                    p2.x += nx * overlap * ratio2
                                    p2.y += ny * overlap * ratio2

                                    // Relative velocity in normal direction
                                    val rVecX = p1.vx - p2.vx
                                    val rVecY = p1.vy - p2.vy
                                    val velAlongNormal = rVecX * nx + rVecY * ny

                                    // Only resolve if velocities are directed towards each other
                                    if (velAlongNormal > 0f) {
                                        val restitution = 0.85f // Bounciness factor
                                        val impulse = -(1f + restitution) * velAlongNormal
                                        
                                        p1.vx += impulse * nx * ratio1
                                        p1.vy += impulse * ny * ratio1
                                        p2.vx -= impulse * nx * ratio2
                                        p2.vy -= impulse * ny * ratio2
                                    }
                                }
                            }
                        }

                        // 2. Individual Particle Movement, Attraction and Wall Collisions
                        for (p in particles) {
                            // Apply attraction forces towards touch coordinates if interactive mode is toggled on
                            val tx = touchX
                            val ty = touchY
                            if (interactive && tx != null && ty != null) {
                                val dx = tx - p.x
                                val dy = ty - p.y
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < activeLineMaxDistance && dist > 1f) {
                                    // Repowered Interaction: Stronger pull with a slight orbital/swirl force
                                    val forcePercent = 1f - (dist / activeLineMaxDistance)
                                    val pullIntensity = forcePercent * 180f * speedScale
                                    
                                    // Orbital swirl component
                                    val swirlX = -dy / dist
                                    val swirlY = dx / dist
                                    val swirlIntensity = forcePercent * 80f * speedScale

                                    p.vx += ((dx / dist) * pullIntensity + swirlX * swirlIntensity) * dt
                                    p.vy += ((dy / dist) * pullIntensity + swirlY * swirlIntensity) * dt
                                    
                                    // Dampen velocity slightly when very close to touch point to prevent orbiting out of control
                                    if (dist < activeLineMaxDistance * 0.3f) {
                                        p.vx *= 0.95f
                                        p.vy *= 0.95f
                                    }
                                }
                            }

                            // Dynamic boundary collisions (respecting radius)
                            p.x += p.vx * dt
                            p.y += p.vy * dt

                            val r = p.r
                            if (p.x - r < 0) {
                                p.x = r
                                p.vx = kotlin.math.abs(p.vx)
                            } else if (p.x + r > widthPx) {
                                p.x = widthPx - r
                                p.vx = -kotlin.math.abs(p.vx)
                            }

                            if (p.y - r < 0) {
                                p.y = r
                                p.vy = kotlin.math.abs(p.vy)
                            } else if (p.y + r > heightPx) {
                                p.y = heightPx - r
                                p.vy = -kotlin.math.abs(p.vy)
                            }

                            // Safeguard speed bounds to guarantee stable floating animations
                            val speed = sqrt(p.vx * p.vx + p.vy * p.vy)
                            val maxSpeed = 120f * speedScale // Increased max speed for more dynamic movement
                            val minSpeed = 10f * speedScale
                            if (speed > maxSpeed && speed > 0f) {
                                p.vx = (p.vx / speed) * maxSpeed
                                p.vy = (p.vy / speed) * maxSpeed
                            } else if (speed < minSpeed && speed > 0f) {
                                p.vx = (p.vx / speed) * minSpeed
                                p.vy = (p.vy / speed) * minSpeed
                            }
                        }
                    }
                    frameTrigger = time
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Observe tick trigger state to invalidate layouts automatically
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTrigger
            
            val timeMs = System.currentTimeMillis()
            val pulse = (Math.sin(timeMs / 300.0) * 0.5 + 0.5).toFloat()

            val particlesListSize = particles.size
            
            // 1. Render all interlocked connectivity vectors
            for (i in 0 until particlesListSize) {
                val p1 = particles[i]
                for (j in i + 1 until particlesListSize) {
                    val p2 = particles[j]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    val dist = sqrt(dx * dx + dy * dy)
                    
                    if (dist < maxLinesDistance) {
                        val distanceRatio = 1f - dist / maxLinesDistance
                        // Enhanced glowing lines: alpha and thickness scale with proximity
                        val alpha = distanceRatio * 0.35f
                        drawLine(
                            color = primaryColor.copy(alpha = alpha),
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = 1f + (distanceRatio * 2f)
                        )
                    }
                }

                // Render strong high-contrast connector vectors leading into the user touch coordinates
                val tx = touchX
                val ty = touchY
                if (interactive && tx != null && ty != null) {
                    val dx = p1.x - tx
                    val dy = p1.y - ty
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < activeLineMaxDistance) {
                        val distanceRatio = 1f - dist / activeLineMaxDistance
                        val alpha = distanceRatio * 0.55f
                        drawLine(
                            color = secondaryColor.copy(alpha = alpha),
                            start = Offset(p1.x, p1.y),
                            end = Offset(tx, ty),
                            strokeWidth = 1.5f + (distanceRatio * 3f)
                        )
                    }
                }
            }

            // 2. Render all node cores
            for (i in 0 until particlesListSize) {
                val p = particles[i]
                
                // Repowered: Layered triple-circle rendering creates beautiful depth, glow & core brightness
                
                // Outer subtle glow
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = p.r * 1.8f,
                    center = Offset(p.x, p.y)
                )
                
                // Mid layer
                drawCircle(
                    color = primaryColor.copy(alpha = 0.45f),
                    radius = p.r,
                    center = Offset(p.x, p.y)
                )
                
                // Inner bright core
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.95f),
                    radius = p.r * 0.5f,
                    center = Offset(p.x, p.y)
                )
            }
            
            // 3. Render Touch Attractor Node if active
            val tx = touchX
            val ty = touchY
            if (interactive && tx != null && ty != null) {
                // Pulsating touch magnet effect
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.15f + (pulse * 0.15f)),
                    radius = 35f + (pulse * 15f),
                    center = Offset(tx, ty)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.4f),
                    radius = 20f,
                    center = Offset(tx, ty)
                )
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.9f),
                    radius = 8f,
                    center = Offset(tx, ty)
                )
            }
        }
    }
}
