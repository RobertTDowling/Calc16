package com.rtdti.calc16

import android.app.Application
import android.content.Context
import android.util.Log
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
import androidx.room.Transaction
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

    // @Update // Won't update unless table has a matching entry
    // suspend fun updatePad(padTable: PadTable)
    @Query("INSERT OR REPLACE INTO PadTable (rowid, pad) values (:rowid, :pad)")
    suspend fun insertOrUpdatePad(rowid: Int, pad: String)
    // @Query("INSERT OR REPLACE INTO PadTable (rowid, pad) values (:padTable)")
    // suspend fun updatePad(padTable: PadTable) // does not compile
    // @Upsert // Appends, never updates
    // suspend fun updatePad(padTable: PadTable)

    @Query("SELECT * from StackTable")
    fun getStack() : Flow<List<StackTable>>

    @Insert
    suspend fun insertStack(stackTable: StackTable)

    @Query("delete from StackTable")
    suspend fun clearStack()

    @Query("DELETE FROM StackTable where epoch = :epoch")
    fun rollbackStack(epoch: Int)

    @Transaction
    suspend fun insertFullStackClearPad(listStackTable: List<StackTable>) {
        insertOrUpdatePad(0,"")
        for (st in listStackTable) {
            insertStack(st)
        }
    }

    @Transaction
    suspend fun insertFullStack(listStackTable: List<StackTable>) {
        for (st in listStackTable) {
            insertStack(st)
        }
    }
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
    suspend fun insertOrUpdatePad(pad: String)
    fun getStack() : Flow<List<StackTable>>
    suspend fun clearStack()
    suspend fun insertStack(stackTable: StackTable)
    fun rollbackStack(epoch: Int)
    suspend fun insertFullStack(lst: List<StackTable>)
    suspend fun insertFullStackClearPad(lst: List<StackTable>)
}

class OfflineCalcRepository(private val calcDao: CalcDao) : CalcRepository {
    override fun getPad(): Flow<String> = calcDao.getPad()
    override suspend fun insertOrUpdatePad(pad: String) = calcDao.insertOrUpdatePad(0, pad)
    override fun getStack() = calcDao.getStack()
    override suspend fun clearStack() = calcDao.clearStack()
    override suspend fun insertStack(stackTable: StackTable) = calcDao.insertStack(stackTable)
    override fun rollbackStack(epoch: Int) = calcDao.rollbackStack(epoch)
    override suspend fun insertFullStack(lst: List<StackTable>) = calcDao.insertFullStack(lst)
    override suspend fun insertFullStackClearPad(lst: List<StackTable>) = calcDao.insertFullStackClearPad(lst)
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
    fun asListStackTable(epoch: Int) : List<StackTable> {
        if (stack.isEmpty()) {
            return listOf(StackTable(0, epoch, -1, 0.0))
        }
        return stack.mapIndexed { depth, value -> StackTable(0, epoch, depth, value)}
    }
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
            repository.insertOrUpdatePad(padState.value.pad.plus(char))
        }
    }

    fun padBackspace() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            if (!padIsEmpty()) {
                repository.insertOrUpdatePad(padState.value.pad.substring(0, padState.value.pad.length - 1))
            }
        }
    }

    ////////
    /// Stack
    ////////
    val stackFirstEpoch = mutableStateOf(0)
    val stackLastEpoch = mutableStateOf(0)
    fun stackStateFromStackTableList(stl: List<StackTable>): StackState? {
        // Quick out if list is empty
        if (stl.isEmpty()) {
            debugString.value = String.format("E: Empty Flow")
            return null
        }
        val sortedStl = stl.sortedBy { it.depth }// .sortedWith(compareBy<StackTable>{ it.epoch }.thenBy{ it.depth })
        val firstEpoch = sortedStl.minOf { it.epoch }
        val lastEpoch = sortedStl.maxOf { it.epoch }
        // Filter for only lastEpoch
        val filteredStl = sortedStl.filter { it.epoch == lastEpoch }.filter { it.depth >= 0 }
        // Sanity check
        if (!filteredStl.isEmpty() && (filteredStl.first().depth > 0 || filteredStl.last().depth != filteredStl.size - 1)) {
            // we are in trouble
            // FIXME some how signal no state to update
            // Log.i("RoomPersistence", "Invalid stack in DB, missing entries")
            debugString.value = String.format("E: %d..%d Invalid", firstEpoch, lastEpoch, filteredStl.size)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.clearStack()
                }
            }
            return null
        }
        stackLastEpoch.value = lastEpoch // FIXME: Seems this should be bundled into StackState
        debugString.value = String.format("E: %d..%d D: %d", firstEpoch, lastEpoch, filteredStl.size)
        return StackState(filteredStl.map { it.value })
    }

    val stackState = repository.getStack().mapNotNull { stackStateFromStackTableList(it) }
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
        val epoch = stackLastEpoch.value + 1
        repository.insertFullStack(workingStack.asListStackTable(epoch))
    }

    private suspend fun doEnter(): WorkingStack {
        // Copy pad to top of stack and clear pad
        val x = try {
            if (formatGet() == NumberFormat.HEX) {
                padState.value.pad.toLong(radix = 16).toDouble()
            } else {
                padState.value.pad.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
        val workingStack = WorkingStack(stackState.value)
        workingStack.push(x)
        // repository.insertOrUpdatePad("")
        // backupStack(workingStack)
        val epoch = stackLastEpoch.value + 1
        repository.insertFullStackClearPad(workingStack.asListStackTable(epoch))
        stackLastEpoch.value = epoch
        return workingStack
    }
    private suspend fun doImpliedEnter(): WorkingStack {
        // Only doEnter if pad isn't empty
        if (!padIsEmpty()) {
            return doEnter()
        }
        val workingStack = WorkingStack(stackState.value)
        return workingStack
    }

    private fun Enter() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            doEnter()
        }
    }

    fun pushConstant(x: Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            workingStack.push(x)
            backupStack(workingStack)
        }
    }

    fun swap() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
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
            val workingStack = doImpliedEnter()
            workingStack.pick(index)
            backupStack(workingStack)
        }
    }
    fun binop(op: (Double, Double) -> Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
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
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                workingStack.push(op(a))
                backupStack(workingStack)
            }
        }
    }

    fun pop1op(op: (Double) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                op(a)
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
            Enter()
        } else {
            if (!stackState.value.stack.isEmpty()) {
                val a = stackState.value.stack.first();
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