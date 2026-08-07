package cx.viz.slovo.platform

interface Haptics {
    fun light()
    fun success()
    fun error()
}

class NoopHaptics : Haptics {
    override fun light() { /* no-op */ }
    override fun success() { /* no-op */ }
    override fun error() { /* no-op */ }
}
