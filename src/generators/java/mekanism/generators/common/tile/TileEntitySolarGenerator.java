package mekanism.generators.common.tile;

import javax.annotation.Nonnull;
import mekanism.api.TileNetworkList;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.GeneratorsBlock;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.RainType;

public class TileEntitySolarGenerator extends TileEntityGenerator {

    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getSeesSun"};

    private boolean seesSun;
    private boolean needsRainCheck = true;
    private double peakOutput;

    public TileEntitySolarGenerator() {
        this(GeneratorsBlock.SOLAR_GENERATOR, MekanismGeneratorsConfig.generators.solarGeneration.get() * 2);
    }

    public TileEntitySolarGenerator(IBlockProvider blockProvider, double output) {
        super(blockProvider, output);
    }

    public boolean canSeeSun() {
        return seesSun;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull Direction side) {
        return new int[]{0};
    }

    @Override
    public void validate() {
        super.validate();
        Biome b = world.getDimension().getBiome(getPos());

        // Consider the best temperature to be 0.8; biomes that are higher than that
        // will suffer an efficiency loss (semiconductors don't like heat); biomes that are cooler
        // get a boost. We scale the efficiency to around 30% so that it doesn't totally dominate
        float tempEff = 0.3f * (0.8f - b.getTemperature(getPos()));

        // Treat rainfall as a proxy for humidity; any humidity works as a drag on overall efficiency.
        // As with temperature, we scale it so that it doesn't overwhelm production. Note the signedness
        // on the scaling factor. Also note that we only use rainfall as a proxy if it CAN rain; some dimensions
        // (like the End) have rainfall set, but can't actually support rain.
        float humidityEff = -0.3f * (b.getPrecipitation() != RainType.NONE ? b.getDownfall() : 0.0f);

        peakOutput = getConfiguredMax() * (1.0f + tempEff + humidityEff);
        needsRainCheck = b.getPrecipitation() != RainType.NONE;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            ChargeUtils.charge(0, this);
            // Sort out if the generator can see the sun; we no longer check if it's raining here,
            // since under the new rules, we can still generate power when it's raining, albeit at a
            // significant penalty.
            seesSun = world.isDaytime() && canSeeSky() && !world.getDimension().isNether();

            if (canOperate()) {
                setActive(true);
                setEnergy(getEnergy() + getProduction());
            } else {
                setActive(false);
            }
        }
    }

    protected boolean canSeeSky() {
        return world.canBlockSeeSky(getPos());
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull Direction side) {
        if (slotID == 0) {
            return ChargeUtils.canBeOutputted(itemstack, true);
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return ChargeUtils.canBeCharged(itemstack);
        }
        return true;
    }

    @Override
    public boolean canOperate() {
        return getEnergy() < getMaxEnergy() && seesSun && MekanismUtils.canFunction(this);
    }

    public double getProduction() {
        // Get the brightness of the sun; note that there are some implementations that depend on the base
        // brightness function which doesn't take into account the fact that rain can't occur in some biomes.
        float brightness = world.getSunBrightness(1.0f);
        //TODO: Galacticraft
        /*if (MekanismUtils.existsAndInstance(world.provider, "micdoodle8.mods.galacticraft.api.world.ISolarLevel")) {
            brightness *= ((ISolarLevel) world.provider).getSolarEnergyMultiplier();
        }*/

        // Production is a function of the peak possible output in this biome and sun's current brightness
        double production = peakOutput * brightness;

        // If the generator is in a biome where it can rain and it's raining penalize production by 80%
        if (needsRainCheck && (world.isRaining() || world.isThundering())) {
            production *= 0.2;
        }
        return production;
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        switch (method) {
            case 0:
                return new Object[]{getEnergy()};
            case 1:
                return new Object[]{output};
            case 2:
                return new Object[]{getBaseStorage()};
            case 3:
                return new Object[]{getBaseStorage() - getEnergy()};
            case 4:
                return new Object[]{seesSun};
            default:
                throw new NoSuchMethodException();
        }
    }

    @Override
    public void handlePacketData(PacketBuffer dataStream) {
        super.handlePacketData(dataStream);
        if (world.isRemote) {
            seesSun = dataStream.readBoolean();
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(seesSun);
        return data;
    }

    @Override
    public boolean canOutputEnergy(Direction side) {
        return side == Direction.DOWN;
    }

    protected double getConfiguredMax() {
        return MekanismGeneratorsConfig.generators.solarGeneration.get();
    }

    @Override
    public double getMaxOutput() {
        return peakOutput;
    }
}