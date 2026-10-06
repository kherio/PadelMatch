package com.kherio.padelmatch.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.BuildConfig
import com.kherio.padelmatch.data.AppRelease
import com.kherio.padelmatch.data.UpdateCheckResult
import com.kherio.padelmatch.data.UpdateRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { UpdateRepository() }

    var result by remember {
        mutableStateOf<UpdateCheckResult?>(null)
    }
    var checking by remember { mutableStateOf(false) }

    fun check() {
        checking = true
        scope.launch {
            result = repository.checkForUpdate(BuildConfig.VERSION_NAME)
            checking = false
        }
    }

    LaunchedEffect(Unit) {
        check()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Actualizaciones", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "Actualizaciones"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { check() },
                        enabled = !checking
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Buscar actualizaciones"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            Icon(
                imageVector = when (result) {
                    is UpdateCheckResult.Available -> Icons.Default.Update
                    is UpdateCheckResult.UpToDate -> Icons.Default.CheckCircle
                    is UpdateCheckResult.Error -> Icons.Default.ErrorOutline
                    null -> Icons.Default.SystemUpdate
                },
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "PadelMatch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Versión instalada: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            when (val state = result) {
                null -> {
                    if (checking) {
                        CircularProgressIndicator()
                    }
                }

                is UpdateCheckResult.UpToDate -> {
                    StatusCard(
                        title = "Estás al día",
                        message = "Tienes la versión ${state.installedVersion}, que es la última Release disponible.",
                        positive = true
                    )
                }

                is UpdateCheckResult.Available -> {
                    UpdateAvailableCard(
                        release = state.release,
                        downloadUrl = repository.downloadUrlFor(state.release)
                    )
                }

                is UpdateCheckResult.Error -> {
                    StatusCard(
                        title = "No se pudo comprobar",
                        message = state.message,
                        positive = false
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { check() },
                enabled = !checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Comprobando…")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar actualización")
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Las actualizaciones se consultan desde las Releases públicas de GitHub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    positive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (positive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (positive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    release: AppRelease,
    downloadUrl: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Nueva versión disponible",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                release.name?.takeIf { it.isNotBlank() }
                    ?: release.tagName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            release.body?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(Modifier.height(16.dp))
                Text(
                    "Novedades",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Descargar actualización")
            }
        }
    }
}
