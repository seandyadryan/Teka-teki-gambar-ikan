package com.deploydulupulangnanti.tekatekigambarikan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

@Composable
internal fun LanguagePicker(selected: String, choose: (String) -> Unit, dismiss: () -> Unit) {
    val system = AppLanguage("", stringResource(R.string.language_system))
    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = InkLift,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp).selectableGroup()) {
                items(listOf(system) + appLanguages, key = AppLanguage::tag) { language ->
                    val checked = selected == language.tag
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(checked, role = Role.RadioButton) { choose(language.tag) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = checked, onClick = null)
                        Text(
                            language.name,
                            Modifier.padding(start = 12.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.Content,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
