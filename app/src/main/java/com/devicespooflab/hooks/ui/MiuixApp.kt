package com.devicespooflab.hooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.devicespooflab.hooks.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MiuixApp(appState: SpoofAppState) {
    val snackbarHostState = remember { SnackbarHostState() }
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.Home.name) }
    val destination = AppDestination.valueOf(destinationName)
    val message = appState.message
    var appsMenuExpanded by remember { mutableStateOf(false) }
    var navigationExpanded by rememberSaveable { mutableStateOf(false) }
    val windowSizeClass = calculateWindowSizeClass(appState.activity)
    val expanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val packageInfo = remember {
        appState.activity.packageManager.getPackageInfo(appState.activity.packageName, 0)
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            appState.consumeMessage()
        }
    }

    val title = stringResource(
        if (destination == AppDestination.Home) R.string.home_title else destination.labelRes,
    )
    val subtitle = if (destination == AppDestination.Settings) {
        "${packageInfo.versionName.orEmpty()} (${PackageInfoCompat.getLongVersionCode(packageInfo)})"
    } else {
        ""
    }

    val page: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MiuixTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = title,
                    largeTitle = title,
                    subtitle = subtitle,
                    color = MiuixTheme.colorScheme.surface,
                    actions = {
                        if (destination == AppDestination.Apps) {
                            IconButton(onClick = { appsMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.safe_mode_more_options),
                                )
                            }
                            MiuixMenuDialog(
                                show = appsMenuExpanded,
                                onDismiss = { appsMenuExpanded = false },
                            ) {
                                MiuixListRow(
                                    title = stringResource(R.string.safe_mode_hide_system_apps),
                                    checked = appState.hideSystemAppsInAppSettings,
                                    onCheckedChange = {
                                        appState.toggleSystemAppsInAppSettings()
                                        appsMenuExpanded = false
                                    },
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (!expanded) {
                    NavigationBar(
                        color = MiuixTheme.colorScheme.surface,
                        showDivider = false,
                    ) {
                        AppDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == destination,
                                onClick = { destinationName = item.name },
                                icon = if (item == destination) item.selectedIcon else item.unselectedIcon,
                                label = stringResource(item.labelRes),
                                colors = NavigationBarDefaults.navigationBarItemColors(
                                    unselectedContentColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    selectedContentColor = MiuixTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            Box(Modifier.fillMaxSize()) {
                if (appState.initialized) {
                    when (destination) {
                        AppDestination.Home -> MiuixHomeScreen(appState, contentPadding)
                        AppDestination.Apps -> MiuixAppProfilesScreen(appState, contentPadding)
                        AppDestination.Profiles -> MiuixProfileManagementScreen(appState, contentPadding)
                        AppDestination.Settings -> MiuixAppSettingsScreen(appState, contentPadding)
                    }
                }
            }
        }
    }

    if (expanded) {
        Row(Modifier.fillMaxSize()) {
            val railState = rememberNavigationRailState(
                if (navigationExpanded) NavigationRailValue.Expanded else NavigationRailValue.Collapsed,
            )
            NavigationRail(
                state = railState,
                header = {
                    Spacer(Modifier.height(MdSpacing.md))
                    Spacer(Modifier.height(MdSpacing.lg))
                },
            ) {
                AppDestination.entries.forEach { item ->
                    NavigationRailItem(
                        selected = item == destination,
                        onClick = { destinationName = item.name },
                        icon = if (item == destination) item.selectedIcon else item.unselectedIcon,
                        label = stringResource(item.labelRes),
                    )
                }
            }
            Box(Modifier.weight(1f)) { page() }
        }
    } else {
        page()
    }
}
