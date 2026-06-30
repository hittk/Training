package com.kargathra.fitness.data.repo

import com.kargathra.fitness.data.db.FavouriteDao
import com.kargathra.fitness.data.db.FavouriteEntity
import kotlinx.coroutines.flow.Flow

class FavouriteRepository(private val dao: FavouriteDao) {

    /** Stream of all favourited exercise ids. */
    fun favouriteIds(): Flow<List<String>> = dao.favouriteIds()

    /** Stream of whether a single exercise is favourited. */
    fun isFavourite(id: String): Flow<Boolean> = dao.isFavourite(id)

    suspend fun toggle(id: String, makeFavourite: Boolean) {
        if (makeFavourite) dao.add(FavouriteEntity(exerciseId = id))
        else dao.remove(id)
    }
}
