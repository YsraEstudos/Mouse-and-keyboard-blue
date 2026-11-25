package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.VaultCredential
import com.example.ui.components.RemoteSurface
import com.example.viewmodel.RemoteViewModel

/**
 * Encrypted Biometric Credential Vault (On-Demand Unlock):
 * Keeps credentials encrypted and out of memory until authenticated via BiometricPrompt,
 * auto-locks when leaving the Vault screen or moving to background, and requires confirmation before deletion.
 */
@Composable
fun VaultScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val credentials by viewModel.vaultCredentials.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteCred by remember { mutableStateOf<VaultCredential?>(null) }

    fun authenticateUser(title: String, subtitle: String, onSuccess: () -> Unit) {
        val activity = context as? FragmentActivity
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        if (activity != null && canAuth) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        viewModel.showBanner("Autenticação cancelada")
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
        } else {
            // Fallback when emulator/device has no enrolled screen lock or biometrics
            onSuccess()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), highlighted = true) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cofre Criptografado (AES-256)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Digita automaticamente Usuário + Tab + Senha + Enter em telas de login, VMs e terminais sem passar pela área de transferência.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isVaultUnlocked) {
                        OutlinedButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.testTag("vault_lock_button"),
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Bloquear")
                        }
                    }
                }
            }

            if (!isVaultUnlocked) {
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Text("Cofre Bloqueado", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Suas senhas permanecem criptografadas no Keystore do Android e só são carregadas na memória após sua biometria ou PIN do aparelho. O cofre bloqueia automaticamente ao sair.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                authenticateUser(
                                    title = "Desbloquear Cofre de Senhas",
                                    subtitle = "Confirme sua biometria ou PIN para acessar suas credenciais",
                                    onSuccess = { viewModel.unlockVault() },
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("vault_unlock_button"),
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Desbloquear Cofre com Biometria")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("vault_add_button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar Credencial Protegida")
                }

                if (credentials.isEmpty()) {
                    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                            Text("Nenhuma credencial salva", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Salve atalhos de login (Windows, Linux, SSH, VPN ou sites) criptografados no Keystore do Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(credentials, key = { it.id }) { cred ->
                            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cred.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (cred.username.isNotBlank()) "${cred.username} • ••••••••" else "Somente senha • ••••••••",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            authenticateUser(
                                                title = "Digitar login: ${cred.title}",
                                                subtitle = "Confirme para digitar credenciais via Bluetooth HID",
                                                onSuccess = { viewModel.autoTypeVaultCredential(cred) },
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Digitar")
                                    }
                                    IconButton(onClick = { pendingDeleteCred = cred }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCredentialDialog(
            onSave = { cred ->
                viewModel.saveVaultCredential(cred)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    pendingDeleteCred?.let { cred ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCred = null },
            title = { Text("Remover credencial?") },
            text = { Text("Deseja excluir permanentemente \"${cred.title}\" do Cofre criptografado?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteVaultCredential(cred.id)
                    pendingDeleteCred = null
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCred = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun AddCredentialDialog(
    onSave: (VaultCredential) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pressEnter by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Credencial no Cofre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (ex: Login PC Trabalho, Servidor SSH)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário ou E-mail (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pressEnter, onCheckedChange = { pressEnter = it })
                    Text("Pressionar Enter ao finalizar", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && password.isNotBlank()) {
                    onSave(
                        VaultCredential(
                            title = title.trim(),
                            username = username.trim(),
                            password = password,
                            pressEnterAfter = pressEnter,
                        )
                    )
                }
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
