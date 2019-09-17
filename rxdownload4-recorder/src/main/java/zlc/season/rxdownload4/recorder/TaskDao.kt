package zlc.season.rxdownload4.recorder

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.IGNORE
import io.reactivex.Maybe
import zlc.season.rxdownload4.manager.Status

@Dao
interface TaskDao {
    @Insert(onConflict = IGNORE)
    fun insert(taskEntity: TaskEntity)

    @Update
    fun update(taskEntity: TaskEntity)

    @Update
    fun update(list: List<TaskEntity>)

    @Delete
    fun delete(taskEntity: TaskEntity)

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
}