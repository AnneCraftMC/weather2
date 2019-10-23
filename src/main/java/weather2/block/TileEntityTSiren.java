package weather2.block;

import CoroUtil.util.CoroUtilPhysics;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import weather2.ClientTickHandler;
import weather2.CommonProxy;
import weather2.config.ConfigMisc;
import weather2.config.ConfigSand;
import weather2.util.WeatherUtilSound;
import weather2.weathersystem.storm.StormObject;
import CoroUtil.util.Vec3;
import weather2.weathersystem.storm.WeatherObjectSandstorm;

import java.util.List;

public class TileEntityTSiren extends TileEntity implements ITickableTileEntity
{
    public long lastPlayTime = 0L;

    @Override
    public void tick()
    {
    	if (world.isRemote) {
    	    int meta = CommonProxy.blockTSiren.getMetaFromState(this.world.getBlockState(this.getPos()));
            if (BlockTSiren.isEnabled(meta)) {
                //System.out.println("enabled");
                tickClient();
            }

    	}
    }
    
    @OnlyIn(Dist.CLIENT)
    public void tickClient() {
    	
    	if (this.lastPlayTime < System.currentTimeMillis())
        {
            Vec3 pos = new Vec3(getPos().getX(), getPos().getY(), getPos().getZ());

    		StormObject so = ClientTickHandler.weatherManager.getClosestStorm(pos, ConfigMisc.sirenActivateDistance, StormObject.STATE_FORMING);

            if (so != null)
            {
                this.lastPlayTime = System.currentTimeMillis() + 13000L;
                WeatherUtilSound.playNonMovingSound(pos, "streaming.siren", 1.0F, 1.0F, 120);
            } else {
                if (!ConfigSand.Sandstorm_Siren_PleaseNoDarude) {
                    WeatherObjectSandstorm sandstorm = ClientTickHandler.weatherManager.getClosestSandstormByIntensity(pos);

                    if (sandstorm != null) {
                        List<Vec3> field_75884_a = sandstorm.getSandstormAsShape();

                        float distMax = 75F;

                        //double scale = sandstorm.getSandstormScale();
                        boolean inStorm = CoroUtilPhysics.isInConvexShape(pos, field_75884_a);
                        double dist = Math.min(distMax, CoroUtilPhysics.getDistanceToShape(pos, field_75884_a));

                        if (inStorm || dist < distMax) {
                            String soundToPlay = "siren_sandstorm_5_extra";
                            if (getWorld().rand.nextBoolean()) {
                                soundToPlay = "siren_sandstorm_6_extra";
                            }

                            float distScale = Math.max(0.1F, 1F - (float) ((dist) / distMax));
                            if (inStorm) distScale = 1F;

                            this.lastPlayTime = System.currentTimeMillis() + 15000L;//WeatherUtilSound.soundToLength.get(soundToPlay) - 500L;
                            WeatherUtilSound.playNonMovingSound(pos, "streaming." + soundToPlay, 1F, distScale, distMax);
                        }
                    }
                }
            }
        }
    }
    
}
