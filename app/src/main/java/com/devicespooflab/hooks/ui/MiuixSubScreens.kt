package com.devicespooflab.hooks.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.RealInfoActivity
import com.devicespooflab.hooks.data.AppProfileStore
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.data.DevicePreset
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.TimeZone

private const val MIUIX_GITHUB_URL = "https://github.com/BuSung-dev/SpoofMyDevice"

@Composable
private fun MiuixSubScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                largeTitle = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = actions,
            )
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
fun MiuixProfileEditorLoadingScreen(
    creating: Boolean,
    onBack: () -> Unit,
) {
    MiuixEditorScaffold(
        title = stringResource(if (creating) R.string.profile_create_title else R.string.profile_edit_title),
        saveEnabled = false,
        snackbarHostState = remember { SnackbarHostState() },
        onSave = {},
        onBack = onBack,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.profile_editor_loading),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
fun MiuixProfileEditorScreen(
    appState: SpoofAppState,
    creating: Boolean,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = appState.message
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            appState.consumeMessage()
        }
    }

    MiuixEditorScaffold(
        title = stringResource(if (creating) R.string.profile_create_title else R.string.profile_edit_title),
        saveEnabled = profileName.isNotBlank() && !appState.loading,
        snackbarHostState = snackbarHostState,
        onSave = onSave,
        onBack = onBack,
    ) { contentPadding ->
        MiuixEditorForm(
            appState = appState,
            scaffoldPadding = contentPadding,
            profileName = profileName,
            onProfileNameChange = onProfileNameChange,
        )
    }
}

@Composable
private fun MiuixEditorScaffold(
    title: String,
    saveEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onSave: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    MiuixSubScaffold(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(onClick = onSave, enabled = saveEnabled) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.save_profile),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}

@Composable
fun MiuixEditorForm(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
) {
    val editor = appState.editor
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(1) }

    val tabs = listOf(
        stringResource(R.string.tab_soc),
        stringResource(R.string.tab_system),
        stringResource(R.string.tab_screen),
        stringResource(R.string.tab_network),
        stringResource(R.string.tab_identifiers),
    )
    val textMeasurer = rememberTextMeasurer()
    val tabStyle = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold)
    val tabLayouts = tabs.map { textMeasurer.measure(it, style = tabStyle, maxLines = 1) }
    val density = LocalDensity.current
    val tabWidth = (with(density) { tabLayouts.maxOf { it.size.width }.toDp() } + 32.dp)
        .coerceAtLeast(100.dp)
    val tabHeight = (with(density) { tabLayouts.maxOf { it.size.height }.toDp() } + 20.dp)
        .coerceAtLeast(48.dp)
    val fields = when (selectedTabIndex) {
        0 -> socFields
        1 -> systemFields
        2 -> screenFields
        3 -> networkFields
        else -> advancedFields
    }
    val profileNameLabel = stringResource(R.string.app_profile_name)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .consumeWindowInsets(scaffoldPadding)
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "profile_name") {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = profileNameLabel, style = MiuixTheme.textStyles.body1)
                    TextField(
                        value = profileName,
                        onValueChange = onProfileNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = profileNameLabel },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    MiuixPresetSelector(
                        appState = appState,
                        onPresetSelected = { preset ->
                            if (profileName.isBlank()) {
                                onProfileNameChange(preset.displayName)
                            }
                        },
                    )
                }
            }
        }
        item(key = "tabs") {
            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                minWidth = tabWidth,
                maxWidth = tabWidth,
                height = tabHeight,
            )
        }
        if (selectedTabIndex == 4) {
            item(key = "identifier_actions") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            editor.randomizeAll()
                            editor.customMode = true
                        },
                    ) {
                        Icon(Icons.Filled.Casino, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_generate_all))
                    }
                    Button(
                        onClick = {
                            editor.clearAdvanced()
                            editor.customMode = true
                        },
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_all))
                    }
                }
            }
        }
        items(fields, key = { it.fieldId }) { field ->
            if (selectedTabIndex == 4) {
                MiuixAdvancedEditorField(field = field, editor = editor)
            } else {
                MiuixEditorField(field = field, editor = editor)
            }
        }
    }
}

@Composable
private fun MiuixPresetSelector(
    appState: SpoofAppState,
    onPresetSelected: ((DevicePreset) -> Unit)? = null,
) {
    val editor = appState.editor
    var expanded by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val selectedLabel = appState.presets.firstOrNull { it.id == editor.selectedPresetId }?.displayName.orEmpty()

    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        MiuixListRow(
            title = stringResource(R.string.settings_hint_preset),
            summary = selectedLabel,
            onClick = { expanded = true },
            showDivider = false,
        )
    }

    if (expanded) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.settings_hint_preset),
            onDismissRequest = { expanded = false },
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                PresetPickerContent(
                    presets = appState.presets,
                    scrollbarColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f, fill = false),
                    searchField = { query, onQueryChange ->
                        TextField(
                            value = query,
                            onValueChange = onQueryChange,
                            label = stringResource(R.string.preset_search_hint),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    emptyContent = {
                        Text(
                            text = stringResource(R.string.preset_search_empty),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(12.dp),
                        )
                    },
                ) { preset, showDivider ->
                    MiuixListRow(
                        title = preset.displayName,
                        summary = preset.summary.takeIf(String::isNotBlank),
                        onClick = {
                            editor.selectPreset(preset)
                            onPresetSelected?.invoke(preset)
                            keyboard?.hide()
                            expanded = false
                        },
                        showDivider = showDivider,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixEditorField(
    field: EditorField,
    editor: ProfileEditorState,
    modifier: Modifier = Modifier,
) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    MiuixEditorFieldCard(
        field = field,
        value = editor.values[field.fieldId].orEmpty(),
        spoofEnabled = spoofEnabled,
        onSpoofToggle = { editor.enabledFields[field.fieldId] = !spoofEnabled },
        onValueChange = {
            editor.values[field.fieldId] = it
            editor.customMode = true
        },
        modifier = modifier,
    )
}

@Composable
private fun MiuixAdvancedEditorField(
    field: EditorField,
    editor: ProfileEditorState,
    modifier: Modifier = Modifier,
) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    MiuixEditorFieldCard(
        field = field,
        value = editor.advancedValues[field.fieldId].orEmpty(),
        spoofEnabled = spoofEnabled,
        onSpoofToggle = { editor.enabledFields[field.fieldId] = !spoofEnabled },
        onValueChange = {
            editor.advancedValues[field.fieldId] = it
            editor.customMode = true
        },
        onRandomize = {
            editor.randomize(field.fieldId)
            editor.customMode = true
        },
        modifier = modifier,
    )
}

@Composable
private fun MiuixEditorFieldCard(
    field: EditorField,
    value: String,
    spoofEnabled: Boolean,
    onSpoofToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRandomize: (() -> Unit)? = null,
) {
    val label = stringResource(field.labelRes)
    MiuixCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (field.toggleable) Checkbox(
                    state = if (spoofEnabled) ToggleableState.On else ToggleableState.Off,
                    onClick = onSpoofToggle,
                    modifier = Modifier.semantics { contentDescription = label },
                )
                if (onRandomize != null) {
                    IconButton(onClick = onRandomize, enabled = spoofEnabled) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = stringResource(R.string.settings_generate_random),
                        )
                    }
                }
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                enabled = spoofEnabled,
                singleLine = false,
                minLines = if (field.multiline) 2 else 1,
                maxLines = Int.MAX_VALUE,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
            )
        }
    }
}
@Composable
fun MiuixSafeModeScreen(
    appState: SpoofAppState,
    initialSelectedPackages: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val apps = remember { mutableStateListOf<SafeAppEntry>() }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelectedPackages) } }
    var loading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hideSystemApps by remember { mutableStateOf(true) }
    var showOptions by remember { mutableStateOf(false) }
    var showExplanation by remember {
        mutableStateOf(!AppSettingsStore.isSafeModeExplanationDismissed(appState.activity))
    }
    var doNotShowAgain by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun toggle(packageName: String) {
        if (selected.contains(packageName)) {
            selected.remove(packageName)
        } else {
            selected.add(packageName)
        }
        onSelectionChanged(selected.toSet())
    }

    fun dismissExplanation() {
        if (doNotShowAgain) {
            AppSettingsStore.setSafeModeExplanationDismissed(appState.activity, true)
        }
        showExplanation = false
    }

    LaunchedEffect(Unit) {
        val selectedAtEntry = initialSelectedPackages
        apps.addAll(
            appState.loadInstalledApps().sortedWith(
                compareBy<SafeAppEntry> { !selectedAtEntry.contains(it.packageName) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
        )
        loading = false
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_safe_mode_title),
                largeTitle = stringResource(R.string.settings_safe_mode_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.safe_mode_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.safe_mode_more_options),
                        )
                    }
                    MiuixMenuDialog(show = showOptions, onDismiss = { showOptions = false }) {
                        MiuixListRow(
                            title = stringResource(R.string.safe_mode_hide_system_apps),
                            checked = hideSystemApps,
                            onCheckedChange = {
                                hideSystemApps = it
                                showOptions = false
                            },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
            return@Scaffold
        }

        val visibleApps = apps.filter {
            !hideSystemApps || !it.systemApp || selected.contains(it.packageName)
        }

        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    appState.refreshInstalledApps()
                    val selectedAtEntry = selected.toSet()
                    apps.clear()
                    apps.addAll(
                        appState.installedApps.sortedWith(
                            compareBy<SafeAppEntry> { !selectedAtEntry.contains(it.packageName) }
                                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
                        )
                    )
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.safe_mode_apps_title),
                            style = MiuixTheme.textStyles.subtitle,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.safe_mode_selected_count, selected.size),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                if (visibleApps.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.safe_mode_empty),
                            modifier = Modifier.padding(MdSpacing.sm),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                } else {
                    itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { _, app ->
                        val checked = selected.contains(app.packageName)
                        val versionText = if (app.versionName.isBlank()) {
                            stringResource(R.string.safe_mode_version_unknown)
                        } else {
                            stringResource(R.string.safe_mode_version_format, app.versionName)
                        }
                        MiuixCard(modifier = Modifier.fillMaxWidth()) {
                            MiuixListRow(
                                title = app.label,
                                summary = app.packageName + "\n" + versionText,
                                bitmap = app.icon.asImageBitmap(),
                                checked = checked,
                                onCheckedChange = { toggle(app.packageName) },
                                showDivider = false,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(MdSpacing.lg)) }
            }
        }
    }

    if (showExplanation) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.settings_safe_mode_title),
            onDismissRequest = ::dismissExplanation,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.safe_mode_explanation),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        state = if (doNotShowAgain) ToggleableState.On else ToggleableState.Off,
                        onClick = { doNotShowAgain = !doNotShowAgain },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.safe_mode_dont_show_again))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(text = stringResource(android.R.string.ok), onClick = ::dismissExplanation)
                }
            }
        }
    }
}

@Composable
private fun MiuixInfoCard(rows: List<Pair<String, String>>) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            rows.forEachIndexed { index, (label, value) ->
                MiuixListRow(
                    title = label,
                    summary = value,
                    showDivider = index < rows.lastIndex,
                )
            }
        }
    }
}

@Composable
fun MiuixDiagnosticsScreen(appState: SpoofAppState, onBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_diagnostics_title),
                largeTitle = stringResource(R.string.settings_diagnostics_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.safe_mode_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val config = appState.loadedConfig
        if (!appState.initialized || config == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = config.profile
        val metrics = appState.activity.resources.displayMetrics
        val buildRows = listOf(
            stringResource(R.string.settings_field_brand) to Build.BRAND,
            stringResource(R.string.settings_field_manufacturer) to Build.MANUFACTURER,
            stringResource(R.string.settings_field_model) to Build.MODEL,
            stringResource(R.string.settings_field_device_codename) to Build.DEVICE,
            stringResource(R.string.settings_field_board) to Build.BOARD,
            stringResource(R.string.settings_field_hardware) to Build.HARDWARE,
            stringResource(R.string.settings_field_build_id) to Build.ID,
            stringResource(R.string.settings_field_build_display_id) to Build.DISPLAY,
            stringResource(R.string.settings_field_fingerprint) to Build.FINGERPRINT,
            stringResource(R.string.settings_field_android_release) to Build.VERSION.RELEASE,
            stringResource(R.string.settings_field_sdk_level) to Build.VERSION.SDK_INT.toString(),
            stringResource(R.string.settings_field_security_patch) to Build.VERSION.SECURITY_PATCH,
        )
        val runtimeRows = listOf(
            stringResource(R.string.home_info_profile_display_title) to stringResource(
                R.string.home_info_display_value_format,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
            ),
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            stringResource(R.string.settings_field_timezone) to TimeZone.getDefault().id,
            stringResource(R.string.settings_field_locale) to configuration.locales[0].toLanguageTag(),
        )
        val savedRows = listOf(
            stringResource(R.string.settings_hint_preset) to appState.presetLabel(config.selectedPresetId),
            stringResource(R.string.home_info_profile_mode_title) to stringResource(if (config.isCustomMode) R.string.mode_custom else R.string.mode_preset),
            stringResource(R.string.settings_field_model) to profile.model,
            stringResource(R.string.settings_field_device_codename) to profile.deviceCode,
            stringResource(R.string.settings_field_build_id) to profile.buildId,
            stringResource(R.string.settings_field_build_display_id) to profile.buildDisplayId,
            stringResource(R.string.settings_field_fingerprint) to profile.buildFingerprint,
            stringResource(R.string.settings_field_timezone) to profile.timezone,
            stringResource(R.string.settings_field_locale) to profile.locale,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            item { MiuixSectionTitle(stringResource(R.string.real_info_build_title)) }
            item { MiuixInfoCard(buildRows) }
            item { MiuixSectionTitle(stringResource(R.string.real_info_runtime_title), Modifier.padding(top = 22.dp)) }
            item { MiuixInfoCard(runtimeRows) }
            item { MiuixSectionTitle(stringResource(R.string.real_info_saved_title), Modifier.padding(top = 22.dp)) }
            item { MiuixInfoCard(savedRows) }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}

@Composable
fun MiuixAppInfoScreen(activity: RealInfoActivity) {
    val packageInfo = remember {
        activity.packageManager.getPackageInfo(activity.packageName, 0)
    }
    val appIcon = remember {
        activity.applicationInfo.loadIcon(activity.packageManager).toBitmap(128, 128).asImageBitmap()
    }
    val versionText = stringResource(
        R.string.app_info_version_format,
        packageInfo.versionName.orEmpty(),
        PackageInfoCompat.getLongVersionCode(packageInfo),
    )
    val overviewRows = listOf(
        stringResource(R.string.app_info_description_title) to stringResource(R.string.xposed_description),
        stringResource(R.string.app_info_package_title) to activity.packageName,
        versionText to stringResource(R.string.app_name),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.toolbar_real_info),
                largeTitle = stringResource(R.string.toolbar_real_info),
                navigationIcon = {
                    IconButton(onClick = activity::finish) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.safe_mode_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.primaryContainer,
                        contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MiuixTheme.textStyles.title3,
                                color = MiuixTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = versionText,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            item { MiuixSectionTitle(stringResource(R.string.app_info_overview_title), Modifier.padding(top = 22.dp)) }
            item { MiuixInfoCard(overviewRows) }
            item { MiuixSectionTitle(stringResource(R.string.app_info_credits_title), Modifier.padding(top = 22.dp)) }
            item {
                MiuixInfoCard(
                    listOf(stringResource(R.string.app_info_credits_title) to stringResource(R.string.app_info_credits_body)),
                )
            }
            item { MiuixSectionTitle(stringResource(R.string.app_info_links_title), Modifier.padding(top = 22.dp)) }
            item {
                MiuixCard(modifier = Modifier.fillMaxWidth()) {
                    MiuixListRow(
                        title = stringResource(R.string.app_info_github_title),
                        summary = stringResource(R.string.app_info_github_summary),
                        icon = Icons.Outlined.Code,
                        onClick = {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MIUIX_GITHUB_URL)))
                        },
                        showDivider = false,
                    )
                }
            }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}

@Composable
fun MiuixAppProfileAssignmentLoadingScreen(onBack: () -> Unit) {
    MiuixAssignmentScaffold(
        title = stringResource(R.string.app_profile_assignment_title),
        onBack = onBack,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_profiles),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
fun MiuixAppProfileAssignmentScreen(
    label: String,
    packageName: String,
    icon: Bitmap,
    profiles: List<AppProfileStore.Profile>,
    assignedProfileId: String?,
    defaultProfileName: String?,
    onProfileSelected: (String?) -> Unit,
    onCreateProfile: () -> Unit,
    onBack: () -> Unit,
) {
    MiuixAssignmentScaffold(
        title = stringResource(R.string.app_profile_assignment_title),
        onBack = onBack,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                MiuixCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = label,
                                style = MiuixTheme.textStyles.main,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = packageName,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(MdSpacing.sm))
                MiuixSectionTitle(stringResource(R.string.app_profile_select))
            }
            item {
                MiuixCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(null) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_profile_none),
                                style = MiuixTheme.textStyles.main,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (defaultProfileName != null) {
                                    stringResource(R.string.app_profile_none_summary_with_default, defaultProfileName)
                                } else {
                                    stringResource(R.string.app_profile_none_summary)
                                },
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = assignedProfileId == null, onClick = null)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(
                items = profiles,
                key = { _, profile -> profile.id },
            ) { _, profile ->
                MiuixCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(profile.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBox,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = profile.name,
                            style = MiuixTheme.textStyles.main,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = profile.id == assignedProfileId, onClick = null)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                Button(
                    onClick = onCreateProfile,
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
            item { Spacer(Modifier.height(MdSpacing.xl)) }
        }
    }
}

@Composable
private fun MiuixAssignmentScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                largeTitle = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        content = content,
    )
}
