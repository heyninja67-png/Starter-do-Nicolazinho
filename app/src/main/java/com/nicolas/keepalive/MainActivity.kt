package com.nicolas.keepalive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nicolas.keepalive.services.KeepAliveService
import com.nicolas.keepalive.termux.BotAction
import com.nicolas.keepalive.termux.TermuxBridge
import kotlinx.coroutines.delay

private val Purple = Color(0xFF6D28D9)
private val Deep = Color(0xFF10081E)
private val Gold = Color(0xFFFFD54A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Gold,
                    secondary = Purple,
                    background = Deep,
                    surface = Color(0xFF1A0F2E)
                )
            ) {
                HomeScreen(
                    onStart = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, KeepAliveService::class.java)
                                .setAction(KeepAliveService.ACTION_START)
                        )
                    },
                    onStop = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, KeepAliveService::class.java)
                                .setAction(KeepAliveService.ACTION_STOP)
                        )
                    },
                    onRestart = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, KeepAliveService::class.java)
                                .setAction(KeepAliveService.ACTION_RESTART)
                        )
                    },
                    onRefresh = { TermuxBridge.execute(this, BotAction.STATUS) },
                    onAutoRestartChange = { enabled ->
                        getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
                            .edit().putBoolean(TermuxBridge.KEY_AUTO_RESTART, enabled).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRefresh: () -> Unit,
    onAutoRestartChange: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(TermuxBridge.PREFS, android.content.Context.MODE_PRIVATE)
    }
    var processRunning by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)) }
    var whatsappConnected by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)) }
    var lastMessage by remember { mutableStateOf(prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando.") }
    var autoRestart by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)) }
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Início", "Robô Nicolas", "Versões", "Parear", "Configurações")
    val termuxInstalled = remember { TermuxBridge.isInstalled(context) }
    val termuxPermission = remember { TermuxBridge.hasRunCommandPermission(context) }

    LaunchedEffect(prefs) {
        while (true) {
            processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
            whatsappConnected = prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
            lastMessage = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando."
            autoRestart = prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
            delay(1000)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF180B2B), Deep))
        )
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("Nicolas Keep Alive", color = Gold, style = MaterialTheme.typography.headlineSmall)
            Text("Seu painel do Robô Nicolas", color = Color(0xFFBFAFD4))
            Spacer(Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF24133D)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("ROBÔ NICOLAS", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            whatsappConnected -> "● Processo ativo • WhatsApp conectado"
                            processRunning -> "● Processo ativo • verificando conexão WhatsApp"
                            else -> "● Robô parado ou aguardando resposta do Termux"
                        },
                        color = when {
                            whatsappConnected -> Color(0xFF8EF0A8)
                            processRunning -> Gold
                            else -> Color(0xFFFF8A80)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                            modifier = Modifier.weight(1f)
                        ) { Text("INICIAR", color = Deep) }
                        OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                            Text("PARAR")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                        Text("REINICIAR ROBÔ")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            StatusCard(
                "Processo do bot",
                if (processRunning) "PID de inicialização ativo" else "Sem processo confirmado"
            )
            StatusCard(
                "WhatsApp / Baileys",
                when {
                    whatsappConnected -> "Conectado (mensagem de autenticação detectada)"
                    processRunning -> "Inicializando ou reconectando com a sessão salva"
                    else -> "Sem confirmação de conexão"
                }
            )
            StatusCard(
                "Termux",
                when {
                    !termuxInstalled -> "Termux não instalado"
                    !termuxPermission -> "Falta a permissão RUN_COMMAND"
                    else -> "Instalado e autorizado"
                }
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reconexão automática", color = Gold)
                    Text("Reinicia o processo se ele cair após uma conexão válida.", color = Color(0xFFBFAFD4), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = autoRestart,
                    onCheckedChange = {
                        autoRestart = it
                        onAutoRestartChange(it)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("ATUALIZAR STATUS")
            }
            Text(
                lastMessage.take(240),
                color = Color(0xFFBFAFD4),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.weight(1f))
            NavigationBar(containerColor = Color(0xFF160C26)) {
                titles.forEachIndexed { i, title ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(if (i == 0) "⌂" else "•") },
                        label = { Text(title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1233)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Gold)
                Text(value, color = Color(0xFFC8BCD7))
            }
            Text("●", color = Color(0xFF8EF0A8))
        }
    }
}
