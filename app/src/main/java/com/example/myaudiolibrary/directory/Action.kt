package com.example.myaudiolibrary.directory

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myaudiolibrary.R
import com.primex.core.Text
import com.primex.core.stringResource

@Stable
sealed class Action(val id: String, val title: Text, val icon: ImageVector) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}


@Stable
sealed class GroupBy(id: String, title: Text, icon: ImageVector) : Action(id, title, icon) {

    private constructor(id: String, @StringRes title: Int, icon: ImageVector):this(id, Text(title), icon)
    object None : GroupBy(ORDER_BY_NONE, R.string.none, Icons.Outlined.FilterNone)
    object Name : GroupBy(ORDER_BY_NAME, R.string.name, Icons.Outlined.Title)
    object DateModified :
        GroupBy(ORDER_BY_DATE_MODIFIED, R.string.date_modified, Icons.Outlined.AccessTime)

    object DateAdded : GroupBy(ORDER_BY_DATE_ADDED, R.string.date_added, Icons.Outlined.CalendarMonth)
    object Length : GroupBy(ORDER_BY_LENGTH, R.string.length, Icons.Outlined.AvTimer)

    companion object {
        private const val ORDER_BY_NONE = "order_by_none"
        private const val ORDER_BY_NAME = "order_by_name"
        private const val ORDER_BY_DATE_MODIFIED = "order_by_date_modified"
        private const val ORDER_BY_DATE_ADDED = "order_by_date_added"
        private const val ORDER_BY_LENGTH = "order_by_length"

        fun map(id: String): GroupBy {
            return when (id) {
                ORDER_BY_NONE -> None
                ORDER_BY_DATE_ADDED -> DateAdded
                ORDER_BY_DATE_MODIFIED -> DateModified
                ORDER_BY_LENGTH -> Length
                ORDER_BY_NAME -> Name
                else -> error("No such GroupBy algo. $id")
            }
        }
    }
}

@Stable
sealed class ViewType(id: String, title: Text, icon: ImageVector) : Action(id, title, icon) {
    private constructor(id: String, @StringRes title: Int, icon: ImageVector):this(id, Text(title), icon)
    object List : ViewType(VIEW_TYPE_LIST, R.string.list, Icons.Outlined.List)
    object Grid : ViewType(VIEW_TYPE_GRID, R.string.grid, Icons.Outlined.GridView)

    companion object {
        val VIEW_TYPE_LIST = "view_type_list"
        val VIEW_TYPE_GRID = "view_type_grid"

        fun map(id: String): ViewType {
            return when (id) {
                VIEW_TYPE_LIST -> List
                VIEW_TYPE_GRID -> Grid
                else -> error("No such view type id: $id")
            }
        }
    }
}