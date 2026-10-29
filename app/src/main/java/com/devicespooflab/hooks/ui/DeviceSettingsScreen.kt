package com.devicespooflab.hooks.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.progressSemantics
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devicespooflab.hooks.AppProfileAssignmentActivity
import com.devicespooflab.hooks.ProfileEditorActivity
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.utils.ConfigManager
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppProfilesScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
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

    val visibleApps = remember(appState.installedApps, appState.hideSystemAppsInAppSettings) {
        if (appState.hideSystemAppsInAppSettings) {
            appState.installedApps.filterNot(SafeAppEntry::systemApp)
        } else {
            appState.installedApps
        }
    }
    AppSelectionList(
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun AppSelectionList(
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
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.app_profile_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                )
                Spacer(Modifier.height(MdSpacing.sm))
            }
            itemsIndexed(
                items = filteredApps,
                key = { _, app -> app.packageName },
            ) { index, app ->
                val assignedProfile = profileName(app.packageName)
                SegmentedListItem(
                    onClick = { onSelect(app) },
                    shapes = connectedListItemShapes(index, filteredApps.size),
                    modifier = if (index < filteredApps.lastIndex) {
                        Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                    } else {
                        Modifier
                    },
                    leadingContent = {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                    },
                    supportingContent = {
                        Text(
                            text = app.packageName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Text(
                            text = assignedProfile ?: stringResource(R.string.app_profile_unassigned),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (assignedProfile == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item { Spacer(Modifier.height(112.dp)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileManagementScreen(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profiles = appState.profileState?.profiles.orEmpty()
    var profileIdPendingDeletion by remember { mutableStateOf<String?>(null) }
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
            .padding(horizontal = 20.dp),
    ) {
        item { SectionTitle(stringResource(R.string.profile_management_list_title)) }
        itemsIndexed(
            items = profiles,
            key = { _, profile -> profile.id },
        ) { index, profile ->
            SegmentedListItem(
                onClick = {
                    editorLauncher.launch(ProfileEditorActivity.createEditIntent(context, profile.id))
                },
                shapes = connectedListItemShapes(index, profiles.size),
                modifier = if (index < profiles.lastIndex) {
                    Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
                } else {
                    Modifier
                },
                leadingContent = {
                    Icon(Icons.Outlined.AccountBox, contentDescription = null)
                },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.app_profile_assignment_count, appState.profileAssignmentCount(profile.id)))
                        if (appState.defaultProfileId() == profile.id) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_default_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                trailingContent = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            val isDefault = appState.defaultProfileId() == profile.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (isDefault) R.string.profile_remove_default else R.string.profile_set_default
                                        )
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        appState.setDefaultProfile(if (isDefault) null else profile.id)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.profile_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    profileIdPendingDeletion = profile.id
                                },
                                enabled = profiles.size > 1,
                            )
                        }
                    }
                },
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(profile.name)
            }
        }
        item {
            FilledTonalButton(
                onClick = { editorLauncher.launch(ProfileEditorActivity.createCreateIntent(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MdSpacing.sm),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(MdSpacing.xs))
                Text(stringResource(R.string.app_profile_create))
            }
        }
        item { Spacer(Modifier.height(112.dp)) }
    }

    val profilePendingDeletion = profiles.firstOrNull { it.id == profileIdPendingDeletion }
    if (profilePendingDeletion != null) {
        val assignmentCount = appState.profileAssignmentCount(profilePendingDeletion.id)
        AlertDialog(
            onDismissRequest = { profileIdPendingDeletion = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_delete_message,
                        profilePendingDeletion.name,
                        assignmentCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileIdPendingDeletion = null
                        scope.launch { appState.deleteProfile(profilePendingDeletion.id) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.profile_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileIdPendingDeletion = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

