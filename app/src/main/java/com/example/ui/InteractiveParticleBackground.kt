package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val density = LocalDensity.current
    val primaryColor = if (isDarkTheme) palette.darkPrimary else palette.lightPrimary
    val secondaryColor = if (isDarkTheme) palette.darkSecondary else palette.lightSecondary

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val maxLinesDistance = with(density) { 90.dp.toPx() }
        val activeLineMaxDistance = with(density) { 140.dp.toPx() }

        // Touch Coordinates
        var touchX by remember { mutableStateOf<Float?>(null) }
        var touchY by remember { mutableStateOf<Float?>(null) }

        // Particles collection
        val particles = remember { mutableStateListOf<AnimParticle>() }

        // Initialize or recalculate particle positions if Resolution or parameters change
        LaunchedEffect(widthPx, heightPx) {
            if (widthPx > 0 && heightPx > 0) {
                particles.clear()
                val count = 30 // Optimized quantity to render at maximum performance and keep CPU load extremely light
                for (i in 0 until count) {
                    particles.add(
                        AnimParticle(
                            x = Random.nextFloat() * widthPx,
                            y = Random.nextFloat() * heightPx,
                            vx = (Random.nextFloat() * 2f - 1f) * 40f, // stable pixel movement speed
                            vy = (Random.nextFloat() * 2f - 1f) * 40f,
                            r = Random.nextFloat() * 3.5f + 3f
                        )
                    )
                }
            }
        }

        // Tick loop driving position physics
        var frameTrigger by remember { mutableStateOf(0L) }
        LaunchedEffect(particles.size) {
            var lastTime = System.nanoTime()
            while (isActive) {
                withFrameNanos { time ->
                    val elapsedSeconds = (time - lastTime) / 1_000_000_000f
                    lastTime = time
                    val dt = elapsedSeconds.coerceAtMost(0.033f) // Cap dt at ~30fps equivalent to maintain stability under any lag spikes

                    if (widthPx > 0f && heightPx > 0f) {
                        for (p in particles) {
                            // Apply attraction forces towards finger coordinates if interactive mode is toggled on inside Settings
                            val tx = touchX
                            val ty = touchY
                            if (interactive && tx != null && ty != null) {
                                val dx = tx - p.x
                                val dy = ty - p.y
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < activeLineMaxDistance && dist > 1f) {
                                    val pullIntensity = (1f - dist / activeLineMaxDistance) * 40f
                                    p.vx += (dx / dist) * pullIntensity * dt
                                    p.vy += (dy / dist) * pullIntensity * dt
                                }
                            }

                            // Dynamic collision boundaries against screen borders
                            p.x += p.vx * dt
                            p.y += p.vy * dt

                            if (p.x < 0) {
                                p.x = 0f
                                p.vx *= -1
                            } else if (p.x > widthPx) {
                                p.x = widthPx
                                p.vx *= -1
                            }

                            if (p.y < 0) {
                                p.y = 0f
                                p.vy *= -1
                            } else if (p.y > heightPx) {
                                p.y = heightPx
                                p.vy *= -1
                            }

                            // Safeguard speed bounds to guarantee stable floating animations
                            val speed = sqrt(p.vx * p.vx + p.vy * p.vy)
                            val maxSpeed = 50f
                            val minSpeed = 15f
                            if (speed > maxSpeed) {
                                p.vx = (p.vx / speed) * maxSpeed
                                p.vy = (p.vy / speed) * maxSpeed
                            } else if (speed < minSpeed) {
                                p.vx = (p.vx / speed) * minSpeed
                                p.vy = (p.vy / speed) * minSpeed
                            }
                        }
                    }
                    frameTrigger = time
                }
            }
        }

        // Attach multi-touch input interceptors only if user has authorized Interaction Mode
        val motionEventModifier = if (interactive) {
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed) {
                                    touchX = change.position.x
                                    touchY = change.position.y
                                } else {
                                    touchX = null
                                    touchY = null
                                }
                            }
                        }
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }

        Canvas(modifier = motionEventModifier) {
            // Observe tick trigger state to invalidate layouts automatically
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTrigger

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
                        val alpha = (1f - dist / maxLinesDistance) * 0.20f
                        drawLine(
                            color = primaryColor.copy(alpha = alpha),
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = 1.1f
                        )
                    }
                }

                // Render strong high-contrast connector vectors leading into the user drag coordinates
                val tx = touchX
                val ty = touchY
                if (interactive && tx != null && ty != null) {
                    val dx = p1.x - tx
                    val dy = p1.y - ty
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < activeLineMaxDistance) {
                        val alpha = (1f - dist / activeLineMaxDistance) * 0.45f
                        drawLine(
                            color = secondaryColor.copy(alpha = alpha),
                            start = Offset(p1.x, p1.y),
                            end = Offset(tx, ty),
                            strokeWidth = 1.6f
                        )
                    }
                }
            }

            // 2. Render all node cores
            for (i in 0 until particlesListSize) {
                val p = particles[i]
                // Layered dual-circle rendering creates beautiful depth & core brightness
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = p.r,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.85f),
                    radius = p.r * 0.55f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
