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
data class ZuperTable(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "epoch") val epoch: Int,
    @ColumnInfo(name = "pad") val pad: String,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "stack00") val stack00: Double,
    @ColumnInfo(name = "stack01") val stack01: Double,
    @ColumnInfo(name = "stack02") val stack02: Double,
    @ColumnInfo(name = "stack03") val stack03: Double,
    @ColumnInfo(name = "stack04") val stack04: Double,
    @ColumnInfo(name = "stack05") val stack05: Double,
    @ColumnInfo(name = "stack06") val stack06: Double,
    @ColumnInfo(name = "stack07") val stack07: Double,
    @ColumnInfo(name = "stack08") val stack08: Double,
    @ColumnInfo(name = "stack09") val stack09: Double,
    @ColumnInfo(name = "epsilon") val epsilon: Double,
    @ColumnInfo(name = "decimalPlaces") val decimalPlaces: Int,
    @ColumnInfo(name = "numberFormat") val numberFormat: String
) {
    // constructor(uid: Int, epoch: Int, pad: Pad, stack: Stack, numberFormat: NumberFormat):
    companion object { // Super-cool way to make a 2nd constructor.  I'll never remember this
        operator fun invoke(uid: Int, epoch: Int, pad: Pad, stack: Stack, formatParameters: FormatParameters): ZuperTable {
            val depth = stack.depthGet()
            val s = Array<Double>(10) { 0.0 }
            for (i in 0..depth-1) {
                s[i] = stack.entry(i).value
            }
            return ZuperTable(uid, epoch, pad.get(), depth,
                            s[0], s[1], s[2], s[3], s[4],
                            s[5], s[6], s[7], s[8], s[9],
                            formatParameters.epsilon.value,
                            formatParameters.decimalPlaces.value,
                            formatParameters.numberFormat.value.toString())
        }
    }
}

@Dao
interface CalcDao {
    @Query("SELECT * from ZuperTable")
    fun getZuper(): Flow<List<ZuperTable>>

    @Query(
        """INSERT INTO ZuperTable 
        (epoch, pad, depth, 
         stack00, stack01, stack02, stack03, stack04, 
         stack05, stack06, stack07, stack08, stack09, 
         epsilon, decimalPlaces, numberFormat)
         VALUES  (:epoch, :pad, :depth,
                  :stack00, :stack01, :stack02, :stack03, :stack04, 
                  :stack05, :stack06, :stack07, :stack08, :stack09,
                  :epsilon, :decimalPlaces, :numberFormat)"""
    )
    suspend fun insertZuper(
        epoch: Int,
        pad: String,
        depth: Int,
        stack00: Double,
        stack01: Double,
        stack02: Double,
        stack03: Double,
        stack04: Double,
        stack05: Double,
        stack06: Double,
        stack07: Double,
        stack08: Double,
        stack09: Double,
        epsilon: Double,
        decimalPlaces: Double,
        numberFormat: NumberFormat
    )

    @Query("DELETE from ZuperTable")
    fun clearZuperTable()

    @Query(
        """UPDATE ZuperTable SET 
        pad = :pad, depth = :depth, 
        stack00 = :stack00, stack01 = :stack01, stack02 = :stack02, stack03 = :stack03, stack04 = :stack04,
        stack05 = :stack05, stack06 = :stack06, stack07 = :stack07, stack08 = :stack08, stack09 = :stack09,
        epsilon = :epsilon, decimalPlaces = :decimalPlaces, numberFormat = :numberFormat 
        WHERE epoch = :epoch"""
    )
    suspend fun updateZuper(
        epoch: Int,
        pad: String,
        depth: Int,
        stack00: Double,
        stack01: Double,
        stack02: Double,
        stack03: Double,
        stack04: Double,
        stack05: Double,
        stack06: Double,
        stack07: Double,
        stack08: Double,
        stack09: Double,
        epsilon: Double,
        decimalPlaces: Double,
        numberFormat: NumberFormat
    )
}

@Database(entities = arrayOf(ZuperTable::class),
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
    val zuper: Flow<List<ZuperTable>> = calcDao.getZuper()

    @WorkerThread
    suspend fun insertZuper(
        epoch: Int,
        pad: String,
        depth: Int,
        stack00: Double,
        stack01: Double,
        stack02: Double,
        stack03: Double,
        stack04: Double,
        stack05: Double,
        stack06: Double,
        stack07: Double,
        stack08: Double,
        stack09: Double,
        epsilon: Double,
        decimalPlaces: Double,
        numberFormat: NumberFormat
    ) {
        calcDao.insertZuper(
            epoch,
            pad,
            depth,
            stack00,
            stack01,
            stack02,
            stack03,
            stack04,
            stack05,
            stack06,
            stack07,
            stack08,
            stack09,
            epsilon,
            decimalPlaces,
            numberFormat
        )
    }

    @WorkerThread
    suspend fun updateZuper(
        epoch: Int,
        pad: String,
        depth: Int,
        stack00: Double,
        stack01: Double,
        stack02: Double,
        stack03: Double,
        stack04: Double,
        stack05: Double,
        stack06: Double,
        stack07: Double,
        stack08: Double,
        stack09: Double,
        epsilon: Double,
        decimalPlaces: Double,
        numberFormat: NumberFormat
    ) {
        calcDao.updateZuper(
            epoch,
            pad,
            depth,
            stack00,
            stack01,
            stack02,
            stack03,
            stack04,
            stack05,
            stack06,
            stack07,
            stack08,
            stack09,
            epsilon,
            decimalPlaces,
            numberFormat
        )
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
    val zuper: LiveData<List<ZuperTable>> = repository.zuper.asLiveData()
    fun insertZuper(epoch: Int, pad: String, depth: Int, stack00: Double, stack01: Double, stack02: Double, stack03: Double, stack04: Double, stack05: Double,
                    stack06: Double, stack07: Double, stack08: Double, stack09: Double, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) =
        viewModelScope.launch {
            repository.insertZuper(epoch, pad, depth, stack00, stack01, stack02, stack03, stack04, stack05, stack06, stack07, stack08, stack09, epsilon, decimalPlaces, numberFormat)
        }
    fun updateZuper(epoch: Int, pad: String, depth: Int, stack00: Double, stack01: Double, stack02: Double, stack03: Double, stack04: Double, stack05: Double,
                    stack06: Double, stack07: Double, stack08: Double, stack09: Double, epsilon: Double, decimalPlaces: Double, numberFormat: NumberFormat) =
        viewModelScope.launch {
            repository.updateZuper(epoch, pad, depth, stack00, stack01, stack02, stack03, stack04, stack05, stack06, stack07, stack08, stack09, epsilon, decimalPlaces, numberFormat)
        }
}

/*
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
*/