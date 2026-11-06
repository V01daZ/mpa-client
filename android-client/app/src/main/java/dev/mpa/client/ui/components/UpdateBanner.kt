package dev.mpa.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.ReleaseInfo
import dev.mpa.client.data.RuleSetState
import dev.mpa.client.ui.theme.Board
import dev.mpa.client.ui.theme.Fail
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.PaperInk
import dev.mpa.client.ui.theme.Rule
import dev.mpa.client.ui.theme.Signal

/** Объявление на табло: доступна новая версия. */
@Composable
fun UpdateBanner(
    release: ReleaseInfo,
    downloadState: DownloadState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Board)
            .border(1.dp, Signal.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Новая версия ${release.versionName}", color = OnBoard, style = MaterialTheme.typography.titleSmall)
                val sub = when (downloadState) {
                    is DownloadState.Downloading -> "Загрузка… ${downloadState.progress}%"
                    is DownloadState.Ready -> "Скачано, можно ставить"
                    is DownloadState.Failed -> "Не скачалось. Проверь сеть и повтори"
                    else -> release.releaseNotes.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                }
                if (sub.isNotEmpty()) {
                    Text(
                        sub,
                        color = if (downloadState is DownloadState.Failed) Fail else OnBoardMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when (downloadState) {
                is DownloadState.Downloading -> CircularProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    color = Signal, trackColor = Rule, strokeWidth = 3.dp,
                    modifier = Modifier.padding(horizontal = 12.dp).size(24.dp),
                )
                else -> Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = PaperInk),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(
                        when (downloadState) {
                            is DownloadState.Ready -> "Установить"
                            is DownloadState.Failed -> "Повторить"
                            else -> "Скачать"
                        }
                    )
                }
            }
            if (downloadState !is DownloadState.Downloading) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Скрыть", tint = OnBoardMuted)
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
        if (downloadState is DownloadState.Downloading) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { downloadState.progress / 100f },
                color = Signal, trackColor = Rule,
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            )
        }
    }
}

/** Строка состояния RU-правил под шапкой. Видна только пока качаются или при ошибке. */
@Composable
fun RuleSetStrip(state: RuleSetState, modifier: Modifier = Modifier) {
    val text = when (state) {
        is RuleSetState.Checking, is RuleSetState.Downloading -> "Обновляю правила для RU-сайтов…"
        is RuleSetState.Error -> "Правила для RU-сайтов не обновились: ${state.message}"
        else -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        if (state !is RuleSetState.Error) {
            CircularProgressIndicator(color = Signal, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (state is RuleSetState.Error) Signal else OnBoardMuted, style = MaterialTheme.typography.bodySmall)
    }
}
