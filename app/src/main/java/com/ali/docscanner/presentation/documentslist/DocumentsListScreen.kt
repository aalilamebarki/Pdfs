package com.ali.docscanner.presentation.documentslist

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.domain.model.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsListScreen(
    onDocumentClick: (Long) -> Unit,
    viewModel: DocumentsListViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.documents_list_title)) }) }
    ) { padding ->
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_documents_yet),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(documents, key = { it.id }) { document ->
                    DocumentRow(
                        document = document,
                        onClick = { onDocumentClick(document.id) },
                        onDelete = { viewModel.deleteDocument(document) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(document: Document, onClick: () -> Unit, onDelete: () -> Unit) {
    val deleteLabel = stringResource(R.string.delete_document)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentThumbnail(path = document.thumbnailPath)

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = document.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.document_meta_label,
                        document.pageCount,
                        formatDate(document.updatedAt)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { contentDescription = deleteLabel }
            ) {
                Text(text = "\u2715", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DocumentThumbnail(path: String?) {
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = if (path != null) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun formatDate(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}
