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
import androidx.compose.runtime.saveable.rememberSaveable
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
private val Muted = Color(0xFFC8BCD7)

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
                    onStart = { requestServiceAction(KeepAliveService.ACTION_START, "iniciar") },
                    onStop = { requestServiceAction(KeepAliveService.ACTION_STOP, "parar") },
                    onRestart = { requestServiceAction(KeepAliveService.ACTION_RESTART, "reiniciar") },
                    onRefresh = {
                        val ok = TermuxBridge.execute(this, BotAction.STATUS)
                        val message = getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
                            .getString(TermuxBridge.KEY_LAST_MESSAGE, "") ?: ""
                        Toast.makeText(
                            this,
                            if (ok) "Consultando o Termux..." else message.ifBlank { "Não foi possível consultar o Termux." },
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onShowLogs = {
                        val ok = TermuxBridge.execute(this, BotAction.LOGS)
                        val message = getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
                            .getString(TermuxBridge.KEY_LAST_MESSAGE, "") ?: ""
                        Toast.makeText(
                            this,
                            if (ok) "Buscando os logs no Termux..." else message.ifBlank { "Não foi possível buscar os logs." },
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onCopySetup = {
                        copyText(TermuxBridge.SETUP_COMMAND, "Comando de integração copiado.")
                    },
                    onCopyPairing = {
                        copyText("cd ~/Robo-Nicolas && bash start.sh", "Comando de autenticação copiado.")
                    },
                    onOpenPermissions = { openAppSettings() },
                    onOpenTermux = { openTermux() },
                    onOpenRepository = { openUrl("https://github.com/heyninja67-png/Starter-do-Nicolazinho") },
                    onAutoRestartChanged = { enabled ->
                        getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
                            .edit().putBoolean(TermuxBridge.KEY_AUTO_RESTART, enabled).apply()
                        Toast.makeText(
                            this,
                            if (enabled) "Reconexão automática ativada." else "Reconexão automática desativada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun requestServiceAction(action: String, label: String) {
        val prefs = getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
        prefs.edit().putString(TermuxBridge.KEY_LAST_MESSAGE, "Solicitando ao Android: $label o Robô Nicolas...").apply()
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, KeepAliveService::class.java).setAction(action)
            )
            Toast.makeText(this, "Pedido para $label enviado.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val message = "O Android não iniciou o serviço: ${e.message ?: e.javaClass.simpleName}"
            prefs.edit().putString(TermuxBridge.KEY_LAST_MESSAGE, message).apply()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun copyText(value: String, confirmation: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nicolas Keep Alive", value))
        Toast.makeText(this, confirmation, Toast.LENGTH_LONG).show()
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir as permissões: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openTermux() {
        try {
            val launch = packageManager.getLaunchIntentForPackage(TermuxBridge.PACKAGE_TERMUX)
            if (launch == null) {
                Toast.makeText(this, "Não encontrei o Termux instalado.", Toast.LENGTH_LONG).show()
            } else {
                startActivity(launch)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir o Termux: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum navegador disponível para abrir o link.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun HomeScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRefresh: () -> Unit,
    onShowLogs: () -> Unit,
    onCopySetup: () -> Unit,
    onCopyPairing: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTermux: () -> Unit,
    onOpenRepository: () -> Unit,
    onAutoRestartChanged: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
    }
    var processRunning by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)) }
    var whatsappConnected by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)) }
    var lastMessage by remember {
        mutableStateOf(prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando.")
    }
    var lastOutput by remember {
        mutableStateOf(prefs.getString("last_command_output", "") ?: "")
    }
    var autoRestart by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)) }
    var termuxPermission by remember { mutableStateOf(TermuxBridge.hasRunCommandPermission(context)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val termuxInstalled = remember { TermuxBridge.isInstalled(context) }

    LaunchedEffect(prefs) {
        while (true) {
            processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
            whatsappConnected = prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
            lastMessage = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Aguardando comando.") ?: "Aguardando comando."
            lastOutput = prefs.getString("last_command_output", "") ?: ""
            autoRestart = prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
            termuxPermission = TermuxBridge.hasRunCommandPermission(context)
            delay(700)
        }
    }

    val processText = if (processRunning) "Processo do bot ativo" else "Processo não confirmado"
    val whatsappText = when {
        whatsappConnected -> "WhatsApp conectado"
        processRunning -> "Bot ativo; WhatsApp verificando/reconectando"
        else -> "WhatsApp sem confirmação de conexão"
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF180B2B), Deep))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text("Nicolas Keep Alive", color = Gold, style = MaterialTheme.typography.headlineSmall)
                Text("Painel de controle do Robô Nicolas", color = Color(0xFFBFAFD4))
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    0 -> HomePage(
                        processRunning = processRunning,
                        whatsappConnected = whatsappConnected,
                        termuxInstalled = termuxInstalled,
                        termuxPermission = termuxPermission,
                        lastMessage = lastMessage,
                        onStart = onStart,
                        onStop = onStop,
                        onRestart = onRestart,
                        onRefresh = onRefresh
                    )
                    1 -> BotPage(
                        processText = processText,
                        whatsappText = whatsappText,
                        lastMessage = lastMessage,
                        lastOutput = lastOutput,
                        onStart = onStart,
                        onStop = onStop,
                        onRestart = onRestart,
                        onRefresh = onRefresh,
                        onShowLogs = onShowLogs
                    )
                    2 -> VersionsPage(
                        onOpenRepository = onOpenRepository,
                        onRefresh = onRefresh
                    )
                    3 -> PairingPage(
                        lastOutput = lastOutput,
                        onOpenTermux = onOpenTermux,
                        onCopyPairing = onCopyPairing
                    )
                    else -> SettingsPage(
                        autoRestart = autoRestart,
                        termuxInstalled = termuxInstalled,
                        termuxPermission = termuxPermission,
                        onAutoRestartChanged = onAutoRestartChanged,
                        onCopySetup = onCopySetup,
                        onOpenPermissions = onOpenPermissions,
                        onOpenTermux = onOpenTermux
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            NavigationBar(containerColor = Color(0xFF160C26)) {
                val titles = listOf("Início", "Robô", "Versões", "Parear", "Configurações")
                val symbols = listOf("⌂", "●", "↻", "⌁", "⚙")
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(symbols[index], color = if (tab == index) Gold else Muted) },
                        label = { Text(title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    processRunning: Boolean,
    whatsappConnected: Boolean,
    termuxInstalled: Boolean,
    termuxPermission: Boolean,
    lastMessage: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24133D)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("ROBÔ NICOLAS", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    whatsappConnected -> "● Processo ativo • WhatsApp conectado"
                    processRunning -> "● Processo ativo • verificando WhatsApp"
                    else -> "● Robô parado ou aguardando o Termux"
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
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text("PARAR") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text("REINICIAR ROBÔ")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("ATUALIZAR STATUS")
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    StatusCard("Termux", when {
        !termuxInstalled -> "Termux não está instalado"
        !termuxPermission -> "Falta autorizar RUN_COMMAND nas permissões do Android"
        else -> "Instalado e com permissão RUN_COMMAND"
    })
    StatusCard("Processo", if (processRunning) "Em execução" else "Sem confirmação de execução")
    StatusCard("WhatsApp / Baileys", if (whatsappConnected) "Conectado" else "Aguardando confirmação")
    MessageCard("Último resultado", lastMessage)
}

@Composable
private fun BotPage(
    processText: String,
    whatsappText: String,
    lastMessage: String,
    lastOutput: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRefresh: () -> Unit,
    onShowLogs: () -> Unit
) {
    SectionTitle("Controle do robô")
    StatusCard("Processo", processText)
    StatusCard("WhatsApp / Baileys", whatsappText)
    Spacer(Modifier.height(10.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
        Text("INICIAR ROBÔ", color = Deep)
    }
    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("PARAR ROBÔ") }
    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("REINICIAR ROBÔ") }
    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("ATUALIZAR STATUS") }
    OutlinedButton(onClick = onShowLogs, modifier = Modifier.fillMaxWidth()) { Text("CARREGAR LOGS DO TERMUX") }
    Spacer(Modifier.height(10.dp))
    MessageCard("Último resultado", lastMessage)
    MessageCard("Saída do último comando", lastOutput.ifBlank { "Nenhum log foi carregado nesta sessão. Toque em CARREGAR LOGS DO TERMUX." })
}

@Composable
private fun VersionsPage(onOpenRepository: () -> Unit, onRefresh: () -> Unit) {
    SectionTitle("Versão do aplicativo")
    MessageCard("Nicolas Keep Alive", "Versão instalada: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE}).")
    MessageCard(
        "Sobre as atualizações",
        "O app controla o Robô Nicolas que já existe no Termux. Esta tela mostra a versão instalada e abre o repositório do projeto; ela não substitui nem atualiza os arquivos do bot."
    )
    Button(onClick = onOpenRepository, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
        Text("ABRIR REPOSITÓRIO NO GITHUB", color = Deep)
    }
    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("VERIFICAR STATUS DO ROBÔ")
    }
}

@Composable
private fun PairingPage(lastOutput: String, onOpenTermux: () -> Unit, onCopyPairing: () -> Unit) {
    SectionTitle("Sessão do WhatsApp")
    val sessionText = when {
        lastOutput.contains("SESSION=SAVED") -> "Sessão Baileys salva no Termux."
        lastOutput.contains("SESSION=MISSING") -> "Não foi encontrada uma sessão salva no último diagnóstico."
        else -> "Consulte o status do robô para verificar a sessão existente."
    }
    MessageCard("Sessão atual", sessionText)
    MessageCard(
        "Importante",
        "O Nicolas Keep Alive não apaga nem recria a sessão. Para fazer um primeiro pareamento ou corrigir um logout, abra o Termux e execute o comando manual. Quando a sessão já existe, o app usa os arquivos salvos em ~/Robo-Nicolas/auth."
    )
    Button(onClick = onOpenTermux, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
        Text("ABRIR TERMUX", color = Deep)
    }
    OutlinedButton(onClick = onCopyPairing, modifier = Modifier.fillMaxWidth()) {
        Text("COPIAR COMANDO DE AUTENTICAÇÃO")
    }
}

@Composable
private fun SettingsPage(
    autoRestart: Boolean,
    termuxInstalled: Boolean,
    termuxPermission: Boolean,
    onAutoRestartChanged: (Boolean) -> Unit,
    onCopySetup: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTermux: () -> Unit
) {
    SectionTitle("Configurações")
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24133D)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Reconexão automática", color = Gold)
                Text("Tenta recuperar o supervisor após uma queda, depois de uma conexão já confirmada.", color = Muted)
            }
            Switch(checked = autoRestart, onCheckedChange = onAutoRestartChanged)
        }
    }
    Spacer(Modifier.height(10.dp))
    StatusCard("Termux instalado", if (termuxInstalled) "Sim" else "Não")
    StatusCard("Permissão RUN_COMMAND", if (termuxPermission) "Concedida" else "Pendente")
    Button(onClick = onCopySetup, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
        Text("COPIAR COMANDO DE CONFIGURAÇÃO", color = Deep)
    }
    OutlinedButton(onClick = onOpenTermux, modifier = Modifier.fillMaxWidth()) { Text("ABRIR TERMUX") }
    OutlinedButton(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
        Text("ABRIR PERMISSÕES DO APLICATIVO")
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Gold, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1233)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Gold)
            Text(value, color = Muted)
        }
    }
}

@Composable
private fun MessageCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1A43)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Gold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Muted)
        }
    }
}
