package com.ali.docscanner.presentation.documentpreview

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ali.docscanner.domain.model.Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    onAddPage: (Long) -> Unit,
    onExport: (Long) -> Unit,
    onDocumentDeleted: () -> Unit,
    viewModel: DocumentPreviewViewModel = hiltViewModel()
) {
    val pages by viewModel.pages.collectAsState()
    val event by viewModel.events.collectAsState()

    LaunchedEffect(event) {
        if (event is DocumentPreviewEvent.DocumentDeleted) {
            viewModel.consumeEvent()
            onDocumentDeleted()
        }
    }

    val sortedPages = pages.sortedBy { it.pageOrder }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.document_preview_title)) })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedPages, key = { it.id }) { page ->
                    PageRow(
                        page = page,
                        pageNumber = sortedPages.indexOf(page) + 1,
                        canMoveUp = sortedPages.first() != page,
                        canMoveDown = sortedPages.last() != page,
                        onMoveUp = { viewModel.moveUp(page) },
                        onMoveDown = { viewModel.moveDown(page) },
                        onDelete = { viewModel.deletePage(page) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { onAddPage(viewModel.documentId) }) {
                    Text(stringResource(R.string.add_page))
                }
                Button(
                    onClick = { onExport(viewModel.documentId) },
                    enabled = sortedPages.isNotEmpty()
                ) {
                    Text(stringResource(R.string.continue_to_export))
                }
            }
        }
    }
}

@Composable
private fun PageRow(
    page: Page,
    pageNumber: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        val moveUpLabel = stringResource(R.string.move_page_up)
        val moveDownLabel = stringResource(R.string.move_page_down)
        val deleteLabel = stringResource(R.string.delete_page)

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageThumbnail(path = page.thumbnailPath ?: page.imagePath)

            Text(
                text = stringResource(R.string.page_number_label, pageNumber),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.semantics { contentDescription = moveUpLabel }
            ) {
                Text(text = "\u2191", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.semantics { contentDescription = moveDownLabel }
            ) {
                Text(text = "\u2193", style = MaterialTheme.typography.titleLarge)
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
private fun PageThumbnail(path: String) {
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
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
