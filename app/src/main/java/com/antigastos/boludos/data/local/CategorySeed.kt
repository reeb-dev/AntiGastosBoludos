package com.antigastos.boludos.data.local

import com.antigastos.boludos.data.local.entity.CategoryEntity

internal object CategorySeed {
    val defaults: List<CategoryEntity> =
        listOf(
            CategoryEntity(name = "Delivery", slug = "delivery", iconName = "delivery", sortOrder = 0),
            CategoryEntity(name = "Bondi / Subte", slug = "transporte", iconName = "bus", sortOrder = 1),
            CategoryEntity(name = "Salidas", slug = "salidas", iconName = "nightlife", sortOrder = 2),
            CategoryEntity(name = "Super / chino", slug = "super", iconName = "cart", sortOrder = 3),
            CategoryEntity(name = "Servicios", slug = "servicios", iconName = "bolt", sortOrder = 4),
            CategoryEntity(name = "Boludeces", slug = "boludeces", iconName = "spark", sortOrder = 5),
            CategoryEntity(name = "Otros", slug = "otros", iconName = "more", sortOrder = 6),
        )
}
