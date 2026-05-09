package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExpenseEntity): Long

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Delete
    suspend fun delete(entity: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT e.id, e.amountPesos, e.note, e.occurredAt, c.name as categoryName, c.slug as categorySlug
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.occurredAt >= :start AND e.occurredAt < :end
        ORDER BY e.occurredAt DESC
        """
    )
    fun observeExpenseRows(start: Long, end: Long): Flow<List<ExpenseListRow>>

    @Query("SELECT SUM(amountPesos) FROM expenses WHERE occurredAt >= :start AND occurredAt < :end")
    fun observeTotalForRange(start: Long, end: Long): Flow<Long?>

    @Query("SELECT SUM(amountPesos) FROM expenses WHERE occurredAt >= :start AND occurredAt < :end")
    suspend fun sumTotal(start: Long, end: Long): Long?

    @Query(
        """
        SELECT c.slug as slug, SUM(e.amountPesos) as totalPesos
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.occurredAt >= :start AND e.occurredAt < :end
        GROUP BY c.id
        ORDER BY totalPesos DESC
        """
    )
    suspend fun sumByCategory(start: Long, end: Long): List<CategorySpendRow>
}

data class ExpenseListRow(
    val id: Long,
    val amountPesos: Long,
    val note: String?,
    val occurredAt: Long,
    val categoryName: String,
    val categorySlug: String,
)

data class CategorySpendRow(
    val slug: String,
    val totalPesos: Long,
)
