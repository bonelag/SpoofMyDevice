package com.devicespooflab.hooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.data.DevicePreset
import top.yukonga.miuix.kmp.basic.ScrollBarColors
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/** A bounded viewport keeps the search field visible while the results scroll. */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun PresetPickerContent(
    presets: List<DevicePreset>,
    scrollbarColor: Color,
    modifier: Modifier = Modifier,
    searchField: @Composable (String, (String) -> Unit) -> Unit,
    emptyContent: @Composable () -> Unit,
    presetContent: @Composable (DevicePreset, Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchIndex = remember(presets) {
        presets.map { preset ->
            val profile = preset.profile
            preset to listOf(preset.displayName, preset.summary, profile.model, profile.socModel)
                .joinToString(" ")
        }
    }
    val results = remember(searchIndex, query) {
        val term = query.trim()
        searchIndex.filter { (_, text) -> text.contains(term, ignoreCase = true) }
            .map { (preset, _) -> preset }
    }
    val listState = rememberLazyListState()
    val showScrollbar by remember {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }

    LaunchedEffect(query) {
        listState.scrollToItem(0)
    }

    Column(modifier.fillMaxWidth().heightIn(max = 420.dp)) {
        searchField(query) { query = it }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Keep row actions outside the scrollbar's 48 dp touch strip.
                contentPadding = PaddingValues(end = if (showScrollbar) 48.dp else 0.dp),
            ) {
                if (results.isEmpty()) {
                    item { emptyContent() }
                } else {
                    itemsIndexed(results, key = { _, preset -> preset.id }) { index, preset ->
                        presetContent(preset, index < results.lastIndex)
                    }
                }
            }
            if (showScrollbar) {
                // This overlay does not contribute to measuring the viewport.
                Box(Modifier.matchParentSize()) {
                    VerticalScrollBar(
                        adapter = rememberScrollBarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        colors = ScrollBarColors(
                            thumbColor = scrollbarColor,
                            trackColor = scrollbarColor.copy(alpha = 0.12f),
                        ),
                        thumbWidth = 4.dp,
                        endPadding = 4.dp,
                    )
                }
            }
        }
    }
}
