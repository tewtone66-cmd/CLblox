package com.clblox.launcher

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clblox.launcher.ui.BlockButton
import com.clblox.launcher.ui.CL
import com.clblox.launcher.ui.CLTheme
import com.clblox.launcher.ui.GameTile
import com.clblox.launcher.ui.HeatDial
import com.clblox.launcher.ui.HeroBlock
import com.clblox.launcher.ui.NavPill
import com.clblox.launcher.ui.StatusTile
import com.clblox.launcher.ui.Wordmark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { CLTheme { CLbloxApp() } }
    }
}

private fun toast(context: Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

@Composable
fun CLbloxApp() {
    val context = LocalContext.current
    val store = remember { GameStore(context) }
    val games = remember { mutableStateListOf<Game>().apply { addAll(store.load()) } }
    var lastId by remember { mutableStateOf(store.lastPlayed()) }
    var tab by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    fun play(game: Game) {
        when {
            !RobloxLauncher.isInstalled(context) -> {
                toast(context, "Roblox isn't installed")
                RobloxLauncher.openStorePage(context)
            }
            RobloxLauncher.launchGame(context, game.placeId) -> {
                lastId = game.placeId
                store.setLastPlayed(game.placeId)
            }
            else -> toast(context, "Couldn't open Roblox")
        }
    }

    val hero = games.firstOrNull { it.placeId == lastId } ?: games.firstOrNull()

    Box(Modifier.fillMaxSize().background(CL.Night).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Wordmark()
                if (tab == 0) BlockButton("Add", { showAdd = true }, color = CL.Mint)
            }
            Box(Modifier.weight(1f)) {
                if (tab == 0) {
                    HomeScreen(
                        games = games,
                        hero = hero,
                        onPlay = ::play,
                        onRemove = { g ->
                            games.remove(g)
                            store.save(games.toList())
                        },
                        onAdd = { showAdd = true }
                    )
                } else {
                    TuneScreen()
                }
            }
        }
        NavPill(
            selected = tab,
            labels = listOf("Games", "Tune"),
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }

    if (showAdd) {
        AddGameDialog(
            onDismiss = { showAdd = false },
            onAdd = { link, name ->
                val id = RobloxLauncher.parsePlaceId(link)
                when {
                    id == null -> toast(context, "That isn't a game link or ID")
                    games.any { it.placeId == id } -> toast(context, "Already in your list")
                    else -> {
                        games.add(Game(id, name.ifBlank { "Game $id" }))
                        store.save(games.toList())
                        showAdd = false
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    games: List<Game>,
    hero: Game?,
    onPlay: (Game) -> Unit,
    onRemove: (Game) -> Unit,
    onAdd: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroBlock(game = hero, onPlay = { hero?.let(onPlay) }, onAdd = onAdd)
        }
        items(games, key = { it.placeId }) { g ->
            GameTile(g, onPlay = { onPlay(g) }, onRemove = { onRemove(g) })
        }
    }
}

private fun heatWord(level: Int) = when (level) {
    -1 -> "n/a"
    0 -> "Cool"
    1 -> "Warm"
    2 -> "Hot"
    3 -> "Very hot"
    else -> "Critical"
}

@Composable
private fun TuneScreen() {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val level = remember(refresh) { PerfTools.thermalLevel(context) }
    val saver = remember(refresh) { PerfTools.batterySaverOn(context) }
    val mode = remember(refresh) { PerfTools.gameMode(context) }
    val dnd = remember(refresh) { PerfTools.dndAccessGranted(context) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeatDial(level, heatWord(level))
        if (level >= 2) {
            Text(
                "Your phone is hot. Lower graphics in Roblox or take a break.",
                color = CL.Mist,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusTile(
                "Battery saver",
                if (saver) "On" else "Off",
                if (saver) CL.Coral else CL.Mint,
                Modifier.weight(1f)
            )
            StatusTile("Game Mode", mode, CL.Butter, Modifier.weight(1f))
        }
        if (saver) {
            Text(
                "Battery saver lowers performance. Turn it off for smoother play.",
                color = CL.MistDim,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(4.dp))

        BlockButton(
            text = if (dnd) "Turn on Do Not Disturb" else "Allow Do Not Disturb access",
            onClick = {
                if (dnd) {
                    PerfTools.setDnd(context, true)
                    toast(context, "Do Not Disturb is on")
                } else {
                    context.startActivity(PerfTools.dndPermissionIntent())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            color = CL.Violet
        )
        BlockButton(
            text = "Turn off Do Not Disturb",
            onClick = {
                PerfTools.setDnd(context, false)
                toast(context, "Do Not Disturb is off")
            },
            modifier = Modifier.fillMaxWidth(),
            color = CL.Coral,
            enabled = dnd
        )
        BlockButton(
            text = "Set a higher refresh rate",
            onClick = { context.startActivity(PerfTools.displaySettingsIntent()) },
            modifier = Modifier.fillMaxWidth(),
            color = CL.Mint
        )
        BlockButton(
            text = "Battery saver settings",
            onClick = { context.startActivity(PerfTools.batterySettingsIntent()) },
            modifier = Modifier.fillMaxWidth(),
            color = CL.Butter
        )
        TextButton(onClick = { refresh++ }) {
            Text("Refresh status", color = CL.Mist)
        }
        Text(
            "These controls use normal Android settings. CLblox never changes the Roblox app or its FPS limit.",
            color = CL.MistDim,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AddGameDialog(onDismiss: () -> Unit, onAdd: (link: String, name: String) -> Unit) {
    var link by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CL.NightRaised,
        titleContentColor = CL.Mist,
        textContentColor = CL.Mist,
        title = { Text("Add a game", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Game link or place ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = { BlockButton("Add game", { onAdd(link, name) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CL.Mist) } }
    )
}
