package zlc.season.rxdownload4.recorder

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import io.reactivex.Maybe
import zlc.season.rxdownload4.manager.Status

@Dao
interface TaskDao {
    @Insert(onConflict = IGNORE)
    fun insert(taskEntity: TaskEntity): Maybe<Long>

    @Update
    fun update(taskEntity: TaskEntity): Maybe<Int>

    @Update
    fun update(list: List<TaskEntity>): Maybe<Int>

    @Delete
    fun delete(taskEntity: TaskEntity): Maybe<Int>

    @Query("SELECT * FROM task_record")
    fun getAll(): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status IN(:status)")
    fun getAllWithStatus(vararg status: Status): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record LIMIT :size OFFSET :start")
    fun page(start: Int, size: Int): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status IN(:status) LIMIT :size OFFSET :start")
    fun pageWithStatus(start: Int, size: Int, vararg status: Status): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE id = :id")
    fun get(id: Int): Maybe<TaskEntity>

    @Query("SELECT * FROM task_record WHERE id IN(:id)")
    fun get(vararg id: Int): Maybe<List<TaskEntity>>

    @Query("UPDATE task_record SET extraInfo = :extraInfo WHERE id = :id")
    fun update(id: Int, extraInfo: String): Maybe<Int>
}