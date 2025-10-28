package com.example.educanet

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> vm.setAvatar(uri?.toString()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar local (galería)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ui.avatarUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(ui.avatarUri),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { pickImage.launch("image/*") }) {
                    Text("Elegir avatar (galería)")
                }
            }

            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recordar sesión", modifier = Modifier.weight(1f))
                Switch(checked = ui.rememberSession, onCheckedChange = vm::setRemember)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modo oscuro (flag)", modifier = Modifier.weight(1f))
                Switch(checked = ui.darkMode, onCheckedChange = vm::setDark)
            }

            Text(
                "El avatar se guarda localmente (DataStore). " +
                        "Si activas Storage más adelante podemos subirlo."
            )
        }
    }
}
