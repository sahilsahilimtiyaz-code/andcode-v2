package com.yugahashimoto.andcode.feature.premium

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.feature.premium.engines.ConflictHunk
import com.yugahashimoto.andcode.feature.premium.engines.ConflictSide
import com.yugahashimoto.andcode.feature.premium.engines.ConflictResolverState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedGitScreen(
    conflictState: ConflictResolverState,
    onResolve: (String, ConflictSide) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Advanced Git", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Merge Conflicts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${conflictState.getResolvedCount()} / ${conflictState.getTotalCount()} resolved", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            items(conflictState.conflicts) { conflict ->
                ConflictCard(
                    conflict = conflict,
                    onResolve = onResolve
                )
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: ConflictHunk,
    onResolve: (String, ConflictSide) -> Unit
) {
    GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(conflict.filePath, color = Color(0xFF00D4FF), fontWeight = FontWeight.Bold)
                Text("Line ${conflict.startLine}", color = Color.White.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (conflict.resolution != null) {
                Text("Resolved: ${conflict.resolution}", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
            } else {
                Text("Ours:", color = Color(0xFF00D4FF), style = MaterialTheme.typography.labelSmall)
                Text(
                    conflict.oursContent.take(100),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Theirs:", color = Color(0xFFB84CFF), style = MaterialTheme.typography.labelSmall)
                Text(
                    conflict.theirsContent.take(100),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResolve(conflict.id, ConflictSide.OURS) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF).copy(alpha = 0.3f))
                    ) {
                        Text("Take Ours", color = Color.White)
                    }
                    Button(
                        onClick = { onResolve(conflict.id, ConflictSide.THEIRS) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB84CFF).copy(alpha = 0.3f))
                    ) {
                        Text("Take Theirs", color = Color.White)
                    }
                    Button(
                        onClick = { onResolve(conflict.id, ConflictSide.UNION) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E).copy(alpha = 0.3f))
                    ) {
                        Text("Union", color = Color.White)
                    }
                }
            }
        }
    }
}
