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
import org.jetbrains.compose.resources.StringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.*

internal data class CategoryIconOption(
    val key: String,
    val label: StringResource,
    val imageVector: ImageVector
)

internal val categoryIconOptions = listOf(
    CategoryIconOption("restaurant", Res.string.category_icon_food, Icons.Outlined.Restaurant),
    CategoryIconOption("local_drink", Res.string.category_icon_drink, Icons.Outlined.LocalDrink),
    CategoryIconOption("checkroom", Res.string.category_icon_clothes, Icons.Outlined.Checkroom),
    CategoryIconOption("brush", Res.string.category_icon_beauty, Icons.Outlined.Brush),
    CategoryIconOption("wine_bar", Res.string.category_icon_bar, Icons.Outlined.WineBar),
    CategoryIconOption("medication", Res.string.category_icon_medicine, Icons.Outlined.Medication),
    CategoryIconOption("description", Res.string.category_icon_notes, Icons.Outlined.Description),
    CategoryIconOption("opacity", Res.string.category_icon_water, Icons.Outlined.Opacity),
    CategoryIconOption("train", Res.string.category_icon_train, Icons.Outlined.Train),
    CategoryIconOption("phone_iphone", Res.string.category_icon_phone, Icons.Outlined.PhoneIphone),
    CategoryIconOption("home", Res.string.category_icon_home, Icons.Outlined.Home),
    CategoryIconOption("wallet", Res.string.category_icon_wallet, Icons.Outlined.AccountBalanceWallet),
    CategoryIconOption("savings", Res.string.category_icon_savings, Icons.Outlined.Savings),
    CategoryIconOption("gift", Res.string.category_icon_gift, Icons.Outlined.CardGiftcard),
    CategoryIconOption("money", Res.string.category_icon_money, Icons.Outlined.AttachMoney),
    CategoryIconOption("coin", Res.string.category_icon_coin, Icons.Outlined.MonetizationOn),
    CategoryIconOption("groups", Res.string.category_icon_people, Icons.Outlined.Groups),
    CategoryIconOption("casino", Res.string.category_icon_games, Icons.Outlined.Casino),
    CategoryIconOption("shopping_cart", Res.string.category_icon_groceries, Icons.Outlined.ShoppingCart),
    CategoryIconOption("local_taxi", Res.string.category_icon_taxi, Icons.Outlined.LocalTaxi),
    CategoryIconOption("fastfood", Res.string.category_icon_fast_food, Icons.Outlined.Fastfood),
    CategoryIconOption("local_pizza", Res.string.category_icon_pizza, Icons.Outlined.LocalPizza),
    CategoryIconOption("local_cafe", Res.string.category_icon_coffee, Icons.Outlined.LocalCafe),
    CategoryIconOption("local_bar", Res.string.category_icon_drinks, Icons.Outlined.LocalBar),
    CategoryIconOption("cake", Res.string.category_icon_dessert, Icons.Outlined.Cake),
    CategoryIconOption("directions_bus", Res.string.category_icon_bus, Icons.Outlined.DirectionsBus),
    CategoryIconOption("directions_car", Res.string.category_icon_car, Icons.Outlined.DirectionsCar),
    CategoryIconOption("local_gas_station", Res.string.category_icon_fuel, Icons.Outlined.LocalGasStation),
    CategoryIconOption("flight", Res.string.category_icon_travel, Icons.Outlined.Flight),
    CategoryIconOption("hotel", Res.string.category_icon_hotel, Icons.Outlined.Hotel),
    CategoryIconOption("local_hospital", Res.string.category_icon_hospital, Icons.Outlined.LocalHospital),
    CategoryIconOption("movie", Res.string.category_icon_movie, Icons.Outlined.Movie),
    CategoryIconOption("headphones", Res.string.category_icon_music, Icons.Outlined.Headphones),
    CategoryIconOption("sports_esports", Res.string.category_icon_gaming, Icons.Outlined.SportsEsports),
    CategoryIconOption("menu_book", Res.string.category_icon_books, Icons.Outlined.MenuBook),
    CategoryIconOption("fitness_center", Res.string.category_icon_fitness, Icons.Outlined.FitnessCenter),
    CategoryIconOption("self_improvement", Res.string.category_icon_wellness, Icons.Outlined.SelfImprovement),
    CategoryIconOption("pets", Res.string.category_icon_pets, Icons.Outlined.Pets),
    CategoryIconOption("toys", Res.string.category_icon_toys, Icons.Outlined.Toys),
    CategoryIconOption("handyman", Res.string.category_icon_tools, Icons.Outlined.Handyman),
    CategoryIconOption("computer", Res.string.category_icon_computer, Icons.Outlined.Computer),
    CategoryIconOption("inventory", Res.string.category_icon_package, Icons.Outlined.Inventory2),
    CategoryIconOption("cleaning", Res.string.category_icon_cleaning, Icons.Outlined.CleaningServices),
    CategoryIconOption("lightbulb", Res.string.category_icon_bills, Icons.Outlined.Lightbulb),
    CategoryIconOption("build", Res.string.category_icon_repair, Icons.Outlined.Build),
    CategoryIconOption("shopping_bag", Res.string.category_icon_shopping, Icons.Outlined.ShoppingBag),
    CategoryIconOption("other", Res.string.category_icon_other, Icons.Outlined.MoreHoriz)
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
