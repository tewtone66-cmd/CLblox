package com.clblox.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clblox.launcher.Game
import com.clblox.launcher.GameInfo
import com.clblox.launcher.GameInfoRepo
import com.clblox.launcher.compact

/**
 * The signature element: a solid block with a hard, offset shadow.
 * When pressed, the block sinks into its shadow.
 */
@Composable
fun Block(
    onClick: (() -> Unit)?,
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    depth: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed) depth else 0.dp, tween(80), label = "drop")

    Box(modifier.padding(bottom = depth), propagateMinConstraints = true) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = depth)
                .background(lerp(color, Color.Black, 0.45f), shape)
        )
        val front = Modifier.offset(y = drop).clip(shape).background(color)
        Box(
            if (onClick != null) {
                front.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            } else {
                front
            },
            content = content
        )
    }
}

@Composable
fun BlockButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CL.Butter,
    enabled: Boolean = true
) {
    val bg = if (enabled) color else CL.NightRaised
    Block(
        onClick = if (enabled) onClick else null,
        color = bg,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        depth = 5.dp
    ) {
        Text(
            text,
            Modifier.align(Alignment.Center).padding(horizontal = 20.dp, vertical = 14.dp),
            color = if (enabled) CL.onTile(color) else CL.MistDim,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CL.Butter)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("CL", color = CL.Night, style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "blox",
            Modifier.padding(start = 4.dp),
            color = CL.Mist,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/** Four studs like a building block; one is missing, which one depends on the seed. */
@Composable
fun StudGlyph(seed: Long, tint: Color, glyphSize: Dp = 34.dp) {
    val missing = (((seed % 4) + 4) % 4).toInt()
    Canvas(Modifier.size(glyphSize)) {
        val gap = 4.dp.toPx()
        val cell = (this.size.width - gap) / 2f
        for (i in 0 until 4) {
            if (i == missing) continue
            val x = (i % 2) * (cell + gap)
            val y = (i / 2) * (cell + gap)
            drawRoundRect(tint, Offset(x, y), Size(cell, cell), CornerRadius(5.dp.toPx()))
        }
    }
}

@Composable
private fun PlayingChip(playing: Long, ink: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(CL.Mint))
        Spacer(Modifier.size(6.dp))
        Text(
            "${compact(playing)} playing",
            color = ink,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun GameTile(game: Game, onPlay: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val info by produceState<GameInfo?>(null, game.placeId) { value = GameInfoRepo.get(game.placeId) }
    val color = CL.tileColor(game.placeId)
    val iconUrl = info?.iconUrl
    val hasImage = iconUrl != null
    val ink = if (hasImage) CL.Mist else CL.onTile(color)
    val name = info?.name?.takeIf { it.isNotBlank() } ?: game.name

    Block(onClick = onPlay, color = color, modifier = modifier.height(170.dp)) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, CL.Night.copy(alpha = 0.92f)))
                )
            )
        }
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (hasImage) Spacer(Modifier.size(1.dp)) else StudGlyph(game.placeId, ink.copy(alpha = 0.85f))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (hasImage) CL.Night.copy(alpha = 0.55f) else Color.Transparent)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove $name", tint = ink)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    name,
                    color = ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                info?.let { PlayingChip(it.playing, ink) }
            }
        }
    }
}

@Composable
fun HeroBlock(game: Game?, onPlay: () -> Unit, onAdd: () -> Unit) {
    val info by produceState<GameInfo?>(null, game?.placeId) {
        value = game?.let { GameInfoRepo.get(it.placeId) }
    }
    val iconUrl = info?.iconUrl
    val name = info?.name?.takeIf { it.isNotBlank() } ?: game?.name ?: "No games yet"

    Block(
        onClick = null,
        color = CL.Violet,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        depth = 8.dp
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(CL.Violet, CL.Violet.copy(alpha = 0.55f)))
                )
            )
        } else {
            Box(Modifier.align(Alignment.TopEnd).padding(20.dp)) {
                StudGlyph(
                    seed = game?.placeId ?: 0L,
                    tint = Color.White.copy(alpha = 0.2f),
                    glyphSize = 64.dp
                )
            }
        }
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                name,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 36.dp)
            )
            if (game == null) {
                Text(
                    "Paste a game link or place ID to add one.",
                    color = CL.Mist,
                    style = MaterialTheme.typography.bodyMedium
                )
                BlockButton("Add game", onAdd)
            } else {
                info?.let { PlayingChip(it.playing, Color.White) }
                BlockButton("Play", onPlay)
            }
        }
    }
}

@Composable
fun NavPill(
    selected: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(CL.NightRaised)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) CL.Butter else Color.Transparent, label = "nav")
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (on) CL.Night else CL.MistDim,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun StatusTile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Block(
        onClick = null,
        color = color,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        depth = 5.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = CL.onTile(color), style = MaterialTheme.typography.titleMedium)
            Text(
                title,
                color = CL.onTile(color).copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Temperature dial. level: 0 cool .. 4 critical, -1 = unknown. */
@Composable
fun HeatDial(level: Int, word: String, modifier: Modifier = Modifier) {
    val target = if (level < 0) 0f else (level / 4f).coerceIn(0.04f, 1f)
    val sweep by animateFloatAsState(target, tween(700), label = "dial")
    val arc = when {
        level <= 1 -> CL.Mint
        level == 2 -> CL.Butter
        else -> CL.Coral
    }
    Box(modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val stroke = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round)
            drawArc(CL.NightRaised, 135f, 270f, false, style = stroke)
            if (sweep > 0f) drawArc(arc, 135f, 270f * sweep, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(word, color = CL.Mist, style = MaterialTheme.typography.headlineMedium)
            Text("phone temperature", color = CL.MistDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
