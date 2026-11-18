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
private val Muted = Color(0xFFBFAFD4)

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
                NicolasKeepAliveApp(
                    onAction = ::runBotAction,
                    onCopySetup = ::copySetupCommand,
                    onCopyPairing = ::copyPairingCommand,
                    onNewSession = ::startNewSession,
                    onOpenAppSettings = ::openAppSettings,
                    onOpenTermux = ::openTermux,
                    onOpenBatterySettings = ::openBatterySettings,
                    onOpenRepository = ::openRepository
                )
            }
        }
    }

    private fun runBotAction(action: BotAction) {
        val prefs = getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
        val serviceAction = when (action) {
            BotAction.START -> KeepAliveService.ACTION_START
            BotAction.STOP -> KeepAliveService.ACTION_STOP
            BotAction.RESTART -> KeepAliveService.ACTION_RESTART
            else -> null
        }

        try {
            if (serviceAction != null) {
                val pendingText = when (action) {
                    BotAction.START -> "Solicitando início do Robô Nicolas…"
                    BotAction.STOP -> "Solicitando parada segura do Robô Nicolas…"
                    else -> "Solicitando reinicialização do Robô Nicolas…"
                }
                prefs.edit().putString(TermuxBridge.KEY_LAST_MESSAGE, pendingText).apply()
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, KeepAliveService::class.java).setAction(serviceAction)
                )
                Toast.makeText(this, "Comando " + action.name + " enviado.", Toast.LENGTH_SHORT).show()
            } else {
                val accepted = TermuxBridge.execute(this, action)
                val message = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, null)
                    ?: "Não foi possível enviar o comando."
                Toast.makeText(
                    this,
                    if (accepted) "Solicitando " + action.name + "…" else message,
                    if (accepted) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            val message = "Não foi possível executar " + action.name + ": " +
                (e.message ?: e.javaClass.simpleName)
            prefs.edit().putString(TermuxBridge.KEY_LAST_MESSAGE, message).apply()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun startNewSession() {
        val started = TermuxBridge.startNewSession(this)
        val prefs = getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
        val message = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Não foi possível abrir o pareamento.")
            ?: "Não foi possível abrir o pareamento."
        Toast.makeText(
            this,
            if (started) "Terminal de pareamento aberto no Termux. Siga as instruções na tela." else message,
            if (started) Toast.LENGTH_LONG else Toast.LENGTH_LONG
        ).show()
    }
    private fun copyText(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copiado.", Toast.LENGTH_LONG).show()
    }

    private fun copySetupCommand() = copyText("Comando de configuração do Termux", TermuxBridge.SETUP_COMMAND)

    private fun copyPairingCommand() =
        copyText("Comando de primeiro acesso", "cd ~/Robo-Nicolas && bash start.sh")

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir as permissões: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun openTermux() {
        try {
            val launch = packageManager.getLaunchIntentForPackage(TermuxBridge.PACKAGE_TERMUX)
            if (launch == null) {
                Toast.makeText(this, "Termux não está instalado.", Toast.LENGTH_LONG).show()
            } else {
                startActivity(launch)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir o Termux: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun openBatterySettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    private fun openRepository() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/heyninja67-png/Starter-do-Nicolazinho")))
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir o repositório: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun NicolasKeepAliveApp(
    onAction: (BotAction) -> Unit,
    onCopySetup: () -> Unit,
    onCopyPairing: () -> Unit,
    onNewSession: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenTermux: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenRepository: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
    }
    var processRunning by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)) }
    var whatsappConnected by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)) }
    var lastMessage by remember {
        mutableStateOf(prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Pronto para conectar ao Termux.") ?: "Pronto para conectar ao Termux.")
    }
    var lastOutput by remember { mutableStateOf(prefs.getString("last_command_output", "") ?: "") }
    var autoRestart by remember { mutableStateOf(prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)) }
    var termuxPermission by remember { mutableStateOf(TermuxBridge.hasRunCommandPermission(context)) }
    var tab by remember { mutableIntStateOf(0) }
    val termuxInstalled = remember { TermuxBridge.isInstalled(context) }

    LaunchedEffect(prefs) {
        while (true) {
            processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
            whatsappConnected = prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
            lastMessage = prefs.getString(TermuxBridge.KEY_LAST_MESSAGE, "Pronto para conectar ao Termux.") ?: "Pronto para conectar ao Termux."
            lastOutput = prefs.getString("last_command_output", "") ?: ""
            autoRestart = prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
            termuxPermission = TermuxBridge.hasRunCommandPermission(context)
            delay(700)
        }
    }

    val titles = listOf("Início", "Robô", "Versões", "Parear", "Ajustes")
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF180B2B), Deep))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> HomePage(
                        processRunning, whatsappConnected, termuxInstalled, termuxPermission,
                        lastMessage, onAction, onCopySetup, onOpenAppSettings
                    )
                    1 -> BotPage(processRunning, whatsappConnected, lastMessage, lastOutput, onAction)
                    2 -> VersionsPage(onOpenRepository)
                    3 -> PairingPage(termuxInstalled, onCopyPairing, onOpenTermux, onNewSession)
                    else -> SettingsPage(
                        autoRestart, termuxPermission,
                        onAutoRestartChange = {
                            prefs.edit().putBoolean(TermuxBridge.KEY_AUTO_RESTART, it).apply()
                            autoRestart = it
                        },
                        onCopySetup = onCopySetup,
                        onOpenAppSettings = onOpenAppSettings,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onOpenTermux = onOpenTermux
                    )
                }
            }

            NavigationBar(containerColor = Color(0xFF160C26)) {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(listOf("⌂", "●", "↻", "⌁", "⚙")[index]) },
                        label = { Text(title, maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Gold, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomePage(
    processRunning: Boolean,
    whatsappConnected: Boolean,
    termuxInstalled: Boolean,
    termuxPermission: Boolean,
    lastMessage: String,
    onAction: (BotAction) -> Unit,
    onCopySetup: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    PageColumn("Nicolas Keep Alive", "Painel de controle do Robô Nicolas") {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF24133D)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ROBÔ NICOLAS", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        whatsappConnected -> "● Processo ativo • WhatsApp conectado"
                        processRunning -> "● Processo ativo • verificando WhatsApp"
                        else -> "● Robô parado ou aguardando resposta do Termux"
                    },
                    color = when {
                        whatsappConnected -> Color(0xFF8EF0A8)
                        processRunning -> Gold
                        else -> Color(0xFFFF8A80)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAction(BotAction.START) },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.weight(1f)
                    ) { Text("INICIAR", color = Deep) }
                    OutlinedButton(
                        onClick = { onAction(BotAction.STOP) },
                        modifier = Modifier.weight(1f)
                    ) { Text("PARAR") }
                }
                OutlinedButton(
                    onClick = { onAction(BotAction.RESTART) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("REINICIAR ROBÔ") }
            }
        }

        StatusCard("Processo do bot", if (processRunning) "Processo ativo" else "Sem processo confirmado")
        StatusCard("WhatsApp / Baileys", if (whatsappConnected) "Conectado" else "Sem confirmação de conexão")
        StatusCard(
            "Termux",
            when {
                !termuxInstalled -> "Termux não instalado"
                !termuxPermission -> "Permissão RUN_COMMAND não concedida"
                else -> "Instalado • permissão concedida"
            }
        )
        Button(
            onClick = { onAction(BotAction.STATUS) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) { Text("ATUALIZAR STATUS") }
        if (true) { // Sempre mostrar as instruções: allow-external-apps pode estar desativado mesmo com RUN_COMMAND concedida.
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1A43)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LIBERAR INTEGRAÇÃO", color = Gold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Instale o Termux e habilite RUN_COMMAND nas permissões do Android. No Termux, também execute o comando de configuração abaixo.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onCopySetup,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) { Text("COPIAR COMANDO DO TERMUX", color = Deep) }
                    OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("ABRIR PERMISSÕES DO APP")
                    }
                }
            }
        }
        InfoCard("Último resultado", lastMessage)
    }
}

@Composable
private fun BotPage(
    processRunning: Boolean,
    whatsappConnected: Boolean,
    lastMessage: String,
    lastOutput: String,
    onAction: (BotAction) -> Unit
) {
    PageColumn("Controle do Robô", "Ações enviadas ao bot já instalado em ~/Robo-Nicolas") {
        StatusCard("Processo", if (processRunning) "Ativo" else "Parado / não confirmado")
        StatusCard("WhatsApp", if (whatsappConnected) "Conectado" else "Não confirmado")
        Button(
            onClick = { onAction(BotAction.START) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) { Text("INICIAR ROBÔ", color = Deep) }
        OutlinedButton(onClick = { onAction(BotAction.STOP) }, modifier = Modifier.fillMaxWidth()) {
            Text("PARAR ROBÔ")
        }
        OutlinedButton(onClick = { onAction(BotAction.RESTART) }, modifier = Modifier.fillMaxWidth()) {
            Text("REINICIAR ROBÔ")
        }
        OutlinedButton(onClick = { onAction(BotAction.STATUS) }, modifier = Modifier.fillMaxWidth()) {
            Text("CONSULTAR STATUS")
        }
        OutlinedButton(onClick = { onAction(BotAction.LOGS) }, modifier = Modifier.fillMaxWidth()) {
            Text("CARREGAR LOGS")
        }
        InfoCard("Resposta mais recente", lastMessage)
        InfoCard("Saída do Termux", if (lastOutput.isBlank()) "Nenhum log recebido ainda. Toque em CARREGAR LOGS." else lastOutput)
    }
}

@Composable
private fun VersionsPage(onOpenRepository: () -> Unit) {
    PageColumn("Versões", "Informações da instalação deste aplicativo") {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF24133D)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NICOLAS KEEP ALIVE", color = Gold, style = MaterialTheme.typography.titleLarge)
                Text("Versão " + BuildConfig.VERSION_NAME, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Código " + BuildConfig.VERSION_CODE, color = Muted)
                Text(
                    "O app controla a instalação existente no Termux. Não substitui o Robô Nicolas nem altera a sessão salva do WhatsApp.",
                    color = Muted
                )
            }
        }
        Button(
            onClick = onOpenRepository,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) { Text("ABRIR REPOSITÓRIO / ATUALIZAÇÕES") }
        InfoCard(
            "Atualização automática",
            "Ainda não existe um servidor de atualização configurado neste projeto. Use o repositório para consultar as versões publicadas."
        )
    }
}

@Composable
private fun PairingPage(
    termuxInstalled: Boolean,
    onCopyPairing: () -> Unit,
    onOpenTermux: () -> Unit,
    onNewSession: () -> Unit
) {
    var confirmNewSession by remember { mutableStateOf(false) }
    PageColumn("Pareamento do WhatsApp", "Gerencie a sessão Baileys do Robô Nicolas") {
        InfoCard(
            "Sessão atual protegida",
            "O uso normal mantém a sessão salva em ~/Robo-Nicolas/auth. Para trocar uma sessão antiga, use a opção abaixo: ela primeiro para o bot e move a pasta auth para um backup, sem apagá-la."
        )
        StatusCard("Termux", if (termuxInstalled) "Instalado" else "Não encontrado")
        Button(
            onClick = onOpenTermux,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) { Text("ABRIR TERMUX") }
        OutlinedButton(onClick = onCopyPairing, modifier = Modifier.fillMaxWidth()) {
            Text("COPIAR COMANDO DE PRIMEIRO LOGIN")
        }
        Button(
            onClick = { confirmNewSession = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) { Text("TROCAR SESSÃO / NOVO PAREAMENTO", color = Deep) }
        InfoCard(
            "Depois do pareamento",
            "Escolha código ou QR e conclua no WhatsApp. Quando aparecer a mensagem de conexão, pressione Ctrl+C no Termux para encerrar o modo interativo sem fazer logout. Volte ao app e toque em INICIAR ROBÔ para usar a sessão nova em segundo plano."
        )
        if (confirmNewSession) {
            AlertDialog(
                onDismissRequest = { confirmNewSession = false },
                title = { Text("Criar uma sessão nova?") },
                text = {
                    Text(
                        "O bot será parado e a pasta auth atual será renomeada para auth.backup-DATA-HORA. Nada será apagado. Depois, o Termux abrirá o pareamento do WhatsApp em um terminal interativo. Você precisa ter acesso ao WhatsApp para confirmar a nova conexão."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmNewSession = false
                        onNewSession()
                    }) { Text("FAZER BACKUP E CONTINUAR") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmNewSession = false }) { Text("CANCELAR") }
                }
            )
        }
    }
}
@Composable
private fun SettingsPage(
    autoRestart: Boolean,
    termuxPermission: Boolean,
    onAutoRestartChange: (Boolean) -> Unit,
    onCopySetup: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenTermux: () -> Unit
) {
    PageColumn("Configurações", "Permissões e comportamento do monitor") {
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
                    Text(
                        "Tenta recuperar o supervisor após uma queda, usando a sessão já salva.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = autoRestart, onCheckedChange = onAutoRestartChange)
            }
        }
        StatusCard(
            "Permissão RUN_COMMAND",
            if (termuxPermission) "Concedida" else "Precisa ser liberada nas permissões do Android"
        )
        Button(
            onClick = onCopySetup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) { Text("COPIAR CONFIGURAÇÃO DO TERMUX", color = Deep) }
        OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
            Text("ABRIR PERMISSÕES DO NICOLAS KEEP ALIVE")
        }
        OutlinedButton(onClick = onOpenTermux, modifier = Modifier.fillMaxWidth()) {
            Text("ABRIR TERMUX")
        }
        OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
            Text("CONFIGURAR BATERIA / SEGUNDO PLANO")
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1233)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Gold)
                Text(value, color = Muted)
            }
            Text("●", color = Color(0xFF8EF0A8))
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1A43)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Gold, style = MaterialTheme.typography.titleSmall)
            Text(value, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
