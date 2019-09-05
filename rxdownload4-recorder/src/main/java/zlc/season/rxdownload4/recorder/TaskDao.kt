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

    @Delete
    fun delete(taskEntity: TaskEntity)

    @Query("SELECT * FROM task_record")
    fun getAll(): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status = :status")
    fun getAllWithStatus(status: Status): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record LIMIT :size OFFSET :start")
    fun page(start: Int, size: Int): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status=:status LIMIT :size OFFSET :start")
    fun pageWithStatus(start: Int, size: Int, status: Status): Maybe<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE id = :id")
    fun get(id: Int): Maybe<TaskEntity>
}