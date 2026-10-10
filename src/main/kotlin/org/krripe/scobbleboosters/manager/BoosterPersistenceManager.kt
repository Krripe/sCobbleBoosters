package org.krripe.scobbleboosters.manager

import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.krripe.scobbleboosters.SCobbleBoosters
import org.krripe.scobbleboosters.config.MainConfigManager
import org.krripe.scobbleboosters.data.persistence.BoosterSaveData
import org.krripe.scobbleboosters.data.repository.BoosterRepository
import org.krripe.scobbleboosters.data.repository.JsonBoosterRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object BoosterPersistenceManager {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sCobbleBoosters-Storage").apply { isDaemon = true }
    }

    @Volatile
    private var repository: BoosterRepository? = null
    private val asyncSaveQueued = AtomicBoolean(false)
    private var lastAutosaveMillis = 0L

    fun load(server: MinecraftServer) {
        val file = server.getSavePath(WorldSavePath.ROOT).resolve("scobbleboosters").resolve("data.json")
        val repo = JsonBoosterRepository(file)
        repository = repo

        val data = repo.load()
        val now = System.currentTimeMillis()

        GlobalBoosterManager.restore(data.global, now)
        PersonalBoosterManager.restore(data.personal, now)
        BoosterStorageManager.restore(data.storage)
        BoosterCoordinator.syncAllPersonalWithGlobal(now)

        lastAutosaveMillis = now

        SCobbleBoosters.LOGGER.info(
            "Loaded booster data: {} global types, {} storage players",
            data.global.size,
            data.storage.size
        )
    }

    fun tick() {
        val now = System.currentTimeMillis()
        val interval = MainConfigManager.config.autosaveSeconds.coerceAtLeast(30) * 1000L

        if (now - lastAutosaveMillis < interval) return

        lastAutosaveMillis = now
        saveAsync()
    }

    fun saveAsync() {
        val repo = repository ?: return

        if (!asyncSaveQueued.compareAndSet(false, true)) return

        val snapshot = createSnapshot()

        executor.execute {
            try {
                repo.save(snapshot)
            } finally {
                asyncSaveQueued.set(false)
            }
        }
    }

    fun saveBlocking() {
        val repo = repository ?: return
        val snapshot = createSnapshot()

        runCatching {
            executor.submit { repo.save(snapshot) }.get(10, TimeUnit.SECONDS)
        }.onFailure {
            SCobbleBoosters.LOGGER.error("Failed to save booster data during shutdown", it)
        }
    }

    fun reset() {
        repository = null
        lastAutosaveMillis = 0L
        asyncSaveQueued.set(false)
    }

    private fun createSnapshot(): BoosterSaveData {
        val now = System.currentTimeMillis()

        return BoosterSaveData(
            global = GlobalBoosterManager.snapshot(now),
            personal = PersonalBoosterManager.snapshot(now),
            storage = BoosterStorageManager.snapshot()
        )
    }
}