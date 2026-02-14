package com.xz.vibenote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class LanguageOption(val label: String, val tag: String)

private val languageOptions = listOf(
    LanguageOption("System Default", ""),
    LanguageOption("English", "en-US"),
    LanguageOption("中文 (简体)", "zh-CN"),
)

@Composable
fun SettingsScreen(viewModel: NoteViewModel) {
    val selectedLanguage by viewModel.speechLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Speech Recognition Language",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        languageOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSpeechLanguage(option.tag) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLanguage == option.tag,
                    onClick = { viewModel.setSpeechLanguage(option.tag) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
