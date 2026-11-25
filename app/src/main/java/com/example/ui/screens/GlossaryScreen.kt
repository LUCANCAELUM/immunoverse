package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.ui.components.GlossaryContentView
import com.example.ui.components.ModuleHeader

@Composable
fun GlossaryScreen(
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Immunology Glossary",
            subtitle = "Searchable core definitions, mechanisms & 8 interactive schematic diagrams.",
            badge = "18 Concepts"
        )

        GlossaryContentView(
            bookmarks = bookmarks,
            onToggleBookmark = onToggleBookmark
        )

        Spacer(Modifier.height(24.dp))
    }
}
