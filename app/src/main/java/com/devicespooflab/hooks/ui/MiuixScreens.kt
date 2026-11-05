package com.devicespooflab.hooks.ui

import android.app.Activity
import android.content.Intent
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.AppProfileAssignmentActivity
import com.devicespooflab.hooks.DiagnosticsActivity
import com.devicespooflab.hooks.ProfileEditorActivity
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.RealInfoActivity
import com.devicespooflab.hooks.SafeModeAppsActivity
import com.devicespooflab.hooks.data.AppSettingsStore
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class MiuixHomeInfo(
    val label: String,
    val value: String,
)

private data class MiuixSettingOption(val id: String, val labelRes: Int)

private enum class MiuixAppFilter {
    All,
    Configured,
    Unassigned,
}

@Composable
fun MiuixCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = 20.dp,
        content = content,
    )
}

@Composable
fun MiuixSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = modifier.padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 8.dp),
    )
}

@Composable
fun MiuixListRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    bitmap: ImageBitmap? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    trailingText: String? = null,
    trailingTextMuted: Boolean = false,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    BasicComponent(
        title = title,
        summary = summary,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        startAction = when {
            bitmap != null -> ({
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            })
            icon != null -> ({
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            })
            else -> null
        },
        endActions = {
            when {
                checked != null && onCheckedChange != null -> {
                    Switch(checked = checked, onCheckedChange = onCheckedChange)
                }
                !trailingText.isNullOrBlank() -> {
                    Text(
                        text = trailingText,
                        style = MiuixTheme.textStyles.body2,
                        color = if (trailingTextMuted) {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantActions
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (onClick != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                onClick != null -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
        onClick = onClick,
    )
    if (showDivider) {
        HorizontalDivider(
            color = MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = if (icon != null || bitmap != null) 84.dp else 20.dp),
        )
    }
}

@Composable
private fun MiuixPill(text: String, emphasized: Boolean) {
    val background = if (emphasized) {
        MiuixTheme.colorScheme.tertiaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (emphasized) {
        MiuixTheme.colorScheme.onTertiaryContainer
    } else {
        MiuixTheme.colorScheme.onSurfaceContainerHigh
    }
    Box(
        modifier = Modifier
            .widthIn(max = 132.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun isMiuixDark(): Boolean {
    return MiuixTheme.colorSchemeMode == ColorSchemeMode.Dark ||
        MiuixTheme.colorSchemeMode == ColorSchemeMode.MonetDark
}

@Composable
fun MiuixMenuDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (show) {
        OverlayDialog(show = true, onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun MiuixOptionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    OverlayDialog(show = true, title = title, onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            options.forEach { (id, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = id == selectedId, onClick = { onSelect(id) })
                    Spacer(Modifier.width(12.dp))
                    Text(text = label, style = MiuixTheme.textStyles.main)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(text = stringResource(android.R.string.cancel), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun MiuixConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(show = true, title = title, summary = message, onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(text = stringResource(android.R.string.cancel), onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            TextButton(
                text = confirmText,
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(
                        color = Color.Transparent,
                        textColor = MiuixTheme.colorScheme.error,
                    )
                } else {
                    ButtonDefaults.textButtonColorsPrimary()
                },
            )
        }
    }
}

@Composable
private fun MiuixOptionLabel(options: List<MiuixSettingOption>, selectedId: String): String {
    return options.firstOrNull { it.id == selectedId }?.let { stringResource(it.labelRes) }.orEmpty()
}

@Composable
private fun MiuixPresetSourceDialog(currentUrl: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val defaultUrl = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL
    var custom by remember { mutableStateOf(currentUrl != defaultUrl) }
    var url by remember { mutableStateOf(if (custom) currentUrl else "") }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.settings_preset_source_title),
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { custom = false }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = !custom, onClick = { custom = false })
                Spacer(Modifier.width(12.dp))
                Text(text = stringResource(R.string.settings_preset_source_option_default), style = MiuixTheme.textStyles.main)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { custom = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = custom, onClick = { custom = true })
                Spacer(Modifier.width(12.dp))
                Text(text = stringResource(R.string.settings_preset_source_option_custom), style = MiuixTheme.textStyles.main)
            }
            if (custom) {
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = stringResource(R.string.settings_preset_source_input_hint),
                    singleLine = true,
                )
                Spacer(Modifier.height(MdSpacing.sm))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(text = stringResource(android.R.string.cancel), onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = { onSave(if (custom) url.trim() else defaultUrl) },
                )
            }
        }
    }
}
@Composable
fun MiuixHomeScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val config = appState.loadedConfig ?: return
    val profile = config.profile
    val moduleActivated = appState.isModuleActivated()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem(0) }
    val profileInfo = listOf(
        MiuixHomeInfo(
            stringResource(R.string.home_info_profile_model_title),
            profile.model,
        ),
        MiuixHomeInfo(
            stringResource(R.string.home_info_profile_preset_title),
            stringResource(
                R.string.home_info_profile_preset_value_format,
                appState.presetLabel(config.selectedPresetId),
                stringResource(if (config.isCustomMode) R.string.mode_custom else R.string.mode_preset),
            ),
        ),
        MiuixHomeInfo(
            stringResource(R.string.home_info_profile_android_title),
            stringResource(R.string.home_info_android_value_format, profile.buildRelease, profile.buildSdk),
        ),
        MiuixHomeInfo(
            stringResource(R.string.home_info_profile_display_title),
            stringResource(
                R.string.home_info_display_value_format,
                profile.screenWidth,
                profile.screenHeight,
                profile.screenDensity,
            ),
        ),
        MiuixHomeInfo(
            stringResource(R.string.home_config_file_title),
            stringResource(
                R.string.home_info_config_value_format,
                config.configFile.name,
                DateFormat.format("yyyy-MM-dd HH:mm", config.configFile.lastModified()),
            ),
        ),
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 16.dp),
    ) {
        item {
            MiuixModuleStatusCard(
                active = moduleActivated,
                profileName = profile.model,
            )
        }
        if (appState.defaultProfileId() == null && moduleActivated) {
            item { Spacer(Modifier.height(MdSpacing.xs)) }
            item {
                MiuixCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_no_default_profile_title),
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_no_default_profile_body),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.sm)) }
        item { MiuixSectionTitle(stringResource(R.string.home_active_profile_title)) }
        item {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    profileInfo.forEachIndexed { index, info ->
                        MiuixListRow(
                            title = info.label,
                            summary = info.value,
                            showDivider = index < profileInfo.lastIndex,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.xl)) }
    }
}

@Composable
private fun MiuixModuleStatusCard(active: Boolean, profileName: String) {
    val dark = isMiuixDark()
    val background = when {
        active && dark -> Color(0xFF173F2A)
        active -> MiuixTheme.colorScheme.primaryContainer
        dark -> Color(0xFF431F24)
        else -> MiuixTheme.colorScheme.errorContainer
    }
    val content = when {
        dark -> Color(0xFFF6F6F6)
        active -> MiuixTheme.colorScheme.onPrimaryContainer
        else -> MiuixTheme.colorScheme.onErrorContainer
    }
    val accent = when {
        active && dark -> Color(0xFF31D875)
        active -> MiuixTheme.colorScheme.onPrimaryContainer
        dark -> Color(0xFFFF6B70)
        else -> MiuixTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Text(
                text = stringResource(
                    if (active) R.string.home_module_active_title else R.string.home_module_inactive_title,
                ),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (active) R.string.home_module_active_body else R.string.home_module_inactive_body,
                ),
                style = MiuixTheme.textStyles.body1,
                color = content.copy(alpha = 0.78f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.home_active_profile_title),
                style = MiuixTheme.textStyles.footnote1,
                color = content.copy(alpha = 0.68f),
            )
            Text(
                text = profileName,
                style = MiuixTheme.textStyles.title4,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.72f),
            )
        }
        Icon(
            imageVector = if (active) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(116.dp)
                .offset(x = 24.dp, y = 18.dp),
        )
    }
}

@Composable
fun MiuixAppProfilesScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val assignmentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch { appState.reloadProfileState() }
    }

    LaunchedEffect(Unit) {
        appState.ensureInstalledAppsLoaded()
    }

    val visibleApps = remember(
        appState.installedApps,
        appState.hideSystemAppsInAppSettings,
        appState.profileState,
    ) {
        val apps = if (appState.hideSystemAppsInAppSettings) {
            appState.installedApps.filterNot(SafeAppEntry::systemApp)
        } else {
            appState.installedApps
        }
        apps.sortedWith(
            compareByDescending<SafeAppEntry> { appState.assignedProfileName(it.packageName) != null }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
        )
    }
    MiuixAppSelectionList(
        apps = visibleApps,
        loading = !appState.installedAppsLoaded,
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                appState.refreshInstalledApps()
                isRefreshing = false
            }
        },
        scaffoldPadding = scaffoldPadding,
        profileName = appState::assignedProfileName,
        onSelect = { app ->
            assignmentLauncher.launch(
                AppProfileAssignmentActivity.createIntent(context, app.packageName),
            )
        },
    )
}

@Composable
private fun MiuixAppSelectionList(
    apps: List<SafeAppEntry>,
    loading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    scaffoldPadding: PaddingValues,
    profileName: (String) -> String?,
    onSelect: (SafeAppEntry) -> Unit,
) {
    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_apps),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var selectedFilterName by rememberSaveable { mutableStateOf(MiuixAppFilter.All.name) }
    val selectedFilter = MiuixAppFilter.valueOf(selectedFilterName)
    val filteredApps = remember(apps, searchQuery, selectedFilterName) {
        val query = searchQuery.trim()
        apps.filter { app ->
            val assigned = profileName(app.packageName) != null
            val matchesFilter = when (selectedFilter) {
                MiuixAppFilter.All -> true
                MiuixAppFilter.Configured -> assigned
                MiuixAppFilter.Unassigned -> !assigned
            }
            val matchesQuery = query.isEmpty() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }
    val filters = listOf(
        MiuixAppFilter.All to stringResource(R.string.app_filter_all),
        MiuixAppFilter.Configured to stringResource(R.string.app_filter_configured),
        MiuixAppFilter.Unassigned to stringResource(R.string.app_filter_unassigned),
    )

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.app_profile_search),
                )
                Spacer(Modifier.height(12.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    filters.forEach { (filter, label) ->
                        Button(
                            onClick = { selectedFilterName = filter.name },
                            colors = if (selectedFilter == filter) {
                                ButtonDefaults.buttonColorsPrimary()
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            itemsIndexed(
                items = filteredApps,
                key = { _, app -> app.packageName },
            ) { _, app ->
                val assignedProfile = profileName(app.packageName)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(13.dp)),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = app.packageName,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        MiuixPill(
                            text = assignedProfile ?: stringResource(R.string.app_profile_unassigned),
                            emphasized = assignedProfile != null,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
fun MiuixProfileManagementScreen(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profiles = appState.profileState?.profiles.orEmpty()
    var profileIdPendingDeletion by remember { mutableStateOf<String?>(null) }
    var menuProfileId by remember { mutableStateOf<String?>(null) }
    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val profileId = result.data?.getStringExtra(ProfileEditorActivity.EXTRA_RESULT_PROFILE_ID)
        scope.launch { appState.reloadProfileState(profileId) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 16.dp),
    ) {
        item { MiuixSectionTitle(stringResource(R.string.profile_management_list_title)) }
        itemsIndexed(
            items = profiles,
            key = { _, profile -> profile.id },
        ) { _, profile ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editorLauncher.launch(ProfileEditorActivity.createEditIntent(context, profile.id))
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MiuixTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBox,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MiuixTheme.textStyles.title4,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.app_profile_assignment_count,
                                    appState.profileAssignmentCount(profile.id),
                                ),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            if (appState.defaultProfileId() == profile.id) {
                                MiuixPill(
                                    text = stringResource(R.string.profile_default_badge),
                                    emphasized = true,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { menuProfileId = profile.id }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            Button(
                onClick = { editorLauncher.launch(ProfileEditorActivity.createCreateIntent(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MdSpacing.sm),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(MdSpacing.xs))
                Text(stringResource(R.string.app_profile_create))
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }

    val menuProfile = profiles.firstOrNull { it.id == menuProfileId }
    if (menuProfile != null) {
        val isDefault = appState.defaultProfileId() == menuProfile.id
        MiuixMenuDialog(show = true, onDismiss = { menuProfileId = null }) {
            MiuixListRow(
                title = stringResource(
                    if (isDefault) R.string.profile_remove_default else R.string.profile_set_default,
                ),
                onClick = {
                    menuProfileId = null
                    scope.launch {
                        appState.setDefaultProfile(if (isDefault) null else menuProfile.id)
                    }
                },
            )
            if (profiles.size > 1) {
                MiuixListRow(
                    title = stringResource(R.string.profile_delete),
                    onClick = {
                        menuProfileId = null
                        profileIdPendingDeletion = menuProfile.id
                    },
                    showDivider = false,
                )
            }
        }
    }

    val profilePendingDeletion = profiles.firstOrNull { it.id == profileIdPendingDeletion }
    if (profilePendingDeletion != null) {
        MiuixConfirmDialog(
            title = stringResource(R.string.profile_delete_title),
            message = stringResource(
                R.string.profile_delete_message,
                profilePendingDeletion.name,
                appState.profileAssignmentCount(profilePendingDeletion.id),
            ),
            confirmText = stringResource(R.string.profile_delete),
            destructive = true,
            onConfirm = {
                profileIdPendingDeletion = null
                scope.launch { appState.deleteProfile(profilePendingDeletion.id) }
            },
            onDismiss = { profileIdPendingDeletion = null },
        )
    }
}

@Composable
fun MiuixAppSettingsScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val context = appState.activity
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var optionDialog by remember { mutableStateOf<String?>(null) }
    var presetSourceDialog by remember { mutableStateOf(false) }
    val safeModeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val packages = result.data
            ?.getStringArrayListExtra(SafeModeAppsActivity.EXTRA_RESULT_SELECTED_PACKAGES)
            ?.toSet()
            ?: emptySet()
        scope.launch { appState.updateSafeModePackages(packages) }
    }

    val themeOptions = listOf(
        MiuixSettingOption(AppSettingsStore.THEME_SYSTEM, R.string.theme_system),
        MiuixSettingOption(AppSettingsStore.THEME_LIGHT, R.string.theme_light),
        MiuixSettingOption(AppSettingsStore.THEME_DARK, R.string.theme_dark),
    )
    val languageOptions = listOf(
        MiuixSettingOption(AppSettingsStore.LANGUAGE_DEFAULT, R.string.language_default),
        MiuixSettingOption(AppSettingsStore.LANGUAGE_ENGLISH, R.string.language_english),
        MiuixSettingOption(AppSettingsStore.LANGUAGE_KOREAN, R.string.language_korean),
        MiuixSettingOption(AppSettingsStore.LANGUAGE_JAPANESE, R.string.language_japanese),
        MiuixSettingOption(AppSettingsStore.LANGUAGE_CHINESE_SIMPLIFIED, R.string.language_chinese),
    )
    val colorOptions = listOf(
        MiuixSettingOption(AppSettingsStore.COLOR_STYLE_MINT, R.string.color_style_mint),
        MiuixSettingOption(AppSettingsStore.COLOR_STYLE_BLUE, R.string.color_style_blue),
        MiuixSettingOption(AppSettingsStore.COLOR_STYLE_ROSE, R.string.color_style_rose),
        MiuixSettingOption(AppSettingsStore.COLOR_STYLE_AMBER, R.string.color_style_amber),
    )
    val uiStyleOptions = listOf(
        MiuixSettingOption(AppSettingsStore.UI_STYLE_MATERIAL, R.string.ui_style_material),
        MiuixSettingOption(AppSettingsStore.UI_STYLE_MIUIX, R.string.ui_style_miuix),
    )
    val systemColors = appState.systemColorsEnabled
    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 16.dp),
    ) {
        item { MiuixSectionTitle(stringResource(R.string.settings_appearance_title)) }
        item {
            MiuixCard {
                MiuixListRow(
                    title = stringResource(R.string.settings_theme_title),
                    summary = stringResource(R.string.settings_theme_helper),
                    trailingText = MiuixOptionLabel(themeOptions, appState.themeMode),
                    onClick = { optionDialog = "theme" },
                )
                MiuixListRow(
                    title = stringResource(R.string.settings_system_colors_title),
                    summary = stringResource(R.string.settings_system_colors_summary),
                    checked = systemColors,
                    onCheckedChange = { appState.updateSystemColors(it) },
                )
                if (!systemColors) {
                    MiuixListRow(
                        title = stringResource(R.string.settings_color_style_title),
                        summary = stringResource(
                            R.string.settings_color_style_summary,
                            MiuixOptionLabel(colorOptions, appState.colorStyle),
                        ),
                        trailingText = MiuixOptionLabel(colorOptions, appState.colorStyle),
                        onClick = { optionDialog = "color" },
                    )
                }
                MiuixListRow(
                    title = stringResource(R.string.settings_ui_style_title),
                    summary = stringResource(R.string.settings_ui_style_summary),
                    trailingText = MiuixOptionLabel(uiStyleOptions, appState.uiStyle),
                    onClick = { optionDialog = "ui" },
                    showDivider = false,
                )
            }
        }

        item { MiuixSectionTitle(stringResource(R.string.settings_language_title), Modifier.padding(top = 12.dp)) }
        item {
            MiuixCard {
                MiuixListRow(
                    title = stringResource(R.string.settings_language_title),
                    summary = stringResource(R.string.settings_language_helper),
                    trailingText = MiuixOptionLabel(languageOptions, appState.languageMode),
                    onClick = { optionDialog = "language" },
                    showDivider = false,
                )
            }
        }

        item { MiuixSectionTitle(stringResource(R.string.settings_runtime_title), Modifier.padding(top = 22.dp)) }
        item {
            MiuixCard {
                MiuixListRow(
                    title = stringResource(R.string.settings_resolution_title),
                    summary = stringResource(R.string.settings_resolution_summary),
                    checked = appState.isScreenMetricsEnabled(),
                    onCheckedChange = { enabled -> scope.launch { appState.updateScreenMetrics(enabled) } },
                )
                MiuixListRow(
                    title = stringResource(R.string.settings_safe_mode_title),
                    summary = if (appState.safeModePackages().isEmpty()) {
                        stringResource(R.string.settings_safe_mode_summary_disabled)
                    } else {
                        stringResource(R.string.settings_safe_mode_summary_count, appState.safeModePackages().size)
                    },
                    onClick = {
                        safeModeLauncher.launch(SafeModeAppsActivity.createIntent(context, appState.safeModePackages()))
                    },
                    showDivider = false,
                )
            }
        }

        item { MiuixSectionTitle(stringResource(R.string.settings_presets_title), Modifier.padding(top = 22.dp)) }
        item {
            MiuixCard {
                MiuixListRow(
                    title = stringResource(R.string.settings_preset_source_title),
                    summary = AppSettingsStore.getPresetSourceUrl(context),
                    onClick = { presetSourceDialog = true },
                    showDivider = false,
                )
            }
        }

        item { MiuixSectionTitle(stringResource(R.string.settings_tools_title), Modifier.padding(top = 22.dp)) }
        item {
            MiuixCard {
                MiuixListRow(
                    title = stringResource(R.string.settings_diagnostics_title),
                    summary = stringResource(R.string.real_info_button_helper),
                    onClick = { context.startActivity(Intent(context, DiagnosticsActivity::class.java)) },
                )
                MiuixListRow(
                    title = stringResource(R.string.settings_real_info_title),
                    summary = stringResource(R.string.settings_real_info_summary),
                    onClick = { context.startActivity(Intent(context, RealInfoActivity::class.java)) },
                    showDivider = false,
                )
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }

    when (optionDialog) {
        "theme" -> MiuixOptionDialog(
            title = stringResource(R.string.settings_choose_theme),
            options = themeOptions.map { it.id to stringResource(it.labelRes) },
            selectedId = appState.themeMode,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateThemeMode(it)
                optionDialog = null
            },
        )
        "language" -> MiuixOptionDialog(
            title = stringResource(R.string.settings_choose_language),
            options = languageOptions.map { it.id to stringResource(it.labelRes) },
            selectedId = appState.languageMode,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateLanguageMode(it)
                optionDialog = null
            },
        )
        "color" -> MiuixOptionDialog(
            title = stringResource(R.string.settings_choose_color_style),
            options = colorOptions.map { it.id to stringResource(it.labelRes) },
            selectedId = appState.colorStyle,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateColorStyle(it)
                optionDialog = null
            },
        )
        "ui" -> MiuixOptionDialog(
            title = stringResource(R.string.settings_choose_ui_style),
            options = uiStyleOptions.map { it.id to stringResource(it.labelRes) },
            selectedId = appState.uiStyle,
            onDismiss = { optionDialog = null },
            onSelect = {
                appState.updateUiStyle(it)
                optionDialog = null
            },
        )
    }

    if (presetSourceDialog) {
        MiuixPresetSourceDialog(
            currentUrl = AppSettingsStore.getPresetSourceUrl(context),
            onDismiss = { presetSourceDialog = false },
            onSave = {
                presetSourceDialog = false
                scope.launch { appState.updatePresetSource(it) }
            },
        )
    }
}
