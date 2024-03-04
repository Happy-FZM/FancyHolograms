package de.oliver.fancyholograms;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.oliver.fancyholograms.api.*;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.commands.FancyHologramsCMD;
import de.oliver.fancyholograms.commands.FancyHologramsTestCMD;
import de.oliver.fancyholograms.commands.HologramCMD;
import de.oliver.fancyholograms.listeners.PlayerListener;
import de.oliver.fancyholograms.storage.FlatFileHologramStorage;
import de.oliver.fancyholograms.version.Hologram1_19_4;
import de.oliver.fancyholograms.version.Hologram1_20_1;
import de.oliver.fancyholograms.version.Hologram1_20_2;
import de.oliver.fancyholograms.version.Hologram1_20_4;
import de.oliver.fancylib.serverSoftware.ServerSoftware;
import de.oliver.fancylib.serverSoftware.schedulers.BukkitScheduler;
import de.oliver.fancylib.serverSoftware.schedulers.FancyScheduler;
import de.oliver.fancylib.serverSoftware.schedulers.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public final class FancyHolograms extends JavaPlugin implements FancyHologramsPlugin {

    public static final String[] SUPPORTED_VERSIONS = {"1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"};


    @Nullable
    private static FancyHolograms INSTANCE;
    private final FancyScheduler scheduler = ServerSoftware.isFolia() ? new FoliaScheduler(this) : new BukkitScheduler(this);
    private final Collection<Command> commands = Arrays.asList(new HologramCMD(this), new FancyHologramsCMD(this));
    private HologramConfiguration configuration = new FancyHologramsConfiguration();
    private HologramStorage hologramStorage = new FlatFileHologramStorage();
    @Nullable
    private HologramManagerImpl hologramsManager;
//    private boolean isUsingViaVersion;

    private final ExecutorService fileStorageExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setPriority(Thread.MIN_PRIORITY + 1)
                    .setNameFormat("FancyHolograms-FileStorageExecutor")
                    .build()
    );

    public static @NotNull FancyHolograms get() {
        return Objects.requireNonNull(INSTANCE, "plugin is not initialized");
    }

    @Override
    public JavaPlugin getPlugin() {
        return INSTANCE;
    }

    @Override
    public void onLoad() {
        INSTANCE = this;

        final var adapter = resolveHologramAdapter();

        if (adapter == null) {
            getLogger().warning("""
                                                    
                    --------------------------------------------------
                    Unsupported minecraft server version.
                    Please update the server to one of (%s).
                    Disabling the FancyHolograms plugin.
                    --------------------------------------------------
                    """.formatted(String.join(" / ", SUPPORTED_VERSIONS)));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        hologramsManager = new HologramManagerImpl(this, adapter);
    }

    @Override
    public void onEnable() {
        getHologramConfiguration().reload(this); // initialize configuration

        reloadCommands();

        registerListeners();

        getHologramsManager().initializeTasks();

//        isUsingViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;

        if (getHologramConfiguration().isAutosaveEnabled()) {
            getScheduler().runTaskTimerAsynchronously(getHologramConfiguration().getAutosaveInterval() * 20L, 20L * 60L * getHologramConfiguration().getAutosaveInterval(), hologramsManager::saveHolograms);
        }
    }

    @Override
    public void onDisable() {
        hologramsManager.saveHolograms();
        fileStorageExecutor.shutdown();
        INSTANCE = null;
    }

//    @Override
//    public boolean isUsingViaVersion() {
//        return isUsingViaVersion;
//    }

    public @NotNull FancyScheduler getScheduler() {
        return this.scheduler;
    }

    @ApiStatus.Internal
    public @NotNull HologramManagerImpl getHologramsManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramManager getHologramManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramConfiguration getHologramConfiguration() {
        return configuration;
    }

    @Override
    public void setHologramConfiguration(HologramConfiguration configuration, boolean reload) {
        this.configuration = configuration;

        if (reload) {
            configuration.reload(this);
            reloadCommands();
        }
    }

    @Override
    public HologramStorage getHologramStorage() {
        return hologramStorage;
    }

    @Override
    public void setHologramStorage(HologramStorage storage, boolean reload) {
        this.hologramStorage = storage;

        if (reload) {
            getHologramsManager().reloadHolograms();
        }
    }

    public ExecutorService getFileStorageExecutor() {
        return this.fileStorageExecutor;
    }

    private @Nullable Function<HologramData, Hologram> resolveHologramAdapter() {
        final var version = Bukkit.getMinecraftVersion();

        return switch (version) {
            case "1.20.3", "1.20.4" -> Hologram1_20_4::new;
            case "1.20.2" -> Hologram1_20_2::new;
            case "1.20", "1.20.1" -> Hologram1_20_1::new;
            case "1.19.4" -> Hologram1_19_4::new;
            default -> null;
        };
    }

    public void reloadCommands() {
        if (getHologramConfiguration().isRegisterCommands()) {
            commands.forEach(command -> getServer().getCommandMap().register("fancyholograms", command));
        } else {
            commands.stream().filter(Command::isRegistered).forEach(command ->
                    command.unregister(getServer().getCommandMap()));
        }

        if (false) {
            FancyHologramsTestCMD fancyHologramsTestCMD = new FancyHologramsTestCMD(this);
            getServer().getCommandMap().register("fancyholograms", fancyHologramsTestCMD);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

}
