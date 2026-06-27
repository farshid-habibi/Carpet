package com.farsh.carpetmapreader.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.farsh.carpetmapreader.R
import com.farsh.carpetmapreader.data.MapCell
import com.farsh.carpetmapreader.data.MapProject
import com.farsh.carpetmapreader.processor.CarpetReaderEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpetApp(viewModel: CarpetViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    
    // Force Persian RTL layout direction globally for carpet-weavers comfort
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onOpenProject = { projectId ->
                            viewModel.selectProject(projectId)
                            currentScreen = AppScreen.Reader(projectId)
                        }
                    )
                }
                is AppScreen.Reader -> {
                    ReaderScreen(
                        viewModel = viewModel,
                        projectId = screen.projectId,
                        onBack = {
                            viewModel.readerEngine.stop()
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
            }
        }
    }
}

sealed class AppScreen {
    object Dashboard : AppScreen()
    data class Reader(val projectId: Long) : AppScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CarpetViewModel,
    onOpenProject: (Long) -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedImgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Image Pickers launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                selectedImgBitmap = bitmap
                showCreateDialog = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            selectedImgBitmap = it
            showCreateDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "نقشه خوان صوتی فرش",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "درباره برنامه",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showStatusDialog = true }) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "وضعیت فعالیت‌ها",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { galleryLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن نقشه جدید")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header Banner configuration with our premium generated carpet illustration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.carpet_banner),
                            contentDescription = "قالی بافی سنتی",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "یار همیشگی هنرمندان قالی‌بافی",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "نقشه‌های شطرنجی و عددی خود را بارگذاری کنید تا برنامه آنها را خانه به خانه همراه با رنگ تلفظ برای شما بخواند.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                item {
                    Text(
                        "برنامه‌های بافندگی شما",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Widgets,
                                    contentDescription = "سبد خالی",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "هیچ نقشه‌ای هنوز بارگذاری نشده است.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "برای شروع، دکمه + پایین را زده یا گزینه زیر را انتخاب کنید.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("گالری عکس")
                                    }
                                    FilledTonalButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("عکاسی مستقیم")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(projects) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onDelete = { viewModel.deleteProject(project) }
                        )
                    }
                }

                
                // Extra padding bottom to avoid FAB overlapping list
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Global Loading Indicator overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {}, // absorb touch events
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                errorMessage ?: "در حال بارگذاری و تحلیل شطرنجی نقشه...",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // About dialog showing developer info
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Status dialog showing overall progress across all projects
    if (showStatusDialog) {
        ProjectsStatusDialog(
            projects = projects,
            onDismiss = { showStatusDialog = false }
        )
    }

    // Modal dialog to configure and split selected map into grid cells
    if (showCreateDialog && selectedImgBitmap != null) {
        CreateProjectDialog(
            bitmap = selectedImgBitmap!!,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, rows, cols, direction, mapType ->
                showCreateDialog = false
                viewModel.createProjectFromImage(selectedImgBitmap!!, name, rows, cols, direction, mapType)
            }
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "نقشه خوان صوتی فرش",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "نسخه ۱.۰",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "توسعه‌دهنده: فرشید حبیبی",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "09914310328",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "habibi.farshid75@gmail.com",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("باشه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProjectsStatusDialog(
    projects: List<MapProject>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var projectStats by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    LaunchedEffect(projects) {
        val db = com.farsh.carpetmapreader.data.AppDatabase.getDatabase(context)
        projectStats = projects.map { p ->
            val total = db.mapDao().getCellCountForProject(p.id)
            val read = db.mapDao().getReadCellCountForProject(p.id)
            Pair(total, read)
        }
    }

    val totalCells = projectStats.sumOf { it.first }
    val readCells = projectStats.sumOf { it.second }
    val completedProjects = projectStats.count { it.first > 0 && it.second >= it.first }
    val progress = if (totalCells > 0) readCells.toFloat() / totalCells.toFloat() else 0f
    val activeProjects = projects.size - completedProjects

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "وضعیت فعالیت‌ها",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Stats cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "${projects.size}",
                        label = "نقشه‌ها",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${activeProjects}",
                        label = "در حال اجرا",
                        color = Color(0xFFE09F3E),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${completedProjects}",
                        label = "تکمیل شده",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Per-project progress
                Text(
                    "پیشرفت هر نقشه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                projectStats.forEachIndexed { index, (total, read) ->
                    val p = projects[index]
                    val projectProgress = if (total > 0) read.toFloat() / total.toFloat() else 0f
                    val cardColor = projectCardColors[index % projectCardColors.size]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardColor.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(cardColor)
                                    )
                                    Text(
                                        p.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "${(projectProgress * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = cardColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { projectProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (projectProgress >= 1f) Color(0xFF2E7D32) else cardColor,
                                trackColor = cardColor.copy(alpha = 0.15f),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$read از $total خانه",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("باشه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val projectCardColors = listOf(
    Color(0xFF7C4DFF), // Soft Purple
    Color(0xFFFFB300), // Warm Amber
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF6D6D), // Coral
    Color(0xFF69F0AE), // Mint
    Color(0xFF448AFF), // Soft Blue
    Color(0xFFFFAB91), // Peach
    Color(0xFFB39DDB), // Lavender
)

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ProjectItemCard(
    project: MapProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val cardColor = remember(project.id) { projectCardColors[project.id.toInt().mod(projectCardColors.size)] }
    val isComplete = progress >= 1f

    LaunchedEffect(project.id) {
        val db = com.farsh.carpetmapreader.data.AppDatabase.getDatabase(context)
        val total = db.mapDao().getCellCountForProject(project.id)
        val read = db.mapDao().getReadCellCountForProject(project.id)
        progress = if (total > 0) read.toFloat() / total.toFloat() else 0f
    }

    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "حذف نقشه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "آیا از حذف «${project.name}» اطمینان دارید؟",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "این عمل قابل بازگشت نیست.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("انصراف", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                onDelete()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("حذف", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (project.mapType == "NUMERICAL") Icons.Default.FormatListNumbered else Icons.Default.GridOn,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val typeLabel = if (project.mapType == "NUMERICAL") "نقشه عددی" else "نقشه شطرنجی"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = cardColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                typeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cardColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (project.mapType == "GRID") {
                            Text(
                                "${project.rows}×${project.cols}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف پروژه",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isComplete) Color(0xFF4CAF50) else cardColor,
                    trackColor = cardColor.copy(alpha = 0.12f),
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) Color(0xFF4CAF50) else cardColor
                )
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (name: String, rows: Int, cols: Int, direction: String, mapType: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "ایجاد نقشه جدید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "پیش‌نمایش نقشه",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("نام نقشه فرش *") },
                    placeholder = { Text("مثلاً نقشه کاشان ۱") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("لطفاً نام نقشه را وارد کنید", color = MaterialTheme.colorScheme.error) }} else { null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                onConfirm(name, 10, 10, "RTL", "NUMERICAL")
                            }
                        },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("شروع پردازش هوشمند")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: CarpetViewModel,
    projectId: Long,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val activeProject by viewModel.activeProject.collectAsState()
    val activeCells by viewModel.activeCells.collectAsState()
    
    val playState by viewModel.playState.collectAsState()
    val currentHighlightCell by viewModel.currentHighlightCell.collectAsState()

    var showEditDialogCell by remember { mutableStateOf<MapCell?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Settings overlay toggle
    var showQuickSettings by remember { mutableStateOf(false) }

    // Attempt to decode bitmap in background memory
    LaunchedEffect(activeProject) {
        activeProject?.let {
            loadedBitmap = viewModel.loadProjectBitmap(it.imageUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        activeProject?.name ?: "نقشه خوان",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showQuickSettings = !showQuickSettings }) {
                        Icon(Icons.Default.Tune, contentDescription = "تنظیمات صوتی")
                    }
                    IconButton(
                        onClick = { viewModel.resetProjectProgress() },
                        modifier = Modifier.testTag("reset_progress_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "بازنشانی نقشه")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Layout Split: 1. Main visual viewport, 2. Interactive grid table, 3. Control deck at ground
            
            // 1. Image Viewport with overlay highlighter
            loadedBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "تصویر فرش",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Golden highlight grid overlay mapped to active rows/cols
                        val rows = activeProject?.rows ?: 1
                        val cols = activeProject?.cols ?: 1
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            val cellW = canvasW / cols
                            val cellH = canvasH / rows

                            currentHighlightCell?.let { cell ->
                                if (cell.rowIdx < rows && cell.colIdx < cols) {
                                    val top = cell.rowIdx * cellH
                                    val left = cell.colIdx * cellW

                                    // Draw blinking colored frame outline
                                    drawRect(
                                        color = Color(0xFFE09F3E),
                                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(cellW, cellH),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5.dp.toPx())
                                    )

                                    // Warm transparent gold fill
                                    drawRect(
                                        color = Color(0x55E09F3E),
                                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(cellW, cellH)
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("در حال بارگذاری تصویر نقشه...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }

            // Quick speech config sliders
            AnimatedVisibility(
                visible = showQuickSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeProject?.let { proj ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "تنظیم گام‌های موتور صوتی فارسی",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("سرعت خواندن: ${"%.1f".format(proj.speed)}x", fontSize = 11.sp)
                                    Slider(
                                        value = proj.speed,
                                        onValueChange = {
                                            viewModel.updateProjectSettings(
                                                speed = it,
                                                pauseCells = proj.pauseBetweenCells,
                                                pauseRows = proj.pauseBetweenRows,
                                                direction = proj.direction
                                            )
                                        },
                                        valueRange = 0.5f..2.0f
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("جهت تلفظ: ${proj.direction}", fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = {
                                                viewModel.updateProjectSettings(
                                                    speed = proj.speed,
                                                    pauseCells = proj.pauseBetweenCells,
                                                    pauseRows = proj.pauseBetweenRows,
                                                    direction = "RTL"
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (proj.direction == "RTL") MaterialTheme.colorScheme.primary else Color.Gray
                                            ),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("RTL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.updateProjectSettings(
                                                    speed = proj.speed,
                                                    pauseCells = proj.pauseBetweenCells,
                                                    pauseRows = proj.pauseBetweenRows,
                                                    direction = "LTR"
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (proj.direction == "LTR") MaterialTheme.colorScheme.primary else Color.Gray
                                            ),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("LTR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.updateProjectSettings(
                                                    speed = proj.speed,
                                                    pauseCells = proj.pauseBetweenCells,
                                                    pauseRows = proj.pauseBetweenRows,
                                                    direction = "ZIGZAG"
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (proj.direction == "ZIGZAG") MaterialTheme.colorScheme.primary else Color.Gray
                                            ),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("زیگزاگ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("مکث خانه‌ها: ${proj.pauseBetweenCells} میلی‌ثانیه", fontSize = 11.sp)
                                    Slider(
                                        value = proj.pauseBetweenCells.toFloat(),
                                        onValueChange = {
                                            viewModel.updateProjectSettings(
                                                speed = proj.speed,
                                                pauseCells = it.toLong(),
                                                pauseRows = proj.pauseBetweenRows,
                                                direction = proj.direction
                                            )
                                        },
                                        valueRange = 500f..3000f,
                                        steps = 5
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("مکث ردیف‌ها: ${proj.pauseBetweenRows} میلی‌ثانیه", fontSize = 11.sp)
                                    Slider(
                                        value = proj.pauseBetweenRows.toFloat(),
                                        onValueChange = {
                                            viewModel.updateProjectSettings(
                                                speed = proj.speed,
                                                pauseCells = proj.pauseBetweenCells,
                                                pauseRows = it.toLong(),
                                                direction = proj.direction
                                            )
                                        },
                                        valueRange = 1000f..5000f,
                                        steps = 8
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val isNumericalMap = activeProject?.mapType == "NUMERICAL"

            // 2. Interactive grid table or list matching map topology
            Text(
                if (isNumericalMap) "قرائت دستورالعمل کلاف‌های رنگی (جهت اصلاح یا پرش، بر روی شماره گره تپ کنید):" else "جدول تفکیکی خانه‌های نقشه (برای پخش، اصلاح یا پرش ضربه بزنید)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (isNumericalMap) {
                // Group cells by sectionName first, then within each section group them by color code rowIdx!
                val sectionsWithGroups = remember(activeCells) {
                    val leftCells = activeCells.filter { it.sectionName == "LEFT" }
                    val rightCells = activeCells.filter { it.sectionName == "RIGHT" }
                    val bottomCells = activeCells.filter { it.sectionName == "BOTTOM" }
                    
                    listOf(
                        Triple("ستون سمت چپ (کتبی چپ)", "LEFT", leftCells.groupBy { it.rowIdx }),
                        Triple("ستون سمت راست (کتبی راست)", "RIGHT", rightCells.groupBy { it.rowIdx }),
                        Triple("بخش پایین نقشه", "BOTTOM", bottomCells.groupBy { it.rowIdx })
                    ).filter { it.third.isNotEmpty() }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sectionsWithGroups.forEach { (sectionTitle, sectionCode, colorGroups) ->
                            // Header for Section
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            colorGroups.forEach { (colorCode, groupCells) ->
                                item {
                                    val firstCell = groupCells.firstOrNull()
                                    val colorName = firstCell?.colorName ?: "کد $colorCode"
                                    val cellBg = try { Color(android.graphics.Color.parseColor(firstCell?.colorHex ?: "#707070")) } catch (e: Exception) { MaterialTheme.colorScheme.surface }
                                    val groupIsAllRead = groupCells.all { it.isRead }
                                    val anyCellHighlightedInGroup = groupCells.any { it.id == currentHighlightCell?.id }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = when {
                                                    anyCellHighlightedInGroup -> 2.dp
                                                    groupIsAllRead -> 1.5.dp
                                                    else -> 0.5.dp
                                                },
                                                color = when {
                                                    anyCellHighlightedInGroup -> Color(0xFFE09F3E)
                                                    groupIsAllRead -> Color(0xFF2E7D32)
                                                    else -> Color.LightGray.copy(alpha = 0.5f)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                anyCellHighlightedInGroup -> Color(0xFFE09F3E).copy(alpha = 0.08f)
                                                groupIsAllRead -> Color(0xFF2E7D32).copy(alpha = 0.06f)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (anyCellHighlightedInGroup) 3.dp else if (groupIsAllRead) 0.dp else 1.dp
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            // Header Row: Color Badge & Label
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(cellBg)
                                                        .border(1.dp, Color.White, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "کلاف رنگ $colorName (کد $colorCode)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = when {
                                                        groupIsAllRead -> Color(0xFF2E7D32)
                                                        anyCellHighlightedInGroup -> Color(0xFF9E2A2B)
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                if (anyCellHighlightedInGroup && !groupIsAllRead) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFE09F3E).copy(alpha = 0.15f)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.PlayArrow,
                                                                contentDescription = null,
                                                                tint = Color(0xFFE09F3E),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "در حال پخش",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFE09F3E)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (groupIsAllRead) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = "کل کلاف بافته شده",
                                                                tint = Color(0xFF2E7D32),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "تکمیل شد",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Row/Flow of node chips belonging to this color (scrollable)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                groupCells.forEach { cell ->
                                                    val isHighlighted = currentHighlightCell?.id == cell.id
                                                    val isRead = cell.isRead
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                when {
                                                                    isHighlighted -> Color(0xFFE09F3E).copy(alpha = 0.2f)
                                                                    isRead -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                                }
                                                            )
                                                            .border(
                                                                width = if (isHighlighted) 2.dp else 0.5.dp,
                                                                color = when {
                                                                    isHighlighted -> Color(0xFFE09F3E)
                                                                    isRead -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                                                                    else -> Color.Gray.copy(alpha = 0.3f)
                                                                },
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                viewModel.readerEngine.jumpToCell(cell)
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            if (isRead && !isHighlighted) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color.Gray,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                            }
                                                            Text(
                                                                text = cell.number ?: "-",
                                                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else if (isRead) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "اصلاح",
                                                                tint = if (isHighlighted) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clickable { showEditDialogCell = cell }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Dynamic grid list layout mapping actual values
                val colsCount = activeProject?.cols ?: 1
                val rowsCount = activeProject?.rows ?: 1
                val cellsByRow = remember(activeCells) {
                    activeCells.groupBy { it.rowIdx }.toSortedMap()
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cellsByRow.forEach { (rowIndex, rowCells) ->
                            val rowAllRead = rowCells.all { it.isRead }
                            val rowIsPlaying = currentHighlightCell?.rowIdx == rowIndex && !rowAllRead

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Row status indicator
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    rowIsPlaying -> Color(0xFFE09F3E).copy(alpha = 0.2f)
                                                    rowAllRead -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            rowIsPlaying -> Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "در حال پخش",
                                                tint = Color(0xFFE09F3E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            rowAllRead -> Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "ردیف کامل",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            else -> Text(
                                                text = "${rowIndex + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Cells in this row
                                    rowCells.sortedBy { it.colIdx }.forEach { cell ->
                                        val isHighlighted = currentHighlightCell?.id == cell.id
                                        val cellBg = try { Color(android.graphics.Color.parseColor(cell.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.surface }
                                        val textColor = if (ColorUtils.isDarkColor(cellBg)) Color.White else Color.Black

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(cellBg)
                                                .border(
                                                    width = if (isHighlighted) 3.dp else 1.dp,
                                                    color = if (isHighlighted) Color(0xFFE09F3E) else if (cell.isRead) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    viewModel.readerEngine.jumpToCell(cell.rowIdx, cell.colIdx)
                                                }
                                                .testTag("cell_${cell.rowIdx}_${cell.colIdx}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cell.isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.White.copy(alpha = 0.35f))
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (cell.isRead) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "خوانده شده",
                                                        tint = Color(0xFF2E7D32),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = cell.number ?: "-",
                                                    color = textColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (cell.isRead) 9.sp else 11.sp
                                                )
                                                Text(
                                                    text = "${cell.rowIdx + 1}:${cell.colIdx + 1}",
                                                    color = textColor.copy(alpha = 0.8f),
                                                    fontSize = 7.sp
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.TopStart
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "اصلاح دستی",
                                                    tint = textColor.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clickable { showEditDialogCell = cell }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Audio Control deck at base
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Speech voice diagnostic badge
                    val hasAssets = viewModel.ttsManager.hasCustomAudioAssets
                    
                    val statusText: String
                    val statusColor: Color
                    
                    if (hasAssets) {
                        statusText = "✓ پخش صوتی: پرونده‌های صوتی فارسی در res/raw آماده (آفلاین)"
                        statusColor = Color(0xFF2E7D32)
                    } else {
                        statusText = "⚠️ پرونده‌های صوتی فارسی یافت نشد. لطفاً فایل‌های mp3 را در res/raw قرار دهید."
                        statusColor = MaterialTheme.colorScheme.error
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Status subtitle
                    val stateLabel = when (playState) {
                        CarpetReaderEngine.PlayState.PLAYING -> "در حال پخش صوتی..."
                        CarpetReaderEngine.PlayState.PAUSED -> "تلفظ متوقف شده"
                        CarpetReaderEngine.PlayState.FINISHED -> "بافت این ردیف/نقشه خاتمه یافت."
                        else -> "آماده برای خواندن صوتی"
                    }
                    Text(
                        stateLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        IconButton(
                            onClick = { viewModel.readerEngine.previous() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "خانه قبلی")
                        }

                        // Main Play Pause State Toggle
                        IconButton(
                            onClick = {
                                if (playState == CarpetReaderEngine.PlayState.PLAYING) {
                                    viewModel.readerEngine.pause()
                                } else {
                                    viewModel.readerEngine.start()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            val icon = if (playState == CarpetReaderEngine.PlayState.PLAYING) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            }
                            Icon(icon, contentDescription = "پخش یا توقف", modifier = Modifier.size(32.dp))
                        }

                        // Next Button
                        IconButton(
                            onClick = { viewModel.readerEngine.next() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "خانه بعدی")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    currentHighlightCell?.let { cell ->
                        Text(
                            "مکان فعلی: ردیف ${cell.rowIdx + 1}، گره ${cell.colIdx + 1} (کد: ${cell.number ?: "-"}، رنگ: ${cell.colorName})",
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // Modal correction dialog to let weaver manually edit grid entries
    if (showEditDialogCell != null) {
        ManualEditCellDialog(
            cell = showEditDialogCell!!,
            onDismiss = { showEditDialogCell = null },
            onConfirm = { num, colorName ->
                viewModel.updateCellManual(showEditDialogCell!!, num, colorName)
                showEditDialogCell = null
            }
        )
    }
}

/**
 * Custom Color utility detector to toggle white or black overlay text
 */
object ColorUtils {
    fun isDarkColor(color: Color): Boolean {
        val rgb = Triple((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
        val luminance = 0.2126 * rgb.first + 0.7152 * rgb.second + 0.0722 * rgb.third
        return luminance < 128
    }
}

@Composable
fun ManualEditCellDialog(
    cell: MapCell,
    onDismiss: () -> Unit,
    onConfirm: (num: String?, colorName: String) -> Unit
) {
    var textNum by remember { mutableStateOf(cell.number ?: "") }
    var colorName by remember { mutableStateOf(cell.colorName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "اصلاح دستی خانه ردیف ${cell.rowIdx + 1}، ستون ${cell.colIdx + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = textNum,
                    onValueChange = { textNum = it },
                    label = { Text("عدد نقشه (کد رنگ)") },
                    placeholder = { Text("مثلاً ۵") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it },
                    label = { Text("نام رنگ غالب به فارسی") },
                    placeholder = { Text("مثلاً قرمز گلی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("لغو")
                    }
                    Button(
                        onClick = { onConfirm(textNum.ifEmpty { null }, colorName) },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("ذخیره تغییرات")
                    }
                }
            }
        }
    }
}


