package com.sanil.ride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.navigation.NavHostController
import androidx.compose.material3.ExperimentalMaterial3Api
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RideApp()
        }
        NotificationHelper.ensureChannel(this)
    }
}

@Composable
fun RideApp() {
    val navController = rememberNavController()
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") { SplashScreen(onDone = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }) }
                composable("login") { LoginScreen(navController) }
                composable("home") { MainHomeScreen(onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }, onOpenCategory = { category ->
                    navController.navigate("map/" + category.route)
                }, onOpenChat = {
                    navController.navigate("chatbot")
                }, onOpenML = {
                    navController.navigate("ml")
                }, onOpenMedia = {
                    navController.navigate("media")
                }) }

                composable("map/{category}") { backStackEntry ->
                    val route = backStackEntry.arguments?.getString("category") ?: "restaurants"
                    val category = PlaceCategory.fromRoute(route)
                    CategoryMapScreen(category = category) {
                        navController.popBackStack()
                    }
                }

                composable("chatbot") {
                    ChatBotScreen(onBack = { navController.popBackStack() })
                }

                composable("ml") {
                    MLScreen(onBack = { navController.popBackStack() })
                }

                composable("media") {
                    MediaScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onDone()
    }
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_scooter),
                    contentDescription = "Scooter Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Loading Ride...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // If granted (or on older Android), send notifications
        NotificationHelper.sendCategoryNotifications(context)
    }
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showRegister by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val repo = remember { AuthRepository() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ride",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your modern travel companion",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
            )
            Spacer(Modifier.height(32.dp))

            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            error = null
                            val u = username.trim()
                            val p = password
                            if (u.isEmpty() || p.isEmpty()) {
                                error = "Enter username and password"
                            } else {
                                repo.signInWithUsername(u, p) { result ->
                                    result.onSuccess {
                                        if (Build.VERSION.SDK_INT >= 33) {
                                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            NotificationHelper.sendCategoryNotifications(context)
                                        }
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }.onFailure { e ->
                                        error = e.message ?: "Sign-in failed"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Sign In") }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showRegister = true }) { Text("New registration") }
                        TextButton(onClick = { showForgot = true }) { Text("Forgot password?") }
                    }
                }
            }
        }
    }

    if (showRegister) {
        RegisterDialog(repo = repo, onDismiss = { showRegister = false }) {
            showRegister = false
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationHelper.sendCategoryNotifications(context)
            }
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    if (showForgot) {
        ForgotPasswordDialog(onDismiss = { showForgot = false }) {
            showForgot = false
        }
    }
}

@Composable
fun RegisterDialog(repo: AuthRepository, onDismiss: () -> Unit, onRegistered: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        confirmButton = {
            TextButton(enabled = !loading, onClick = {
                error = null
                val u = username.trim()
                val e = email.trim()
                val p = password
                val c = confirm
                when {
                    u.isEmpty() -> error = "Enter username"
                    e.isEmpty() -> error = "Enter email"
                    !e.contains('@') -> error = "Enter valid email"
                    p.length < 6 -> error = "Password must be at least 6 chars"
                    p != c -> error = "Passwords do not match"
                    else -> {
                        loading = true
                        repo.register(u, e, p) { result ->
                            loading = false
                            result.onSuccess {
                                onRegistered()
                            }.onFailure { ex ->
                                error = ex.message ?: "Registration failed"
                            }
                        }
                    }
                }
            }) { Text(if (loading) "Please wait..." else "Register") }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New Registration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                message = if (user.trim().equals("sanil", ignoreCase = true)) {
                    "Your password is: 123"
                } else {
                    "User not found. Try username: sanil"
                }
                onDone()
            }) { Text("Retrieve") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Forgot Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Enter username") })
                if (message != null) {
                    Text(message!!, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

enum class PlaceCategory(val label: String, val route: String, val color: Color, val markerHue: Float) {
    Restaurants("Restaurants", "restaurants", Color(0xFF4CAF50), BitmapDescriptorFactory.HUE_GREEN),
    Cafes("Cafes", "cafes", Color(0xFFFFEB3B), BitmapDescriptorFactory.HUE_YELLOW),
    Parks("Parks", "parks", Color(0xFF2196F3), BitmapDescriptorFactory.HUE_AZURE),
    Malls("Malls", "malls", Color(0xFFE91E63), BitmapDescriptorFactory.HUE_ROSE),
    Hotels("Hotels", "hotels", Color(0xFFFF9800), BitmapDescriptorFactory.HUE_ORANGE),
    Hostels("Hostels", "hostels", Color(0xFF7E57C2), BitmapDescriptorFactory.HUE_VIOLET);

    companion object {
        fun fromRoute(route: String): PlaceCategory = entries.firstOrNull { it.route == route } ?: Restaurants
    }
}

private val mumbaiCenter = LatLng(19.0760, 72.8777)
private val naviMumbaiCenter = LatLng(19.0330, 73.0297)

@Composable
fun MainHomeScreen(onLogout: () -> Unit, onOpenCategory: (PlaceCategory) -> Unit, onOpenChat: () -> Unit, onOpenML: () -> Unit, onOpenMedia: () -> Unit) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF42A5F5))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Ride",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Get moving in minutes",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.85f))
                )
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                )
                CategoryGrid(
                    categories = listOf(
                        PlaceCategory.Restaurants,
                        PlaceCategory.Cafes,
                        PlaceCategory.Parks,
                        PlaceCategory.Malls,
                        PlaceCategory.Hotels,
                        PlaceCategory.Hostels
                    ),
                    onOpenCategory = onOpenCategory
                )
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303F9F))
                ) { Text("AI Chat Bot", color = Color.White) }
                Button(
                    onClick = onOpenML,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) { Text("ML Reviews", color = Color.White) }
                OutlinedButton(
                    onClick = onOpenMedia,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Media", color = Color.White) }
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Logout") }
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<PlaceCategory>, onOpenCategory: (PlaceCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in categories.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                for (category in row) {
                    CategoryMapButton(category = category, onClick = { onOpenCategory(category) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryMapButton(category: PlaceCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mumbaiCenter, 10f)
    }
    ElevatedCard(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false, tiltGesturesEnabled = false, rotationGesturesEnabled = false),
                    cameraPositionState = cameraPositionState
                ) {
                    // Add two center markers to indicate region; colored by category
                    Marker(state = MarkerState(position = mumbaiCenter), title = "Mumbai", icon = BitmapDescriptorFactory.defaultMarker(category.markerHue))
                    Marker(state = MarkerState(position = naviMumbaiCenter), title = "Navi Mumbai", icon = BitmapDescriptorFactory.defaultMarker(category.markerHue))
                }
                // Semi-transparent overlay to tint by category color for quick visual cue
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(category.color.copy(alpha = 0.12f))
                )
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = category.color)
            ) {
                Text(category.label, color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMapScreen(category: PlaceCategory, onBack: () -> Unit) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.06, 72.9), 10.5f)
    }
    Scaffold(topBar = {
        SmallTopAppBar(title = { Text(category.label) }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                for ((pos, title) in categoryLocations(category)) {
                    Marker(state = MarkerState(pos), title = title, icon = BitmapDescriptorFactory.defaultMarker(category.markerHue))
                }
            }
        }
    }
}

private fun categoryLocations(category: PlaceCategory): List<Pair<LatLng, String>> {
    return when (category) {
        PlaceCategory.Restaurants -> listOf(
            LatLng(18.9259, 72.8327) to "Leopold Cafe",
            LatLng(19.0721, 72.8700) to "Global Fusion",
            LatLng(19.1076, 72.8376) to "Penthouse Mumbai",
            LatLng(19.0176, 72.8562) to "Britannia & Co.",
            LatLng(19.2350, 72.8440) to "Peshawri",
            LatLng(19.0269, 73.0593) to "Barbeque Nation Vashi"
        )
        PlaceCategory.Cafes -> listOf(
            LatLng(19.0721, 72.8826) to "Starbucks BKC",
            LatLng(19.1030, 72.8260) to "Blue Tokai Bandra",
            LatLng(18.9262, 72.8325) to "Kala Ghoda Cafe",
            LatLng(19.0665, 73.0083) to "Third Wave Coffee Vashi"
        )
        PlaceCategory.Parks -> listOf(
            LatLng(18.9323, 72.8269) to "Horniman Circle Garden",
            LatLng(19.1641, 72.9931) to "Central Park Kharghar",
            LatLng(19.1136, 72.8258) to "Joggers Park",
            LatLng(19.0546, 72.8407) to "Five Gardens"
        )
        PlaceCategory.Malls -> listOf(
            LatLng(19.1730, 72.8604) to "Inorbit Malad",
            LatLng(19.1176, 72.9050) to "Phoenix Marketcity",
            LatLng(19.0004, 73.1175) to "Seawoods Grand Central",
            LatLng(19.0650, 72.9988) to "Inorbit Vashi"
        )
        PlaceCategory.Hotels -> listOf(
            LatLng(19.0965, 72.8743) to "ITC Maratha",
            LatLng(19.0998, 72.8261) to "Taj Lands End",
            LatLng(18.9220, 72.8336) to "The Taj Mahal Palace",
            LatLng(19.0448, 72.8206) to "The St. Regis"
        )
        PlaceCategory.Hostels -> listOf(
            LatLng(19.0730, 72.8340) to "Backpacker Panda Colaba",
            LatLng(19.0639, 72.8340) to "Zostel Mumbai",
            LatLng(19.0650, 73.0020) to "Hosteller Vashi"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatBotScreen(onBack: () -> Unit) {
    val botQuestions = remember {
        listOf(
            "Hello! Namaste! How is your day today?",
            "What are you up to right now?",
            "Have you had water in the last hour?",
            "Did you get enough sleep last night?",
            "What city are you in today?",
            "What’s the weather like there?",
            "Any plans for the evening?",
            "Do you prefer tea or coffee?",
            "What’s your favorite cuisine?",
            "What’s one thing that made you smile today?",
            "Do you enjoy traveling?",
            "Which place would you love to visit next?",
            "Are you more of an early bird or night owl?",
            "What’s your favorite sport?",
            "Do you like reading books?",
            "Which genre do you enjoy the most?",
            "What’s the last movie you watched?",
            "Do you listen to podcasts?",
            "What kind of music do you like?",
            "Who is your favorite singer or band?",
            "Do you cook often?",
            "What’s your comfort food?",
            "How do you relax after a long day?",
            "Do you meditate or do yoga?",
            "How many steps have you walked today?",
            "Do you prefer mountains or beaches?",
            "What’s your favorite app on your phone?",
            "Do you like board games?",
            "What’s your favorite season?",
            "Do you prefer sunrise or sunset?",
            "Are you learning anything new right now?",
            "What’s your favorite subject from school?",
            "Do you like coding?",
            "Which programming language do you enjoy?",
            "What’s a small goal you have this week?",
            "Do you keep a to‑do list?",
            "How often do you exercise?",
            "What’s your favorite fruit?",
            "Do you like spicy food?",
            "How many cups of tea/coffee do you have daily?",
            "Do you enjoy street food?",
            "What’s your favorite dessert?",
            "Do you like museums?",
            "What’s your favorite holiday?",
            "Do you prefer online shopping or in‑store?",
            "How do you commute usually?",
            "Do you use public transport?",
            "Which mobile OS do you use?",
            "Dark mode or light mode?",
            "Do you keep phone notifications on or off?",
            "Do you like taking photos?",
            "What’s your favorite camera feature?",
            "Do you back up your photos?",
            "How many hours do you spend online daily?",
            "Do you like writing journals?",
            "What’s your favorite quote?",
            "Do you play video games?",
            "What’s your favorite game genre?",
            "Do you enjoy gardening?",
            "Do you like pets?",
            "Dog person, cat person, or both?",
            "Do you like to sing?",
            "Have you tried any new hobby this month?",
            "Do you speak multiple languages?",
            "Which language would you like to learn?",
            "Do you enjoy festivals?",
            "What’s your favorite festival food?",
            "Do you follow any sports team?",
            "What inspires you?",
            "Do you set yearly goals?",
            "How do you celebrate achievements?",
            "Do you prefer emails or messages?",
            "How often do you take breaks while working?",
            "What keeps you motivated?",
            "Do you like trying new restaurants?",
            "What’s a must‑visit place in your city?",
            "Do you prefer solo trips or group trips?",
            "How do you plan your travel?",
            "Window seat or aisle seat?",
            "Do you carry a power bank?",
            "What’s one gadget you can’t live without?",
            "Do you use a smartwatch?",
            "Which social platform do you enjoy most?",
            "Do you like volunteering?",
            "What cause do you care about?",
            "Do you prefer working from home or office?",
            "What’s your ideal weekend?",
            "Do you follow a morning routine?",
            "How many alarms do you set?",
            "Do you meal prep?",
            "How many liters of water do you drink daily?",
            "Do you prefer audio books or ebooks?",
            "What’s your favorite childhood cartoon?",
            "Do you collect anything?",
            "Have you tried meditation apps?",
            "Do you like puzzles?",
            "How do you handle stress?",
            "What’s something you’re grateful for today?",
            "Do you enjoy sunsets by the sea?",
            "What’s one skill you want to master?",
            "Thanks for chatting! Anything else you’d like to share?"
        )
    }

    var input by remember { mutableStateOf("") }
    var nextIndex by remember { mutableStateOf(0) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() } // true = bot, false = user

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(true to botQuestions[0])
            nextIndex = 1
        }
    }

    Scaffold(topBar = {
        SmallTopAppBar(title = { Text("AI Chat Bot") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(messages) { (isBot, text) ->
                    val bg = if (isBot) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    val align = if (isBot) Alignment.Start else Alignment.End
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End) {
                        Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
                            Text(text = text, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Type a message") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val trimmed = input.trim()
                    if (trimmed.isNotEmpty()) {
                        messages.add(false to trimmed)
                        input = ""
                        if (nextIndex < botQuestions.size) {
                            messages.add(true to botQuestions[nextIndex])
                            nextIndex++
                        } else {
                            messages.add(true to "It was great chatting! 👋")
                        }
                    }
                }) { Text("Send") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MLScreen(onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(PlaceCategory.Hotels) }
    var menuExpanded by remember { mutableStateOf(false) }
    val reviews = remember(selectedCategory) { generateReviews(selectedCategory, 100) }

    Scaffold(topBar = {
        SmallTopAppBar(title = { Text("ML Reviews") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category:")
                Spacer(Modifier.width(8.dp))
                Box {
                    OutlinedButton(onClick = { menuExpanded = true }) {
                        Text(selectedCategory.label)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        for (c in PlaceCategory.entries) {
                            DropdownMenuItem(text = { Text(c.label) }, onClick = {
                                selectedCategory = c
                                menuExpanded = false
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(reviews) { (text, rating) ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text)
                            Spacer(Modifier.height(6.dp))
                            Text("Rating: " + String.format("%.1f", rating) + "/5")
                        }
                    }
                }
            }
        }
    }
}

private fun generateReviews(category: PlaceCategory, count: Int): List<Pair<String, Float>> {
    val seed = category.route.hashCode()
    val rnd = Random(seed)
    val templates = listOf(
        "Amazing %s! Clean, friendly staff, would visit again.",
        "Loved this %s, great value and location.",
        "Decent %s, could improve service times.",
        "Fantastic %s experience, highly recommended!",
        "Average %s, but the ambiance was nice.",
        "Superb %s with excellent facilities.",
        "Good %s, a bit crowded but worth it.",
        "Top-notch %s, very satisfied.",
        "Nice %s for families and friends.",
        "Solid %s, reasonable pricing."
    )
    val label = category.label.lowercase()
    return List(count) { idx ->
        val t = templates[idx % templates.size]
        val rating = (3.0f + rnd.nextFloat() * 2.0f)
        ("#" + (idx + 1) + " " + String.format(t, label)) to (Math.round(rating * 10f) / 10f)
    }
}

private object NotificationHelper {
    private const val CHANNEL_ID = "ride_ai_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Recommendations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for hotels, restaurants, cafes, parks, malls, hostels"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun sendCategoryNotifications(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        val alerts = listOf(
            "Hotels near Bandra – special offers today" to PlaceCategory.Hotels,
            "Restaurants around Fort, Mumbai – trending now" to PlaceCategory.Restaurants,
            "Cafes in Bandra/Khar – new openings" to PlaceCategory.Cafes,
            "Parks in Navi Mumbai – best hours to visit" to PlaceCategory.Parks,
            "Malls: Phoenix Marketcity & Seawoods – weekend deals" to PlaceCategory.Malls,
            "Hostels in Vashi & Colaba – budget stays" to PlaceCategory.Hostels
        )
        var id = 1000
        for ((text, category) in alerts) {
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scooter)
                .setContentTitle("AI Alert: ${category.label}")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(id, notif)
            id++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val msg = if (success) "Photo saved to gallery" else "Photo capture cancelled"
        scope.launch { snackbar.showSnackbar(msg) }
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val msg = if (success) "Video saved to gallery" else "Video recording cancelled"
        scope.launch { snackbar.showSnackbar(msg) }
    }
    val recordAudioLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val ok = result.resultCode == Activity.RESULT_OK
        val msg = if (ok) "Audio saved to gallery" else "Audio recording cancelled"
        scope.launch { snackbar.showSnackbar(msg) }
    }

    fun ensurePermissionsAnd(action: () -> Unit) {
        val needCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED
        val needAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needCamera || needAudio) {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            action()
        } else action()
    }

    fun createImageUri(): Uri? {
        val name = "IMG_" + System.currentTimeMillis() + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Ride")
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun createVideoUri(): Uri? {
        val name = "VID_" + System.currentTimeMillis() + ".mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Ride")
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    Scaffold(topBar = {
        SmallTopAppBar(title = { Text("Media") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }, snackbarHost = { SnackbarHost(hostState = snackbar) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                ensurePermissionsAnd {
                    val uri = createImageUri()
                    if (uri != null) {
                        pendingImageUri = uri
                        takePictureLauncher.launch(uri)
                    } else scope.launch { snackbar.showSnackbar("Unable to create image location") }
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Capture Photo") }

            Button(onClick = {
                ensurePermissionsAnd {
                    val uri = createVideoUri()
                    if (uri != null) {
                        pendingVideoUri = uri
                        captureVideoLauncher.launch(uri)
                    } else scope.launch { snackbar.showSnackbar("Unable to create video location") }
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Record Video") }

            Button(onClick = {
                ensurePermissionsAnd {
                    val intent = android.content.Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                    recordAudioLauncher.launch(intent)
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Record Audio") }
        }
    }
}
