package weather2.entity;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.api.distmarker.OnlyIn;
import weather2.config.ConfigMisc;
import weather2.config.ConfigStorm;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.Vec3;

public class EntityLightningBolt extends EntityWeatherEffect
{
    /**
     * Declares which state the lightning bolt is in. Whether it's in the air, hit the ground, etc.
     */
    private int lightningState;

    /**
     * A random long that is used to change the vertex of the lightning rendered in RenderLightningBolt
     */
    public long boltVertex;

    /**
     * Determines the time before the EntityLightningBolt is destroyed. It is a random integer decremented over time.
     */
    private int boltLivingTime;

    public int fireLifeTime = ConfigStorm.Lightning_lifetimeOfFire;
    public int fireChance = ConfigStorm.Lightning_OddsTo1OfFire;

    public EntityLightningBolt(World par1World) {
        super(par1World);
    }

    public EntityLightningBolt(World par1World, double par2, double par4, double par6)
    {
        super(par1World);
        this.setLocationAndAngles(par2, par4, par6, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltVertex = this.rand.nextLong();
        this.boltLivingTime = this.rand.nextInt(3) + 1;

        Random rand = new Random();
        
        
        if (ConfigStorm.Lightning_StartsFires) {
            if (!par1World.isRemote && par1World.getGameRules().getBoolean("doFireTick") && (par1World.getDifficulty() == Difficulty.NORMAL || par1World.getDifficulty() == Difficulty.HARD) && par1World.isAreaLoaded(new BlockPos(MathHelper.floor(par2), MathHelper.floor(par4), MathHelper.floor(par6)), 10)) {
                int i = MathHelper.floor(par2);
                int j = MathHelper.floor(par4);
                int k = MathHelper.floor(par6);

                if (CoroUtilBlock.isAir(par1World.getBlockState(new BlockPos(i, j, k)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(par1World, new BlockPos(i, j, k))) {
                    //par1World.setBlockState(new BlockPos(i, j, k), Blocks.fire, fireLifeTime, 3);
                    par1World.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().with(FireBlock.AGE, fireLifeTime));
                }

                for (i = 0; i < 4; ++i) {
                    j = MathHelper.floor(par2) + this.rand.nextInt(3) - 1;
                    k = MathHelper.floor(par4) + this.rand.nextInt(3) - 1;
                    int l = MathHelper.floor(par6) + this.rand.nextInt(3) - 1;

                    if (CoroUtilBlock.isAir(par1World.getBlockState(new BlockPos(j, k, l)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(par1World, new BlockPos(j, k, l))) {
                        //par1World.setBlockState(new BlockPos(j, k, l), Blocks.fire.getDefaultState(), fireLifeTime, 3);
                        par1World.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().with(FireBlock.AGE, fireLifeTime));
                    }
                }
            }
        }
    }

    /**
     * Called to tick the entity's position/logic.
     */
    public void tick()
    {
        super.tick();
        
        //System.out.println("remote: " + world.isRemote);

        //making client side only to fix cauldron issue
        if (world.isRemote) {
	        if (this.lightningState == 2)
	        {
	        	updateSoundEffect();
	        }
        }

        --this.lightningState;

        if (this.lightningState < 0)
        {
            if (this.boltLivingTime == 0)
            {
                this.remove();
            }
            else if (this.lightningState < -this.rand.nextInt(10))
            {
                --this.boltLivingTime;
                this.lightningState = 1;
                this.boltVertex = this.rand.nextLong();

                if (ConfigStorm.Lightning_StartsFires && !this.world.isRemote && rand.nextInt(fireChance) == 0 && this.world.getGameRules().getBoolean("doFireTick") && this.world.isAreaLoaded(new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY), MathHelper.floor(this.posZ)), 10))
                {
                    int i = MathHelper.floor(this.posX);
                    int j = MathHelper.floor(this.posY);
                    int k = MathHelper.floor(this.posZ);

                    if (CoroUtilBlock.isAir(world.getBlockState(new BlockPos(i, j, k)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(world, new BlockPos(i, j, k))) {
                        world.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().with(FireBlock.AGE, fireLifeTime), 3);
                    }
                }
            }
        }

        if (this.lightningState >= 0)
        {
            if (this.world.isRemote)
            {
            	updateFlashEffect();
            }
            else
            {
                //vanilla compat to call onStruckByLightning on entities, with effectOnly set to true to prevent fires
                LightningBoltEntity vanillaBolt =
                        new LightningBoltEntity(world, this.posX, this.posY, this.posZ, true);
                double d0 = 3.0D;
                List list = this.world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(this.posX - d0, this.posY - d0, this.posZ - d0, this.posX + d0, this.posY + 6.0D + d0, this.posZ + d0));

                for (int l = 0; l < list.size(); ++l)
                {
                    Entity entity = (Entity)list.get(l);
                    entity.onStruckByLightning(vanillaBolt);
                }
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public void updateFlashEffect() {
    	Minecraft mc = FMLClientHandler.instance().getClient();
    	//only flash sky if player is within 256 blocks of lightning
    	if (mc.player != null && mc.player.getDistanceToEntity(this) < ConfigStorm.Lightning_DistanceToPlayerForEffects) {
    		this.world.setLastLightningBolt(2);
    	}
    }
    
    @OnlyIn(Dist.CLIENT)
    public void updateSoundEffect() {
    	Minecraft mc = FMLClientHandler.instance().getClient();
    	if (mc.player != null && mc.player.getDistanceToEntity(this) < ConfigStorm.Lightning_DistanceToPlayerForEffects) {
    		this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 64.0F * (float)ConfigMisc.volWindLightningScale, 0.8F + this.rand.nextFloat() * 0.2F, false);
            this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + this.rand.nextFloat() * 0.2F, false);
    	}
    }

    protected void registerData() {}

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readAdditional(CompoundNBT par1NBTTagCompound) {}

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeAdditional(CompoundNBT par1NBTTagCompound) {}

    @OnlyIn(Dist.CLIENT)

    /**
     * Checks using a Vec3d to determine if this entity is within range of that vector to be rendered. Args: vec3D
     */
    public boolean isInRangeToRenderVec3D(Vec3 par1Vec3)
    {
        return this.lightningState >= 0;
    }
}
