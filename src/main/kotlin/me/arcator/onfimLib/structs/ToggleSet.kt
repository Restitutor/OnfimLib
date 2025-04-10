package me.arcator.onfimLib.structs

import java.util.*

@Suppress("unused")
class ToggleSet {
    private val set = hashSetOf<UUID>()
    fun contains(v: UUID) = set.contains(v)

    fun toggle(v: UUID): Boolean {
        if (set.contains(v)) {
            set.remove(v)
        } else {
            set.add(v)
        }
        return set.contains(v)
    }
}
