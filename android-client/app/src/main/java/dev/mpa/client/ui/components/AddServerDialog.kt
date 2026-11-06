package dev.mpa.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.mpa.client.ui.theme.Board
import dev.mpa.client.ui.theme.Fail
import dev.mpa.client.ui.theme.MonoData
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.Paper
import dev.mpa.client.ui.theme.PaperInk
import dev.mpa.client.ui.theme.Rule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    isLoading: Boolean,
    error: String?,
    initialInput: String = "",
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit,
    onScanQr: () -> Unit,
) {
    var input by remember(initialInput) { mutableStateOf(initialInput) }
    var groupName by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Paper,
        unfocusedBorderColor = Rule,
        focusedLabelColor = Paper,
        cursorColor = Paper,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Board,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Text("Добавить сервер", color = OnBoard, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ключ от бота, ссылка vless:// или ссылка на подписку.",
                color = OnBoardMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Ключ или ссылка") },
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let { input = it.trim() } }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Вставить из буфера")
                    }
                },
                isError = error != null,
                supportingText = error?.let { { Text(it, color = Fail) } },
                textStyle = MonoData,
                minLines = 2,
                maxLines = 5,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Название группы (необязательно)") },
                placeholder = { Text("Напр. Личная подписка") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onScanQr, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OnBoard, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("QR-код", color = OnBoard)
                }
                Button(
                    onClick = { onAdd(input.trim(), groupName.trim().takeIf { it.isNotEmpty() }) },
                    enabled = !isLoading && input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = PaperInk),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PaperInk, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Проверяю…")
                    } else {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}
