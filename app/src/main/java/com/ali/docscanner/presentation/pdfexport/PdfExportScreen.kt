package com.ali.docscanner.presentation.pdfexport

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.util.PdfQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportScreen(
    onDone: () -> Unit,
    viewModel: PdfExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var nameField by remember { mutableStateOf<String?>(null) }
    var selectedQuality by remember { mutableStateOf(PdfQuality.MEDIUM) }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is PdfExportUiState.Ready && nameField == null) {
            nameField = state.document.name
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.export_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            when (val state = uiState) {
                is PdfExportUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PdfExportUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }

                else -> {
                    OutlinedTextField(
                        value = nameField ?: "",
                        onValueChange = { nameField = it },
                        label = { Text(stringResource(R.string.document_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = stringResource(R.string.pdf_quality), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityChip(
                            label = stringResource(R.string.quality_low),
                            selected = selectedQuality == PdfQuality.LOW,
                            onClick = { selectedQuality = PdfQuality.LOW }
                        )
                        QualityChip(
                            label = stringResource(R.string.quality_medium),
                            selected = selectedQuality == PdfQuality.MEDIUM,
                            onClick = { selectedQuality = PdfQuality.MEDIUM }
                        )
                        QualityChip(
                            label = stringResource(R.string.quality_high),
                            selected = selectedQuality == PdfQuality.HIGH,
                            onClick = { selectedQuality = PdfQuality.HIGH }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (state) {
                        is PdfExportUiState.Generating -> {
                            CircularProgressIndicator()
                        }
                        is PdfExportUiState.Generated -> {
                            Text(
                                text = stringResource(R.string.pdf_saved_at, state.pdfPath),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, state.pdfUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.share_pdf))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(stringResource(R.string.share_pdf))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDone,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(stringResource(R.string.done))
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    nameField?.let { viewModel.renameDocument(it) }
                                    viewModel.generateAndPreparePdf(selectedQuality)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(stringResource(R.string.generate_pdf))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
