package zlc.season.rxdownload4.database

import android.arch.persistence.room.*
import io.reactivex.Flowable

interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(taskEntity: TaskEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(taskEntity: TaskEntity)

    @Delete
    fun delete(taskEntity: TaskEntity)

    @Query("SELECT * FROM task_record")
    fun getAll(): Flowable<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE id = :id")
    fun get(id: Int): TaskEntity
}