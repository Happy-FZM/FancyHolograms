package de.oliver.fancyholograms.commands.hologram;

import com.google.common.primitives.Ints;
import de.oliver.fancyholograms.FancyHolograms;
import de.oliver.fancyholograms.api.Hologram;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.events.HologramUpdateEvent;
import de.oliver.fancyholograms.commands.HologramCMD;
import de.oliver.fancyholograms.commands.Subcommand;
import de.oliver.fancylib.MessageHelper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LineWidthCMD implements Subcommand {

    @Override
    public List<String> tabcompletion(@NotNull Player player, @Nullable Hologram hologram, @NotNull String[] args) {
        return null;
    }

    @Override
    public boolean run(@NotNull Player player, @Nullable Hologram hologram, @NotNull String[] args) {
        if (!(hologram.getData().getTypeData() instanceof TextHologramData textData)) {
            MessageHelper.error(player, "This command can only be used on text holograms");
            return false;
        }

        var lineWidth = Ints.tryParse(args[3]);

        if (lineWidth == null) {
            MessageHelper.error(player, "Could not parse line width distance");
            return false;
        }

        if (lineWidth <= 0) {
            lineWidth = Hologram.LINE_WIDTH;
        }

        if (Ints.compare(lineWidth, textData.getLineWidth()) == 0) {
            MessageHelper.warning(player, "This hologram already has this line width");
            return false;
        }

        final var copied = hologram.getData().copy();
        ((TextHologramData) copied.getTypeData()).setLineWidth(lineWidth);

        if (!HologramCMD.callModificationEvent(hologram, player, copied, HologramUpdateEvent.HologramModification.LINE_WIDTH)) {
            return false;
        }

        if (Ints.compare(((TextHologramData) copied.getTypeData()).getLineWidth(), textData.getLineWidth()) == 0) {
            MessageHelper.warning(player, "This hologram already has this line width");
            return false;
        }

        textData.setLineWidth(((TextHologramData) copied.getTypeData()).getLineWidth());

        if (FancyHolograms.get().getHologramConfiguration().isSaveOnChangedEnabled()) {
            FancyHolograms.get().getHologramStorage().save(hologram);
        }

        MessageHelper.success(player, "Changed line width");
        return true;

    }
}
