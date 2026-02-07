import io.kotest.core.config.AbstractProjectConfig

object KotestProjectConfig : AbstractProjectConfig() {
    override val coroutineTestScope = true
}
