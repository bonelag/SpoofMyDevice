package com.devicespooflab.hooks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.data.DevicePreset
import com.devicespooflab.hooks.utils.ConfigManager

private data class EditorField(
    val fieldId: String,
    val labelRes: Int,
    val numeric: Boolean = false,
    val multiline: Boolean = false,
)

private val socFields = listOf(
    EditorField(ConfigManager.FIELD_SOC_MODEL, R.string.settings_field_soc_model),
    EditorField(ConfigManager.FIELD_SOC_MANUFACTURER, R.string.settings_field_soc_manufacturer),
    EditorField(ConfigManager.FIELD_BOARD_PLATFORM, R.string.settings_field_board_platform),
    EditorField(ConfigManager.FIELD_HARDWARE, R.string.settings_field_hardware),
    EditorField(ConfigManager.FIELD_CPU_HARDWARE, R.string.settings_field_cpu_hardware),
    EditorField(ConfigManager.FIELD_CPU_CORES, R.string.settings_field_cpu_cores, numeric = true),
    EditorField(ConfigManager.FIELD_GPU_VENDOR, R.string.settings_field_gpu_vendor),
    EditorField(ConfigManager.FIELD_GPU_RENDERER, R.string.settings_field_gpu_renderer),
    EditorField(ConfigManager.FIELD_GL_VERSION, R.string.settings_field_gl_version, multiline = true),
    EditorField(ConfigManager.FIELD_GLSL_VERSION, R.string.settings_field_glsl_version),
    EditorField(ConfigManager.FIELD_VULKAN_VERSION, R.string.settings_field_vulkan_version),
    EditorField(ConfigManager.FIELD_VULKAN_DRIVER_VERSION, R.string.settings_field_vulkan_driver_version),
    EditorField(ConfigManager.FIELD_VULKAN_LEVEL, R.string.settings_field_vulkan_level, numeric = true),
    EditorField(ConfigManager.FIELD_CPU_ABI, R.string.settings_field_cpu_abi),
    EditorField(ConfigManager.FIELD_CPU_ABI_LIST, R.string.settings_field_cpu_abi_list),
    EditorField(ConfigManager.FIELD_CPU_ABI_LIST_64, R.string.settings_field_cpu_abi_list_64),
    EditorField(ConfigManager.FIELD_CPU_ABI_LIST_32, R.string.settings_field_cpu_abi_list_32),
)

private val systemFields = listOf(
    EditorField(ConfigManager.FIELD_MODEL, R.string.settings_field_model),
    EditorField(ConfigManager.FIELD_MANUFACTURER, R.string.settings_field_manufacturer),
    EditorField(ConfigManager.FIELD_BRAND, R.string.settings_field_brand),
    EditorField(ConfigManager.FIELD_PRODUCT, R.string.settings_field_product_name),
    EditorField(ConfigManager.FIELD_DEVICE, R.string.settings_field_device_codename),
    EditorField(ConfigManager.FIELD_BOARD, R.string.settings_field_board),
    EditorField(ConfigManager.FIELD_ANDROID_RELEASE, R.string.settings_field_android_release),
    EditorField(ConfigManager.FIELD_SDK, R.string.settings_field_sdk_level, numeric = true),
    EditorField(ConfigManager.FIELD_SECURITY_PATCH, R.string.settings_field_security_patch),
    EditorField(ConfigManager.FIELD_BUILD_ID, R.string.settings_field_build_id),
    EditorField(ConfigManager.FIELD_BUILD_DISPLAY_ID, R.string.settings_field_build_display_id),
    EditorField(ConfigManager.FIELD_BUILD_INCREMENTAL, R.string.settings_field_build_incremental),
    EditorField(ConfigManager.FIELD_BOOTLOADER, R.string.settings_field_bootloader),
    EditorField(ConfigManager.FIELD_BUILD_USER, R.string.settings_field_build_user),
    EditorField(ConfigManager.FIELD_BUILD_HOST, R.string.settings_field_build_host),
    EditorField(ConfigManager.FIELD_BUILD_DATE_UTC, R.string.settings_field_build_date_utc, numeric = true),
    EditorField(ConfigManager.FIELD_BUILD_DATE, R.string.settings_field_build_date),
    EditorField(ConfigManager.FIELD_FINGERPRINT, R.string.settings_field_fingerprint, multiline = true),
    EditorField(ConfigManager.FIELD_BUILD_DESCRIPTION, R.string.settings_field_build_description, multiline = true),
    EditorField(ConfigManager.FIELD_BUILD_FLAVOR, R.string.settings_field_build_flavor),
    EditorField(ConfigManager.FIELD_BUILD_PRODUCT, R.string.settings_field_build_product),
    EditorField(ConfigManager.FIELD_BUILD_CHARACTERISTICS, R.string.settings_field_build_characteristics),
    EditorField(ConfigManager.FIELD_SERIAL_NUMBER, R.string.settings_field_serial_number),
)

private val screenFields = listOf(
    EditorField(ConfigManager.FIELD_SCREEN_WIDTH, R.string.settings_field_screen_width, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_HEIGHT, R.string.settings_field_screen_height, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_DENSITY, R.string.settings_field_screen_density, numeric = true),
)

private val networkFields = listOf(
    EditorField(ConfigManager.FIELD_BASEBAND, R.string.settings_field_baseband),
    EditorField(ConfigManager.FIELD_OPERATOR_ALPHA, R.string.settings_field_operator_name),
    EditorField(ConfigManager.FIELD_OPERATOR_NUMERIC, R.string.settings_field_operator_numeric, numeric = true),
    EditorField(ConfigManager.FIELD_SIM_OPERATOR_ALPHA, R.string.settings_field_sim_operator_name),
    EditorField(ConfigManager.FIELD_SIM_OPERATOR_NUMERIC, R.string.settings_field_sim_operator_numeric, numeric = true),
    EditorField(ConfigManager.FIELD_SIM_COUNTRY, R.string.settings_field_sim_country_iso),
    EditorField(ConfigManager.FIELD_TIMEZONE, R.string.settings_field_timezone),
    EditorField(ConfigManager.FIELD_LOCALE, R.string.settings_field_locale),
    EditorField(ConfigManager.FIELD_GNSS_MODEL, R.string.settings_field_gnss_model),
    EditorField(ConfigManager.FIELD_GNSS_YEAR, R.string.settings_field_gnss_year, numeric = true),
    EditorField(ConfigManager.FIELD_USER_AGENT, R.string.settings_field_user_agent, multiline = true),
)

private val advancedFields = listOf(
    EditorField(ConfigManager.FIELD_ANDROID_ID, R.string.settings_field_device_id),
    EditorField(ConfigManager.FIELD_IMEI, R.string.settings_field_imei, numeric = true),
    EditorField(ConfigManager.FIELD_MEID, R.string.settings_field_meid),
    EditorField(ConfigManager.FIELD_IMSI, R.string.settings_field_imsi, numeric = true),
    EditorField(ConfigManager.FIELD_ICCID, R.string.settings_field_iccid, numeric = true),
    EditorField(ConfigManager.FIELD_PHONE_NUMBER, R.string.settings_field_phone_number),
    EditorField(ConfigManager.FIELD_GAID, R.string.settings_field_gaid),
    EditorField(ConfigManager.FIELD_GSF_ID, R.string.settings_field_gsf_id),
    EditorField(ConfigManager.FIELD_MEDIA_DRM_ID, R.string.settings_field_media_drm_id),
    EditorField(ConfigManager.FIELD_APP_SET_ID, R.string.settings_field_app_set_id),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorLoadingScreen(
    creating: Boolean,
    onBack: () -> Unit,
) {
    ProfileEditorScaffold(
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
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.profile_editor_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ProfileEditorScreen(
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

    ProfileEditorScaffold(
        title = stringResource(if (creating) R.string.profile_create_title else R.string.profile_edit_title),
        saveEnabled = profileName.isNotBlank() && !appState.loading,
        snackbarHostState = snackbarHostState,
        onSave = onSave,
        onBack = onBack,
    ) { contentPadding ->
        ProfileEditorForm(
            appState = appState,
            scaffoldPadding = contentPadding,
            profileName = profileName,
            onProfileNameChange = onProfileNameChange,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileEditorScaffold(
    title: String,
    saveEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onSave: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSave, enabled = saveEnabled) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.save_profile),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorForm(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
) {
    val editor = appState.editor
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(1) } // Default to SYSTEM tab as in Device Info HW

    val tabs = listOf(
        stringResource(R.string.tab_soc),
        stringResource(R.string.tab_system),
        stringResource(R.string.tab_screen),
        stringResource(R.string.tab_network),
        stringResource(R.string.tab_identifiers),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
    ) {
        // Sticky Header: Profile Name & Preset Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.app_profile_name)) },
                    singleLine = true,
                )
                PresetSelector(
                    appState = appState,
                    onPresetSelected = { preset ->
                        if (profileName.isBlank()) {
                            onProfileNameChange(preset.displayName)
                        }
                    },
                )
            }
        }

        // Horizontal Tab Row
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider() },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // SOC Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(socFields, key = { it.fieldId }) { field ->
                            ProfileEditorField(field = field, editor = editor)
                        }
                    }
                }
                1 -> {
                    // SYSTEM Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(systemFields, key = { it.fieldId }) { field ->
                            ProfileEditorField(field = field, editor = editor)
                        }
                    }
                }
                2 -> {
                    // SCREEN Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(screenFields, key = { it.fieldId }) { field ->
                            ProfileEditorField(field = field, editor = editor)
                        }
                    }
                }
                3 -> {
                    // NETWORK Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(networkFields, key = { it.fieldId }) { field ->
                            ProfileEditorField(field = field, editor = editor)
                        }
                    }
                }
                4 -> {
                    // IDENTIFIERS Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        editor.randomizeAll()
                                        editor.customMode = true
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Casino,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.settings_generate_all))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        editor.clearAdvanced()
                                        editor.customMode = true
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.settings_clear_all))
                                }
                            }
                        }
                        items(advancedFields, key = { it.fieldId }) { field ->
                            AdvancedEditorField(field = field, editor = editor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PresetSelector(
    appState: SpoofAppState,
    onPresetSelected: ((DevicePreset) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val editor = appState.editor
    val selectedLabel = appState.presets.firstOrNull { it.id == editor.selectedPresetId }?.displayName.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_hint_preset)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            appState.presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.displayName)
                            if (preset.summary.isNotBlank()) {
                                Text(
                                    preset.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        editor.selectPreset(preset)
                        onPresetSelected?.invoke(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileEditorField(
    field: EditorField,
    editor: ProfileEditorState,
    modifier: Modifier = Modifier,
) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spoofEnabled,
            onCheckedChange = { editor.enabledFields[field.fieldId] = it },
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = editor.values[field.fieldId].orEmpty(),
            onValueChange = {
                editor.values[field.fieldId] = it
                editor.customMode = true
            },
            label = { Text(stringResource(field.labelRes)) },
            enabled = spoofEnabled,
            minLines = if (field.multiline) 2 else 1,
            maxLines = if (field.multiline) 5 else 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AdvancedEditorField(
    field: EditorField,
    editor: ProfileEditorState,
    modifier: Modifier = Modifier,
) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spoofEnabled,
            onCheckedChange = { editor.enabledFields[field.fieldId] = it },
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = editor.advancedValues[field.fieldId].orEmpty(),
            onValueChange = {
                editor.advancedValues[field.fieldId] = it
                editor.customMode = true
            },
            label = { Text(stringResource(field.labelRes)) },
            enabled = spoofEnabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = {
                editor.randomize(field.fieldId)
                editor.customMode = true
            },
            enabled = spoofEnabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Casino,
                contentDescription = stringResource(R.string.settings_generate_random),
            )
        }
    }
}
