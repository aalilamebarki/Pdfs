package com.ali.docscanner.presentation.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.domain.model.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onDocumentClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentDocuments by viewModel.recentDocuments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.scan),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.recent_documents),
                    style = MaterialTheme.typography.titleMedium
                )
                if (recentDocuments.isNotEmpty()) {
                    TextButton(onClick = onSeeAllClick) {
                        Text(stringResource(R.string.see_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recentDocuments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_documents_yet),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentDocuments.forEach { document ->
                        RecentDocumentRow(
                            document = document,
                            onClick = { onDocumentClick(document.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDocumentRow(document: Document, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecentThumbnail(path = document.thumbnailPath)

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = document.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        R.string.document_meta_label,
                        document.pageCount,
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(document.updatedAt))
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecentThumbnail(path: String?) {
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
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
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
