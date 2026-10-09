package com.salman.navegacao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.salman.navegacao.ui.theme.NavegacaoTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NavegacaoTheme { RecipeApp() } }
    }
}

@Serializable sealed interface AppRoute
@Serializable data object Splash : AppRoute
@Serializable data object Login : AppRoute
@Serializable data object Home : AppRoute
@Serializable data object Recipes : AppRoute
@Serializable data object Favorites : AppRoute
@Serializable data object Profile : AppRoute
@Serializable data class RecipeDetail(val recipeId: Int) : AppRoute

data class Recipe(val id: Int, val name: String, val description: String, val time: String, val emoji: String)

class RecipeViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    val recipes = listOf(
        Recipe(1, "Risoto de cogumelos", "Arroz cremoso com cogumelos frescos, parmesão e ervas.", "35 min", "🍄"),
        Recipe(2, "Panquecas de banana", "Panquecas macias com banana, aveia e mel.", "20 min", "🥞"),
        Recipe(3, "Salada mediterrânea", "Tomate, pepino, azeitonas, feta e azeite.", "15 min", "🥗"),
        Recipe(4, "Sopa de abóbora", "Sopa aveludada com abóbora assada e gengibre.", "40 min", "🍲"),
        Recipe(5, "Tacos de legumes", "Tortilhas recheadas com legumes e molho de iogurte.", "25 min", "🌮"),
        Recipe(6, "Bolo de chocolate", "Bolo fofinho com cobertura de cacau.", "50 min", "🍰")
    )
    private val preferences = application.getSharedPreferences("sabor_afeto", android.content.Context.MODE_PRIVATE)
    var isLoggedIn by mutableStateOf(preferences.getBoolean("logged_in", false))
        private set
    var favorites by mutableStateOf(setOf<Int>())
        private set
    fun login() { isLoggedIn = true; preferences.edit().putBoolean("logged_in", true).apply() }
    fun toggleFavorite(id: Int) { favorites = if (id in favorites) favorites - id else favorites + id }
}

private data class Tab(val route: AppRoute, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(Tab(Home, "Início", Icons.Default.Home), Tab(Recipes, "Receitas", Icons.Default.MenuBook), Tab(Favorites, "Favoritos", Icons.Default.Favorite))

@Composable
fun RecipeApp(vm: RecipeViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showTabs = tabs.any { destination?.hasRoute(it.route::class) == true }
    Scaffold(
        bottomBar = {
            if (showTabs) NavigationBar {
                tabs.forEach { tab ->
                    val selected = destination?.hasRoute(tab.route::class) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.navigate(tab.route) { popUpTo(Home) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Splash, modifier = Modifier.padding(padding)) {
            composable<Splash> {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(900)
                    // A decisão de sessão fica no host, fora da tela Splash.
                    nav.navigate(if (vm.isLoggedIn) Home else Login) { popUpTo<Splash> { inclusive = true } }
                }
                SplashScreen()
            }
            composable<Login> { LoginScreen(onLogin = { vm.login(); nav.navigate(Home) { popUpTo<Login> { inclusive = true } } }) }
            composable<Home> { HomeScreen(vm, onBrowse = { nav.navigate(Recipes) }, onRecipe = { nav.navigate(RecipeDetail(it)) }) }
            composable<Recipes> { RecipeListScreen(vm, vm.recipes, onRecipe = { nav.navigate(RecipeDetail(it)) }) }
            composable<Favorites> { RecipeListScreen(vm, vm.recipes.filter { it.id in vm.favorites }, onRecipe = { nav.navigate(RecipeDetail(it)) }, title = "Favoritas") }
            composable<Profile> { SimpleScreen("Seu perfil", "Organize suas receitas favoritas em um só lugar.") }
            composable<RecipeDetail> { entry ->
                val route = entry.toRoute<RecipeDetail>()
                vm.recipes.firstOrNull { it.id == route.recipeId }?.let { RecipeDetailScreen(vm, it) }
                    ?: SimpleScreen("Receita não encontrada", "Volte à lista e escolha outra receita.")
            }
        }
    }
}

@Composable private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🍋", style = MaterialTheme.typography.displayLarge)
            Text("Sabor & Afeto", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Receitas para todos os dias", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable private fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Bem-vindo à sua cozinha", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("Entre para guardar suas receitas favoritas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(24.dp)); Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Entrar") }
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Continuar como visitante") }
    }
}

@Composable private fun HomeScreen(vm: RecipeViewModel, onBrowse: () -> Unit, onRecipe: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bom apetite!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Uma ideia gostosa para hoje", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        vm.recipes.firstOrNull()?.let { RecipeCard(it, onClick = { onRecipe(it.id) }) }
        Spacer(Modifier.height(16.dp)); Button(onClick = onBrowse, modifier = Modifier.fillMaxWidth()) { Text("Explorar receitas") }
        Spacer(Modifier.height(24.dp)); Text("Feito em casa, com carinho", style = MaterialTheme.typography.titleMedium)
        Text("Descubra pratos simples para deixar cada refeição especial.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun RecipeListScreen(vm: RecipeViewModel, recipes: List<Recipe>, onRecipe: (Int) -> Unit, title: String = "Receitas") {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
        if (recipes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (title == "Favoritas") "Você ainda não salvou receitas. Toque no coração em uma receita para guardá-la." else "Nenhuma receita encontrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(recipes, key = { it.id }) { RecipeCard(it, onClick = { onRecipe(it.id) }) }
        }
    }
}

@Composable private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(recipe.emoji, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(recipe.description, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("⏱ ${recipe.time}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable private fun RecipeDetailScreen(vm: RecipeViewModel, recipe: Recipe) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(recipe.emoji, style = MaterialTheme.typography.displayLarge) }
        Spacer(Modifier.height(20.dp)); Text(recipe.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("⏱ ${recipe.time}", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(26.dp)); Text("Ingredientes e preparo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp)); Text("Separe os ingredientes frescos, prepare com calma e sirva ainda quentinho. Ajuste os temperos ao seu gosto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f)); Button(onClick = { vm.toggleFavorite(recipe.id) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Favorite, contentDescription = if (recipe.id in vm.favorites) "Remover dos favoritos" else "Adicionar aos favoritos")
            Spacer(Modifier.width(8.dp)); Text(if (recipe.id in vm.favorites) "Salva nos favoritos" else "Salvar nos favoritos")
        }
    }
}

@Composable private fun SimpleScreen(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text(title, style = MaterialTheme.typography.headlineMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
