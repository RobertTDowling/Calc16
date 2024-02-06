package com.rtdti.calc16

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

@Entity
data class PadTable(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "pad") val pad: String,
)

@Entity
data class StackTable(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "entry") val pad: Double,
)

@Entity
data class FormatParametersTable(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "epsilon") val epsilon: Double,
    @ColumnInfo(name = "decimalPlaces") val decimalPlaces: Double,
    @ColumnInfo(name = "numberFormat") val numberFormat: String
)

@Dao
interface CalcDao {
    @Query("SELECT * from PadTable")
    fun getPad(): Flow<List<PadTable>>

    @Query("SELECT * from StackTable")
    fun getStack(): Flow<List<StackTable>>

    @Query("SELECT * from FormatParametersTable LIMIT 1")
    fun getFormatParameters(): Flow<List<FormatParametersTable>>

    @Query("INSERT INTO PadTable (epoch, pad) VALUES (:epoch, :pad)")
    suspend fun insertPad(epoch: Int, pad: String)

    @Query("INSERT INTO StackTable (epoch, depth, entry) VALUES (:epoch, :depth, :entry)")
    suspend fun insertStack(epoch: Int, depth: Int, entry: Double)

    @Query("INSERT INTO FormatParametersTable (epoch, epsilon, decimalPlaces, numberFormat) VALUES (:epoch, :epsilon, :decimalPlaces, :numberFormat)")
    suspend fun insertFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat)

    @Query("DELETE from PadTable")
    fun clearPadTable()

    @Query("DELETE from StackTable")
    fun clearStackTable()

    @Query("DELETE from FormatParametersTable")
    fun clearFormatParametersTable()

    @Query("UPDATE PadTable SET pad = :pad WHERE epoch = :epoch")
    fun updatePad(epoch: Int, pad: String)

    @Query("UPDATE StackTable SET entry = :entry WHERE epoch = :epoch AND depth = :depth")
    fun updateStack(epoch: Int, depth: Int, entry: Double)

    @Query("UPDATE FormatParametersTable SET epsilon = :epsilon, decimalPlaces = :decimalPlaces, numberFormat = :numberFormat WHERE epoch = :epoch")
    fun updateFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat)
}

@Database(entities = arrayOf(PadTable::class, StackTable::class, FormatParametersTable::class),
    version = 1, exportSchema = false)
public abstract class CalcDatabase : RoomDatabase() {
    abstract fun calcDao(): CalcDao
    companion object {
        @Volatile
        private var INSTANCE: CalcDatabase? = null
        fun getDatabase(context: Context): CalcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalcDatabase::class.java,
                    "calc_database"
                )
                    .fallbackToDestructiveMigration()
                    //.addCallback(TimeLogDatabaseCallback(scope))
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

class CalcRepository(private val calcDao: CalcDao) {
    val pad: Flow<List<PadTable>> = calcDao.getPad()
    val stack: Flow<List<StackTable>> = calcDao.getStack()
    val formatParameters: Flow<List<FormatParametersTable>> = calcDao.getFormatParameters()

    @WorkerThread
    suspend fun insertPad(epoch: Int, pad: String) {
        calcDao.insertPad(epoch, pad)
    }
    @WorkerThread
    suspend fun insertStack(epoch: Int, depth: Int, entry: Double) {
        calcDao.insertStack(epoch, depth, entry)
    }
    @WorkerThread
    suspend fun insertFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) {
        calcDao.insertFormatParameters(epoch, epsilon, decimalPlaces, numberFormat)
    }
    @WorkerThread
    suspend fun updatePad(epoch: Int, pad: String) {
        calcDao.updatePad(epoch, pad)
    }
    @WorkerThread
    suspend fun updateStack(epoch: Int, depth: Int, entry: Double) {
        calcDao.updateStack(epoch, depth, entry)
    }
    @WorkerThread
    suspend fun updateFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) {
        calcDao.insertFormatParameters(epoch, epsilon, decimalPlaces, numberFormat)
    }
}

class CalcViewModelFactory(private val repository: CalcRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalcViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalcViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CalcViewModel(private val repository: CalcRepository) : ViewModel() {
    val pad: LiveData<List<PadTable>> = repository.pad.asLiveData()
    val stack: LiveData<List<StackTable>> = repository.stack.asLiveData()
    val formatParameters: LiveData<List<FormatParametersTable>> = repository.formatParameters.asLiveData()
    fun insertPad(epoch: Int, pad: String) = viewModelScope.launch {
        repository.insertPad(epoch, pad)
    }
    fun insertStack(epoch: Int, depth: Int, entry: Double) = viewModelScope.launch {
        repository.insertStack(epoch, depth, entry)
    }
    fun insertFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) = viewModelScope.launch {
        repository.insertFormatParameters(epoch, epsilon, decimalPlaces, numberFormat)
    }
    fun updatePad(epoch: Int, pad: String) = viewModelScope.launch {
        repository.updatePad(epoch, pad)
    }
    fun updateStack(epoch: Int, depth: Int, entry: Double) = viewModelScope.launch {
        repository.updateStack(epoch, depth, entry)
    }
    fun updateFormatParameters(epoch: Int, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) = viewModelScope.launch {
        repository.updateFormatParameters(epoch, epsilon, decimalPlaces, numberFormat)
    }
}
