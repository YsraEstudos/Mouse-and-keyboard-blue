package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Entity(tableName = "custom_layouts")
data class CustomLayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val category: String,
    val widgetsJson: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface CustomLayoutDao {
    @Query("SELECT * FROM custom_layouts ORDER BY updatedAt DESC")
    fun getAllLayouts(): Flow<List<CustomLayoutEntity>>

    @Query("SELECT COUNT(*) FROM custom_layouts")
    suspend fun countLayouts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayout(layout: CustomLayoutEntity): Long

    @Query("DELETE FROM custom_layouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [CustomLayoutEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customLayoutDao(): CustomLayoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_bt_db"
                ).build().also { INSTANCE = it }
            }
    }
}

data class CustomLayout(
    val id: Long = 0L,
    val name: String,
    val description: String,
    val category: String,
    val widgets: List<CustomLayoutWidget>,
    val updatedAt: Long = System.currentTimeMillis(),
)

class CustomLayoutRepository(private val dao: CustomLayoutDao) {

    val allLayouts: Flow<List<CustomLayout>> = kotlinx.coroutines.flow.flow {
        dao.getAllLayouts().collect { entities ->
            emit(
                entities.map { entity ->
                    CustomLayout(
                        id = entity.id,
                        name = entity.name,
                        description = entity.description,
                        category = entity.category,
                        widgets = CustomLayoutSerializer.deserializeWidgets(entity.widgetsJson),
                        updatedAt = entity.updatedAt,
                    )
                }
            )
        }
    }

    suspend fun ensureStarterLayouts() {
        if (dao.countLayouts() == 0) {
            CustomLayoutSerializer.starterLayouts().forEach { starter ->
                installPreset(starter)
            }
        }
    }

    suspend fun installPreset(dto: CustomLayoutExportDto): Long {
        return saveLayout(
            CustomLayout(
                name = dto.name,
                description = dto.description,
                category = dto.category,
                widgets = dto.widgets,
            )
        )
    }

    suspend fun saveLayout(layout: CustomLayout): Long {
        val entity = CustomLayoutEntity(
            id = layout.id,
            name = layout.name.trim().ifEmpty { "Layout sem nome" },
            description = layout.description.trim(),
            category = layout.category.trim().ifEmpty { "Personalizado" },
            widgetsJson = CustomLayoutSerializer.serializeWidgets(layout.widgets),
            updatedAt = System.currentTimeMillis(),
        )
        return dao.upsertLayout(entity)
    }

    suspend fun duplicateLayout(layout: CustomLayout): Long {
        return saveLayout(
            layout.copy(
                id = 0L,
                name = "${layout.name} (Cópia)",
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteLayout(id: Long) {
        dao.deleteById(id)
    }

    fun exportToJson(layout: CustomLayout): String =
        CustomLayoutSerializer.exportLayoutJson(
            CustomLayoutExportDto(
                name = layout.name,
                description = layout.description,
                category = layout.category,
                widgets = layout.widgets,
            )
        )

    suspend fun exportAllBackupJson(): String {
        val current = allLayouts.first()
        val dtos = current.map {
            CustomLayoutExportDto(it.name, it.description, it.category, it.widgets)
        }
        return CustomLayoutSerializer.exportAllBundleJson(dtos)
    }

    suspend fun importFromJson(json: String): Result<Int> {
        // Support both single layout JSON and full backup bundle JSON array
        val trimmed = json.trim()
        if (trimmed.startsWith("[")) {
            val list = CustomLayoutSerializer.importAllBundleJson(trimmed)
                ?: return Result.failure(IllegalArgumentException("Backup JSON inválido."))
            list.forEach { installPreset(it) }
            return Result.success(list.size)
        }
        val dto = CustomLayoutSerializer.importLayoutJson(trimmed)
            ?: return Result.failure(IllegalArgumentException("JSON de layout inválido."))
        installPreset(dto)
        return Result.success(1)
    }
}
