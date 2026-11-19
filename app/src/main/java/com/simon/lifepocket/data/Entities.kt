package com.simon.lifepocket.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 数据层实体。全部只存本地（Room / SQLite），无任何网络同步。
 */

/** 记账大类：吃 / 穿 / 住 / 行 / 学习，可自定义增删 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val sort: Int = 0,
)

/** 大类下的自定义小类（午饭 / 奶茶…），仅用于快捷选择，账目上直接存名字 */
@Entity(
    tableName = "sub_categories",
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "name"], unique = true),
    ],
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class SubCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
)

/** 一笔账：type 0=支出 1=收入；金额单位「分」，避免浮点误差 */
@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    val amountCents: Long,
    val categoryId: Long,
    val subName: String,
    val note: String,
    val dateEpochDay: Long,
    val createdAt: Long,
)

/** 任务（分组）：如「装修布置新家」 */
@Entity(tableName = "task_groups")
data class TaskGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** 任务下的子项 */
@Entity(
    tableName = "task_items",
    indices = [Index("groupId")],
    foreignKeys = [ForeignKey(
        entity = TaskGroup::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val title: String,
    val done: Boolean = false,
    val sort: Long,
    val createdAt: Long,
)

/** 用户精选的照片（只存系统选图器返回的 URI，不复制不外传） */
@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val addedAt: Long,
)

/** 统计聚合行：某大类的月度合计 */
data class CategorySum(
    val categoryId: Long,
    val total: Long,
)

/** 任务卡片（分组 + 子项 + 进度） */
data class TaskGroupWithItems(
    val group: TaskGroup,
    val items: List<TaskItem>,
) {
    val doneCount: Int get() = items.count { it.done }
    val totalCount: Int get() = items.size
}
