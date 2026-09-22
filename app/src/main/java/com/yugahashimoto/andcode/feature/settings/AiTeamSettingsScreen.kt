package com.yugahashimoto.andcode.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.core.mission.AgentRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTeamSettingsScreen(
    viewModel: AiTeamSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "AI Team Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            GlobalSettingsCard(
                settings = settings,
                onToggleParallel = viewModel::setEnableParallelAgents,
                onMaxConcurrentChange = viewModel::setMaxConcurrentAgents,
                onAutoRetryChange = viewModel::setAutoRetryFailedAgents,
                onFailFastChange = viewModel::setFailFastOnAgentFailure,
            )
        }

        items(AgentRole.entries.toList()) { role ->
            val entry = settings.getEntry(role)
            AgentCard(
                entry = entry,
                onToggleEnabled = { viewModel.toggleAgent(role, it) },
                onAutonomyChange = { viewModel.setAutonomyLevel(role, it) },
                onBudgetChange = { viewModel.setBudget(role, it) },
            )
        }
    }
}

@Composable
private fun GlobalSettingsCard(
    settings: AiTeamSettings,
    onToggleParallel: (Boolean) -> Unit,
    onMaxConcurrentChange: (Int) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onFailFastChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Global Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            SettingToggle("Parallel Agents", settings.enableParallelAgents, onToggleParallel)
            SettingToggle("Auto-retry Failed", settings.autoRetryFailedAgents, onAutoRetryChange)
            SettingToggle("Fail Fast", settings.failFastOnAgentFailure, onFailFastChange)

            Spacer(modifier = Modifier.height(8.dp))

            var sliderValue by remember { mutableFloatStateOf(settings.maxConcurrentAgents.toFloat()) }
            Text("Max Concurrent: ${sliderValue.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onMaxConcurrentChange(sliderValue.toInt()) },
                valueRange = 1f..6f,
                steps = 4,
                enabled = settings.enableParallelAgents,
            )
        }
    }
}

@Composable
private fun AgentCard(
    entry: AgentTeamEntry,
    onToggleEnabled: (Boolean) -> Unit,
    onAutonomyChange: (AutonomyLevel) -> Unit,
    onBudgetChange: (AgentBudgetConfig) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.enabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.role.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(entry.role.description, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = entry.enabled, onCheckedChange = onToggleEnabled)
            }

            if (entry.enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                AutonomyDropdown(entry.autonomyLevel, onAutonomyChange)

                Spacer(modifier = Modifier.height(8.dp))
                var budgetSlider by remember { mutableFloatStateOf(entry.budget.maxTokensPerTurn.toFloat() / 1000f) }
                Text("Max Tokens: ${(budgetSlider * 1000).toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = budgetSlider,
                    onValueChange = { budgetSlider = it },
                    onValueChangeFinished = {
                        onBudgetChange(entry.budget.copy(maxTokensPerTurn = (budgetSlider * 1000).toLong()))
                    },
                    valueRange = 1f..32f,
                    steps = 30,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutonomyDropdown(
    currentLevel: AutonomyLevel,
    onLevelChange: (AutonomyLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = currentLevel.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Autonomy Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AutonomyLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.displayName) },
                    onClick = {
                        onLevelChange(level)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
