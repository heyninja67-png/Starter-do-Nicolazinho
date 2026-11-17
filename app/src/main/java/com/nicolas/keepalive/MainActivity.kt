package com.nicolas.keepalive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                            Intent(this, KeepAliveService::class.java).setAction(KeepAliveService.ACTION_START)
                        )
                    },
                    onStop = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, KeepAliveService::class.java).setAction(KeepAliveService.ACTION_STOP)
                        )
                    },
                    onRestart = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, KeepAliveService::class.java).setAction(KeepAliveService.ACTION_RESTART)
                        )
                    },
                    onRefresh = { TermuxBridge.execute(this, BotAction.STATUS) },
                    onCopySetup = {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Configurar Termux", TermuxBridge.SETUP_COMMAND))
                        Toast.makeText(this, "Comando copiado. Cole no Termux e execute.", Toast.LENGTH_LONG).show()
                    },
                    onOpenPermissions = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
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
    onCopySetup: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
    }
    var processRunning by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)) }
    var whatsappConnected by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)) }
    var lastMessage by remember { mutableStateOf(prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando.") }
    var autoRestart by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)) }
    var tab by remember { mutableStateOf(0) }
    var termuxPermission by remember { mutableStateOf(TermuxBridge.hasRunCommandPermission(context)) }
    val termuxInstalled = remember { TermuxBridge.isInstalled(context) }
    val titles = listOf("Início", "Robô Nicolas", "Versões", "Parear", "Configurações")

    LaunchedEffect(prefs) {
        while (true) {
            processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
            whatsappConnected = prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
            lastMessage = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando."
            autoRestart = prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
            termuxPermission = TermuxBridge.hasRunCommandPermission(context)
            delay(1000)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF180B2B), Deep))
        )
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {\n            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
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

            Spacer(Modifier.height(12.dp))
            StatusCard(
                "Processo do bot",
                if (processRunning) "Supervisor start.sh ativo" else "Sem processo confirmado"
            )
            StatusCard(
                "WhatsApp / Baileys",
                when {
                    whatsappConnected -> "Conectado • autenticação confirmada pelo log"
                    processRunning -> "Inicializando ou reconectando com a sessão salva"
                    else -> "Sem confirmação de conexão"
                }
            )
            StatusCard(
                "Termux",
                when {
                    !termuxInstalled -> "Termux não instalado"
                    !termuxPermission -> "Permissão RUN_COMMAND ainda não concedida"
                    else -> "Instalado • permissão RUN_COMMAND concedida"
                }
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1A43)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("LIBERAR INTEGRAÇÃO NO TERMUX", color = Gold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "O Termux exige allow-external-apps=true antes de aceitar comandos externos. Copie o comando, abra o Termux, cole e execute uma vez. Depois permita 'Run commands in Termux environment' nas permissões deste aplicativo.",
                        color = Color(0xFFC8BCD7),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onCopySetup,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("COPIAR COMANDO PARA O TERMUX", color = Deep) }
                    OutlinedButton(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("ABRIR PERMISSÕES DO NICOLAS KEEP ALIVE")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reconexão automática", color = Gold)
                    Text(
                        "Recupera o supervisor se ele cair após uma conexão confirmada.",
                        color = Color(0xFFBFAFD4),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = autoRestart, onCheckedChange = {
                    autoRestart = it
                    prefs.edit().putBoolean(TermuxBridge.KEY_AUTO_RESTART, it).apply()
                })
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("ATUALIZAR STATUS")
            }
            Text(
                lastMessage.take(320),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Gold)
                Text(value, color = Color(0xFFC8BCD7))
            }
            Text("●", color = Color(0xFF8EF0A8))
        }
    }
}
