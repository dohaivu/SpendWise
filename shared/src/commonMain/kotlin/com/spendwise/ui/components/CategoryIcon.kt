package com.spendwise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Toys
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category

internal data class CategoryIconOption(
    val key: String,
    val label: String,
    val imageVector: ImageVector
)

internal val categoryIconOptions = listOf(
    CategoryIconOption("restaurant", "Food", Icons.Outlined.Restaurant),
    CategoryIconOption("local_drink", "Drink", Icons.Outlined.LocalDrink),
    CategoryIconOption("checkroom", "Clothes", Icons.Outlined.Checkroom),
    CategoryIconOption("brush", "Beauty", Icons.Outlined.Brush),
    CategoryIconOption("wine_bar", "Bar", Icons.Outlined.WineBar),
    CategoryIconOption("medication", "Medicine", Icons.Outlined.Medication),
    CategoryIconOption("description", "Notes", Icons.Outlined.Description),
    CategoryIconOption("opacity", "Water", Icons.Outlined.Opacity),
    CategoryIconOption("train", "Train", Icons.Outlined.Train),
    CategoryIconOption("phone_iphone", "Phone", Icons.Outlined.PhoneIphone),
    CategoryIconOption("home", "Home", Icons.Outlined.Home),
    CategoryIconOption("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
    CategoryIconOption("savings", "Savings", Icons.Outlined.Savings),
    CategoryIconOption("gift", "Gift", Icons.Outlined.CardGiftcard),
    CategoryIconOption("money", "Money", Icons.Outlined.AttachMoney),
    CategoryIconOption("coin", "Coin", Icons.Outlined.MonetizationOn),
    CategoryIconOption("groups", "People", Icons.Outlined.Groups),
    CategoryIconOption("casino", "Games", Icons.Outlined.Casino),
    CategoryIconOption("shopping_cart", "Groceries", Icons.Outlined.ShoppingCart),
    CategoryIconOption("local_taxi", "Taxi", Icons.Outlined.LocalTaxi),
    CategoryIconOption("fastfood", "Fast food", Icons.Outlined.Fastfood),
    CategoryIconOption("local_pizza", "Pizza", Icons.Outlined.LocalPizza),
    CategoryIconOption("local_cafe", "Coffee", Icons.Outlined.LocalCafe),
    CategoryIconOption("local_bar", "Drinks", Icons.Outlined.LocalBar),
    CategoryIconOption("cake", "Dessert", Icons.Outlined.Cake),
    CategoryIconOption("directions_bus", "Bus", Icons.Outlined.DirectionsBus),
    CategoryIconOption("directions_car", "Car", Icons.Outlined.DirectionsCar),
    CategoryIconOption("local_gas_station", "Fuel", Icons.Outlined.LocalGasStation),
    CategoryIconOption("flight", "Travel", Icons.Outlined.Flight),
    CategoryIconOption("hotel", "Hotel", Icons.Outlined.Hotel),
    CategoryIconOption("local_hospital", "Hospital", Icons.Outlined.LocalHospital),
    CategoryIconOption("movie", "Movie", Icons.Outlined.Movie),
    CategoryIconOption("headphones", "Music", Icons.Outlined.Headphones),
    CategoryIconOption("sports_esports", "Gaming", Icons.Outlined.SportsEsports),
    CategoryIconOption("menu_book", "Books", Icons.Outlined.MenuBook),
    CategoryIconOption("fitness_center", "Fitness", Icons.Outlined.FitnessCenter),
    CategoryIconOption("self_improvement", "Wellness", Icons.Outlined.SelfImprovement),
    CategoryIconOption("pets", "Pets", Icons.Outlined.Pets),
    CategoryIconOption("toys", "Toys", Icons.Outlined.Toys),
    CategoryIconOption("handyman", "Tools", Icons.Outlined.Handyman),
    CategoryIconOption("computer", "Computer", Icons.Outlined.Computer),
    CategoryIconOption("inventory", "Package", Icons.Outlined.Inventory2),
    CategoryIconOption("cleaning", "Cleaning", Icons.Outlined.CleaningServices),
    CategoryIconOption("lightbulb", "Bills", Icons.Outlined.Lightbulb),
    CategoryIconOption("build", "Repair", Icons.Outlined.Build),
    CategoryIconOption("shopping_bag", "Shopping", Icons.Outlined.ShoppingBag),
    CategoryIconOption("other", "Other", Icons.Outlined.MoreHoriz)
)

internal fun categoryIconVector(iconKey: String): ImageVector {
    return categoryIconOptions.firstOrNull { it.key == iconKey }?.imageVector ?: Icons.Outlined.Category
}

@Composable
internal fun CategoryIcon(
    iconKey: String,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Icon(
        imageVector = categoryIconVector(iconKey),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

@Composable
internal fun CategoryLabel(
    category: Category,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CategoryIcon(
            iconKey = category.icon,
            tint = Color(category.color.toInt()),
            modifier = Modifier.size(18.dp)
        )
        Text(category.name)
    }
}
