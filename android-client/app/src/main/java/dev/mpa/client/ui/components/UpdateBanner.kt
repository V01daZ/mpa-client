package dev.mpa.client.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.ReleaseInfo
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.AccentSoft
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Connected
import dev.mpa.client.ui.theme.Error
import dev.mpa.client.ui.theme.Ink
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

@Composable
fun UpdateBanner(
    release: ReleaseInfo,
    downloadState: DownloadState,
    onUpdate: () -> Unit,    // начать загрузку ИЛИ установить если уже скачано
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit  = shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentSoft)
                .border(
                    width = 1.dp,
                    color = Accent.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Доступно обновление v${release.versionName}",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    if (release.releaseNotes.isNotBlank()) {
                        Text(
                            text = release.releaseNotes.lines().first().take(60),
                            color = TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                // Кнопка действия
                when (downloadState) {
                    is DownloadState.Idle -> {
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Ink,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 6.dp
                            ),
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Скачать",
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    is DownloadState.Downloading -> {
                        // Прогресс вместо кнопки
                        CircularProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            color = Accent,
                            trackColor = Border,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                        )
                    }

                    is DownloadState.Ready -> {
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Connected,
                                contentColor = Ink,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 6.dp
                            ),
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Установить",
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    is DownloadState.Failed -> {
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Error,
                                contentColor = Ink,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 6.dp
                            ),
                        ) {
                            Text(
                                "Повторить",
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // Скрыть (только если не идёт загрузка)
                if (downloadState !is DownloadState.Downloading) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Скрыть",
                            tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Полоска прогресса под строкой
            if (downloadState is DownloadState.Downloading) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    color = Accent,
                    trackColor = Border,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = "Загрузка... ${downloadState.progress}%",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
