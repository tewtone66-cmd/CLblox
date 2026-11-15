package com.clblox.launcher

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4F8CFF),
                    background = Color(0xFF0E1117),
                    surface = Color(0xFF171B24)
                )
            ) {
                CLbloxApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CLbloxApp() {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("CLblox") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Games") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Performance") })
            }
            if (tab == 0) GamesScreen() else PerformanceScreen()
        }
    }
}

private fun toast(context: Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

@Composable
fun GamesScreen() {
    val context = LocalContext.current
    val store = remember { GameStore(context) }
    val games = remember { mutableStateListOf<Game>().apply { addAll(store.load()) } }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add game")
            }
        }
    ) { padding ->
        if (games.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No games yet. Tap + and paste a Roblox game link or place ID.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games, key = { it.placeId }) { game ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(game.name, style = MaterialTheme.typography.titleMedium)
                                Text("ID: ${game.placeId}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                if (!RobloxLauncher.isInstalled(context)) {
                                    toast(context, "Roblox is not installed")
                                    RobloxLauncher.openStorePage(context)
                                } else if (!RobloxLauncher.launchGame(context, game.placeId)) {
                                    toast(context, "Could not open Roblox")
                                }
                            }) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                            IconButton(onClick = {
                                games.remove(game)
                                store.save(games.toList())
                            }) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var link by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add game") },
            text = {
                Column {
                    OutlinedTextField(
                        value = link, onValueChange = { link = it },
                        label = { Text("Game link or place ID") }, singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Name (optional)") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = RobloxLauncher.parsePlaceId(link)
                    if (id == null) {
                        toast(context, "Invalid link or ID")
                    } else if (games.any { it.placeId == id }) {
                        toast(context, "Already added")
                    } else {
                        games.add(Game(id, name.ifBlank { "Game $id" }))
                        store.save(games.toList())
                        showDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PerformanceScreen() {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) } // bump to re-read statuses
    val thermal = remember(refresh) { PerfTools.thermalStatus(context) }
    val saver = remember(refresh) { PerfTools.batterySaverOn(context) }
    val mode = remember(refresh) { PerfTools.gameMode(context) }
    val dnd = remember(refresh) { PerfTools.dndAccessGranted(context) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Device status", style = MaterialTheme.typography.titleMedium)
                Text("Temperature: $thermal")
                Text("Battery saver: ${if (saver) "ON (turn off for best FPS)" else "off"}")
                Text("Game Mode: $mode")
            }
        }

        Button(onClick = {
            if (PerfTools.dndAccessGranted(context)) {
                PerfTools.setDnd(context, true)
                toast(context, "Do Not Disturb on")
            } else {
                context.startActivity(PerfTools.dndPermissionIntent())
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (dnd) "Enable Do Not Disturb" else "Allow Do Not Disturb access")
        }
        OutlinedButton(onClick = {
            PerfTools.setDnd(context, false)
            toast(context, "Do Not Disturb off")
        }, modifier = Modifier.fillMaxWidth(), enabled = dnd) { Text("Disable Do Not Disturb") }

        OutlinedButton(
            onClick = { context.startActivity(PerfTools.displaySettingsIntent()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Display settings (set high refresh rate)") }
        OutlinedButton(
            onClick = { context.startActivity(PerfTools.batterySettingsIntent()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Battery saver settings") }
        TextButton(onClick = { refresh++ }) { Text("Refresh status") }

        Text(
            "These helpers only use normal Android settings. CLblox does not change the Roblox app or raise its FPS cap.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
