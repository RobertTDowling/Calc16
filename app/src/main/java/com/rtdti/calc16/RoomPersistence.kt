package com.rtdti.calc16

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity
data class PadTable(
    @PrimaryKey(autoGenerate = true) val rowid: Int = 0,
    @ColumnInfo(name = "pad") val pad: String,
)

@Entity
data class StackTable(
    @PrimaryKey(autoGenerate = true) val rowid: Int = 0,
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "value") val value: Double,
)

@Entity
data class FormatTable(
    @PrimaryKey(autoGenerate = true) val rowid: Int = 0,
    @ColumnInfo(name = "epsilon") val epsilon: Double,
    @ColumnInfo(name = "decimalPlaces") val decimalPlaces: Int,
    @ColumnInfo(name = "numberFormat") val numberFormat: String,
)

@Entity
data class EverythingTable(
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "pad") val pad: String,
    @ColumnInfo(name = "epsilon") val epsilon: Double,
    @ColumnInfo(name = "decimalPlaces") val decimalPlaces: Int,
    @ColumnInfo(name = "numberFormat") val numberFormat: String,
)

@Dao
interface CalcDao {

    @Query("SELECT StackTable.epoch, StackTable.depth, StackTable.value, PadTable.pad, FormatTable.epsilon, FormatTable.decimalPlaces, FormatTable.numberFormat FROM StackTable left JOIN PadTable left JOIN FormatTable")
    fun getEverythingTable(): Flow<List<EverythingTable>>

    // @Update // Won't update unless table has a matching entry
    // suspend fun updatePad(padTable: PadTable)
    @Query("INSERT OR REPLACE INTO PadTable (rowid, pad) values (:rowid, :pad)")
    suspend fun insertOrUpdatePad(rowid: Int, pad: String)
    // @Query("INSERT OR REPLACE INTO PadTable (rowid, pad) values (:padTable)")
    // suspend fun updatePad(padTable: PadTable) // does not compile
    // @Upsert // Appends, never updates
    // suspend fun updatePad(padTable: PadTable)

    @Insert
    suspend fun insertStack(stackTable: StackTable)

    @Query("DELETE FROM StackTable where epoch = :epoch")
    fun rollbackStack(epoch: Int)

    @Query("INSERT OR REPLACE INTO FormatTable (rowid, epsilon, decimalPlaces, numberFormat) values (:rowid, :epsilon, :decimalPlaces, :numberFormat)")
    suspend fun insertOrUpdateFormatTable(rowid: Int, epsilon: Double, decimalPlaces: Int, numberFormat: String)

    @Transaction
    suspend fun insertFullStack(listStackTable: List<StackTable>) {
        for (st in listStackTable) {
            insertStack(st)
        }
    }
}

@Database(entities = arrayOf(PadTable::class, StackTable::class, FormatTable::class),
    version = 1, exportSchema = false)
abstract class CalcDatabase : RoomDatabase() {
    abstract fun calcDao(): CalcDao
    companion object {
        @Volatile
        private var Instance: CalcDatabase? = null
        fun getDatabase(context: Context): CalcDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    CalcDatabase::class.java,
                    "calc_database"
                )
                    .fallbackToDestructiveMigration()
                    //.addCallback(TimeLogDatabaseCallback(scope))
                    //.allowMainThreadQueries()
                    .createFromAsset("empty_calc_database.db")
                    .build()
                Instance = instance
                // return instance
                instance
            }
        }
    }
}

interface CalcRepository {
    fun getEverythingTable(): Flow<List<EverythingTable>>
    suspend fun insertOrUpdatePad(pad: String)
    suspend fun insertStack(stackTable: StackTable)
    fun rollbackStack(epoch: Int)
    suspend fun insertFullStack(lst: List<StackTable>)
    suspend fun insertOrUpdateFormatTable(formatTable: FormatTable)
}

class OfflineCalcRepository(private val calcDao: CalcDao) : CalcRepository {
    override fun getEverythingTable(): Flow<List<EverythingTable>> = calcDao.getEverythingTable()
    override suspend fun insertOrUpdatePad(pad: String) = calcDao.insertOrUpdatePad(1, pad)
    override suspend fun insertStack(stackTable: StackTable) = calcDao.insertStack(stackTable)
    override fun rollbackStack(epoch: Int) = calcDao.rollbackStack(epoch)
    override suspend fun insertFullStack(lst: List<StackTable>) = calcDao.insertFullStack(lst)
    override suspend fun insertOrUpdateFormatTable(formatTable: FormatTable) =
        calcDao.insertOrUpdateFormatTable(formatTable.rowid, formatTable.epsilon, formatTable.decimalPlaces, formatTable.numberFormat)
}

interface AppContainer {
    val calcRepository: CalcRepository
}

// [AppContainer] implementation that provides instance of [OfflineItemsRepository]
class AppDataContainer(private val context: Context) : AppContainer {
    override val calcRepository: CalcRepository by lazy {
        OfflineCalcRepository(CalcDatabase.getDatabase(context).calcDao())
    }
}

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            CalcViewModel(/*this.createSavedStateHandle(),*/calc16application().container.calcRepository)
        }
    }
}

fun CreationExtras.calc16application(): Calc16Application { // =
    val foobar = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    foobar?.let {
        Log.i("TAGME", it.toString())
    }
    return (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Calc16Application)
}

class Calc16Application : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}