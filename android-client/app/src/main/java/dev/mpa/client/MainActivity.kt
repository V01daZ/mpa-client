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

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onVpnPermissionResult(result.resultCode == RESULT_OK) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MpaTheme {
                val uiState      by viewModel.uiState.collectAsState()
                val installedApps by viewModel.installedApps.collectAsState()

                MainScreen(
                    uiState           = uiState,
                    installedApps     = installedApps,
                    onToggleConnection = { viewModel.toggleConnection(vpnPermissionLauncher) },
                    onSelectProfile   = viewModel::setActiveProfile,
                    onRemoveProfile   = viewModel::removeProfile,
                    onAddProfile      = viewModel::addProfileFromInput,
                    onDismissError    = viewModel::clearAddError,
                    onSplitTunnelChange = viewModel::updateSplitTunnel,
                    onOpenSplitTunnel = viewModel::loadInstalledApps,
                    onToggleGroup     = viewModel::toggleGroup,
                    onRenameGroup     = viewModel::renameGroup,
                    onRefreshGroup    = viewModel::forceRefreshGroup,
                    onUpdateAction    = viewModel::onUpdateAction,
                    onDismissUpdate   = viewModel::dismissUpdate,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
        viewModel.refreshAllPings()
        // Проверяем обновления при каждом открытии приложения
        viewModel.checkForUpdate()
        // Скачиваем/обновляем RU rule-set для умного роутинга
        viewModel.downloadRuleSets()
    }

    override fun onStop() {
        viewModel.unbindService()
        super.onStop()
    }
}

