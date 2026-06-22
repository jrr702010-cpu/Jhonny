package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
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
    Historial,
    Asistente
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Room Local Database
        val database = Room.databaseBuilder(
            applicationContext,
            BcvDatabase::class.java,
            "bcv_rates_v1.db"
        ).fallbackToDestructiveMigration().build()
        
        val repository = BcvRepository(database.bcvDao())
        
        // 2. Initialize MainViewModel using custom factory
        val factory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
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
                        Text(
                            text = "Monitor BCV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NaturalCharcoal
                        )

                        // Manual Refresh Button
                        IconButton(
                            onClick = { viewModel.refreshRates() },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_sync_rates")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar",
                                tint = MossGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalLinen,
                    titleContentColor = NaturalCharcoal
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            // Highly customized navigation matching the exact Natural Tones HTML specification
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
            // Smooth horizontal cross-fading animations between tabs
            when (currentScreen) {
                AppScreen.Inicio -> HomeScreen(viewModel = viewModel)
                AppScreen.Historial -> HistoryScreen(viewModel = viewModel)
                AppScreen.Asistente -> AiScreen(viewModel = viewModel)
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
        // Divider line separating layout content from navbar
        HorizontalDivider(color = StoneBeige, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClayGrey)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(80.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = "Inicio",
                icon = Icons.Default.Home,
                selected = currentScreen == AppScreen.Inicio,
                onClick = { onScreenSelected(AppScreen.Inicio) },
                testTag = "nav_btn_home"
            )

            NavBarItem(
                label = "Tendencia",
                icon = Icons.Default.List,
                selected = currentScreen == AppScreen.Historial,
                onClick = { onScreenSelected(AppScreen.Historial) },
                testTag = "nav_btn_history"
            )

            NavBarItem(
                label = "Asistente",
                icon = Icons.Default.Face,
                selected = currentScreen == AppScreen.Asistente,
                onClick = { onScreenSelected(AppScreen.Asistente) },
                testTag = "nav_btn_ai"
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
            .clickable { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active visual state gets a pill outline container in SageGreen
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) SageGreen else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MossDark else MutedOlive,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) NaturalCharcoal else MutedOlive,
            letterSpacing = 0.2.sp
        )
    }
}
