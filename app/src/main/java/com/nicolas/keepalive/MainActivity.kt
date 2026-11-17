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
import com.nicolas.keepalive.services.KeepAliveService

private val Purple = Color(0xFF6D28D9)
private val Deep = Color(0xFF10081E)
private val Gold = Color(0xFFFFD54A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Gold,
                secondary = Purple,
                background = Deep,
                surface = Color(0xFF1A0F2E)
            )) {
                HomeScreen(
                    onStart = { startService(Intent(this, KeepAliveService::class.java)) },
                    onStop = { stopService(Intent(this, KeepAliveService::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit, onStop: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Início", "Robô Nicolas", "Versões", "Parear", "Configurações")

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
                        if (running) "● Serviço de monitoramento ativo" else "● Desconectado",
                        color = if (running) Color(0xFF8EF0A8) else Color(0xFFFF8A80)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { running = true; onStart() }, colors = ButtonDefaults.buttonColors(containerColor = Gold), modifier = Modifier.weight(1f)) {
                            Text("INICIAR", color = Deep)
                        }
                        OutlinedButton(onClick = { running = false; onStop() }, modifier = Modifier.weight(1f)) {
                            Text("PARAR")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            StatusCard("Bateria", "Monitoramento pelo Android")
            StatusCard("Internet", "Verificada pelo serviço")
            StatusCard("Termux", "Integração controlada")
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Gold)
                Text(value, color = Color(0xFFC8BCD7))
            }
            Text("●", color = Color(0xFF8EF0A8))
        }
    }
}
