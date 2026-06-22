package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.BcvDatabase
import com.example.data.BcvRepository
import com.example.ui.*
import com.example.ui.theme.*

enum class AppScreen {
    Inicio,
    Historial
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Room Local Database
        val database = Room.databaseBuilder(
            applicationContext,
            BcvDatabase::class.java,
            "bcv_rates_v2.db"
        ).fallbackToDestructiveMigration().build()
        
        val repository = BcvRepository(database.bcvDao())
        
        // Get SharedPreferences
        val sharedPrefs = getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
        
        // 2. Initialize MainViewModel using custom factory with SharedPreferences
        val factory = ViewModelFactory(repository, sharedPrefs)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = isDark) {
                MainAppLayout(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.Inicio) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MONITOR BCV",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tasa Oficial de Cambio",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Manual Refresh Button styled as a modern crypto swap action icon
                        IconButton(
                            onClick = { viewModel.refreshRates() },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("btn_sync_rates")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronizar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.Inicio -> HomeScreen(viewModel = viewModel)
                AppScreen.Historial -> HistoryScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(72.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = "Mercado",
                icon = Icons.Default.Home,
                selected = currentScreen == AppScreen.Inicio,
                onClick = { onScreenSelected(AppScreen.Inicio) },
                testTag = "nav_btn_home"
            )

            NavBarItem(
                label = "Historial",
                icon = Icons.Default.List,
                selected = currentScreen == AppScreen.Historial,
                onClick = { onScreenSelected(AppScreen.Historial) },
                testTag = "nav_btn_history"
            )
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.2.sp
        )
    }
}
