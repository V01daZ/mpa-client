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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Error
import dev.mpa.client.ui.theme.Ink
import dev.mpa.client.ui.theme.JetBrainsMonoFamily
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.Surface
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

@Composable
fun AddServerDialog(
    isLoading: Boolean,
    error: String?,
    initialInput: String = "",
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onScanQr: () -> Unit,
) {
    var input by remember(initialInput) { mutableStateOf(initialInput) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Добавить сервер",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                // Кнопка QR
                IconButton(onClick = onScanQr) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Сканировать QR",
                        tint = Accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "ССЫЛКА ИЛИ КЛЮЧ",
                color = TextMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.1.sp,
            )
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = {
                    Text(
                        "vless://... / ссылка на подписку / ключ активации",
                        color = TextMuted,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedContainerColor = Ink,
                    unfocusedContainerColor = Ink,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = TextPrimary,
                ),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error, color = Error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onAdd(input.trim()) },
                enabled = !isLoading && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink,
                    disabledContainerColor = Accent.copy(alpha = 0.4f),
                    disabledContentColor = Ink.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isLoading) "Добавление..." else "Добавить",
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Вставь ссылку vless://, ссылку на подписку или ключ активации. " +
                       "Или нажми иконку QR-кода чтобы сканировать.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
