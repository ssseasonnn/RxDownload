package zlc.season.rxdownload4.database

import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import io.reactivex.Completable
import io.reactivex.Flowable
import zlc.season.rxdownload4.manager.Status

interface TaskDao {
    @Insert
    fun insert(taskEntity: TaskEntity): Completable

    @Update
    fun update(taskEntity: TaskEntity): Completable

    @Delete
    fun delete(taskEntity: TaskEntity): Completable

    @Query("SELECT * FROM task_record")
    fun getAll(): Flowable<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status = :status")
    fun getAllWithStatus(status: Status): Flowable<List<TaskEntity>>

    @Query("SELECT * FROM task_record LIMIT :size OFFSET :start")
    fun page(start: Int, size: Int): Flowable<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE status=:status LIMIT :size OFFSET :start")
    fun pageWithStatus(start: Int, size: Int, status: Status): Flowable<List<TaskEntity>>

    @Query("SELECT * FROM task_record WHERE id = :id")
    fun get(id: Int): Flowable<TaskEntity>
}