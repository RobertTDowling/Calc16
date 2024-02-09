package com.rtdti.calc16

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@Dao
interface CalcDao {

    @Query("SELECT pad from PadTable")
    fun getPad(): Flow<String>

    @Query("DELETE from PadTable")
    fun clearPad()

    @Insert
    suspend fun setPad(padTable: PadTable)

    @Query("SELECT max(epoch) FROM StackTable")
    fun getLastEpoch(): Flow<Int>

    @Query("SELECT * from StackTable where epoch = :epoch")
    fun getStackAtEpoch(epoch: Int) : Flow<List<StackTable>>

    @Query("SELECT * from StackTable where epoch = (SELECT max(epoch) FROM StackTable)")
    fun getStackAtLastEpoch() : Flow<List<StackTable>>

    @Insert
    suspend fun insertStack(stackTable: StackTable)

    @Query("DELETE FROM StackTable where epoch = :epoch")
    fun rollbackStack(epoch: Int)
}

@Database(entities = arrayOf(PadTable::class, StackTable::class),
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
                    .build()
                Instance = instance
                // return instance
                instance
            }
        }
    }
}

interface CalcRepository {
    fun getPad(): Flow<String?>
    suspend fun clearPad()
    suspend fun setPad(pad: String)
    fun getLastEpoch(): Flow<Int>
    fun getStackAtEpoch(epoch: Int) : Flow<List<StackTable>>
    fun getStackAtLastEpoch() : Flow<List<StackTable>>
    suspend fun insertStack(stackTable: StackTable)
    fun rollbackStack(epoch: Int)
}

class OfflineCalcRepository(private val calcDao: CalcDao) : CalcRepository {
    override fun getPad(): Flow<String> = calcDao.getPad()
    override suspend fun clearPad() = calcDao.clearPad()
    override suspend fun setPad(pad: String) = calcDao.setPad(PadTable(0, pad))
    override fun getLastEpoch(): Flow<Int> = calcDao.getLastEpoch()
    override fun getStackAtEpoch(epoch: Int) = calcDao.getStackAtEpoch(epoch)
    override fun getStackAtLastEpoch() = calcDao.getStackAtLastEpoch()
    override suspend fun insertStack(stackTable: StackTable) = calcDao.insertStack(stackTable)
    override fun rollbackStack(epoch: Int) = calcDao.rollbackStack(epoch)
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

data class PadState(val pad: String)
data class StackState(val stack: List<Double>)

fun PadState.isEmpty() : Boolean = pad.isEmpty()

class WorkingStack(stackState: StackState) {
    val stack: MutableList<Double> = stackState.stack.toMutableList()
    fun hasDepth(depth: Int) = stack.size >= depth
    fun isEmpty() = !hasDepth(1)
    fun push(x: Double) = stack.add(0, x)
    fun pop(): Double = stack.removeAt(0)
    fun pick(depth: Int) = push(stack[depth])
}

class CalcViewModel(private val repository: CalcRepository) : ViewModel() {
    val debugString = mutableStateOf("")
    //////
    // Format Parameters
    //////
    var formatParameters = FormatParameters()
    fun formatSet(numberFormat: NumberFormat) { formatParameters.numberFormat.value = numberFormat }
    fun formatGet() : NumberFormat { return formatParameters.numberFormat.value }
    fun formatter(): StackFormatter { return formatParameters.numberFormat.value.formatter() }
    ////////
    /// Pad
    ////////
    val padState = repository.getPad().map { PadState(it ?: "") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500L),
            initialValue = PadState("")
        )

    fun padIsEmpty() = padState.value.isEmpty()
    fun padAppend(char: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.clearPad()
            repository.setPad(padState.value.pad.plus(char))
        }
    }

    fun padBackspace() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            if (!padIsEmpty()) {
                repository.clearPad()
                repository.setPad(padState.value.pad.substring(0, padState.value.pad.length - 1))
            }
        }
    }

    ////////
    /// Stack
    ////////
    val stackLastEpoch = repository.getLastEpoch()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    fun stackStateFromStackTableList(stl: List<StackTable>): StackState? {
        // Quick out if list is empty
        if (stl.isEmpty()) {
            return StackState(listOf())
        }
        val sortedStl = stl.sortedBy { it.depth }
        // Sanity check
        if (sortedStl.first().depth > 0 || sortedStl.last().depth != sortedStl.size - 1) { // we are in trouble
            // FIXME some how signal no state to update
            // debugString.value = "Invalid stack in DB"
            // return StackState(listOf())
            return null
        }
        return StackState(sortedStl.map { it.value })
    }

    val stackState = repository.getStackAtLastEpoch().mapNotNull { stackStateFromStackTableList(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StackState(listOf())
        )

    fun stackDepth() : Int = stackState.value.stack.size
    fun stackRollBack() : Boolean {
        val epoch = stackLastEpoch.value - 1
        if (epoch > 0) { // FIXME
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.rollbackStack(epoch)
                }
            }
            return false
        } else {
            return true
        }
    }
    private suspend fun backupStack(workingStack: WorkingStack) {
        val epoch = stackLastEpoch.value
        debugString.value = "epoch " + epoch.toString()
        for ((depth, ss) in workingStack.stack.withIndex()) {
            repository.insertStack(StackTable(0, epoch+1, depth, ss))
        }
    }

    private fun impliedEnter() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            // Copy pad to top of stack and clear pad
            if (padIsEmpty()) {
                // Do nothing?
            } else {
                val x = try {
                    //if (formatGet() == NumberFormat.HEX) {
                    //    padState.value.pad.toLong(radix = 16).toDouble()
                    //} else {
                    padState.value.pad.toDouble()
                    //}
                } catch (e: Exception) {
                    0.0
                }
                val workingStack = WorkingStack(stackState.value)
                workingStack.push(x)
                repository.clearPad()
                backupStack(workingStack)
            }
        }
    }

    fun pushConstant(x: Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            workingStack.push(x)
            backupStack(workingStack)
        }
    }

    fun swap() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            if (workingStack.hasDepth(2)) {
                val b = workingStack.pop()
                val a = workingStack.pop()
                workingStack.push(b)
                workingStack.push(a)
                backupStack(workingStack)
            }
        }
    }

    fun pick(index: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            workingStack.pick(index)
            backupStack(workingStack)
        }
    }
    fun binop(op: (Double, Double) -> Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            if (workingStack.hasDepth(2)) {
                val b = workingStack.pop()
                val a = workingStack.pop()
                workingStack.push(op(a, b))
                backupStack(workingStack)
            }
        }
    }

    fun unop(op: (Double) -> Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            if (workingStack.hasDepth(2)) {
                val a = workingStack.pop()
                workingStack.push(op(a))
                backupStack(workingStack)
            }
        }
    }

    fun pop1op(op: (Double) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            impliedEnter().join()
            val workingStack = WorkingStack(stackState.value)
            if (workingStack.hasDepth(2)) {
                val a = workingStack.pop()
                backupStack(workingStack)
            }
        }
    }

    /////////
    // Pad + Stack
    /////////
    fun backspaceOrDrop() { // Combo backspace and drop
        if (padIsEmpty()) {
            if (!stackState.value.stack.isEmpty()) {
                pop1op({ d -> })
            }
        } else {
            padBackspace()
        }
    }

    fun enterOrDup() { // Combo enter and dup
        if (!padIsEmpty()) {
            impliedEnter()
        } else {
            if (!stackState.value.stack.isEmpty()) {
                val a = stackState.value.stack.last();
                pushConstant(a)
            }
        }
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

/*
class CalcViewModelFactory(private val repository: OfflineCalcRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalcViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalcViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CalcViewModel(private val repository: OfflineCalcRepository) : ViewModel() {
    val zuper: LiveData<List<Zuper>> = repository.zuper.asLiveData()
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

*/

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