package org.krripe.scobbleboosters.booster.effect

object BoosterEffects {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        ShinyBooster.register()
        AlphaBooster.register()
        CatchBooster.register()
        ExperienceBooster.register()
        QualityBooster.register()
        BucketBooster.register()
        TypeBooster.register()
        BaitBooster.register()
        SnackBooster.register()
    }
}