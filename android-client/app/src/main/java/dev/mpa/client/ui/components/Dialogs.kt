package dev.mpa.client.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mpa.client.ui.theme.Board
import dev.mpa.client.ui.theme.Fail
import dev.mpa.client.ui.theme.OnBoard

@Composable
fun ConfirmDeleteDialog(profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Board,
        title = { Text("Удалить сервер?") },
        text = { Text("«$profileName» пропадёт из списка. Если он из подписки, вернётся при следующем обновлении.") },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = Fail)) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = OnBoard)) { Text("Отмена") }
        },
    )
}

@Composable
fun RenameGroupDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Board,
        title = { Text("Название подписки") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = OnBoard),
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = OnBoard)) { Text("Отмена") }
        },
    )
}
