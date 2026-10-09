package mod.gottsch.neo.gmm.core.entity.monster.ghoul;

import java.util.OptionalInt;

/**
 * @author by Mark Gottschling on 11/6/2025
 */
public interface IGhoul {

    public int getOnFireDuration();
    public boolean burnsInSun();
    public OptionalInt getFoodInventory();
    public float getHealAmount();

    public boolean canOpenDoors();
    public void setCanOpenDoors(boolean canOpenDoors);
}
