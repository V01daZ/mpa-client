package dev.mpa.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import dev.mpa.client.ui.MainScreen
import dev.mpa.client.ui.theme.MpaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Лаунчер разрешения VPN (аналог диалога UAC на десктопе)
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnPermissionResult(result.resultCode == RESULT_OK)
    }

    // Лаунчер разрешения уведомлений (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* просто принимаем результат */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запрашиваем разрешение на уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MpaTheme {
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    uiState = uiState,
                    onToggleConnection = {
                        viewModel.toggleConnection(vpnPermissionLauncher)
                    },
                    onSelectProfile = viewModel::setActiveProfile,
                    onRemoveProfile = viewModel::removeProfile,
                    onAddProfile = { input ->
                        viewModel.addProfileFromInput(input)
                    },
                    onDismissError = viewModel::clearAddError,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
        viewModel.refreshAllPings()
    }

    override fun onStop() {
        viewModel.unbindService()
        super.onStop()
    }
}
