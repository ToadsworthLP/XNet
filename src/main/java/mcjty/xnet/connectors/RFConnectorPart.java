package mcjty.xnet.connectors;

import mcjty.xnet.client.XNetClientModelLoader;
import mcjty.xnet.init.ModItems;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class RFConnectorPart extends AbstractConnectorPart {

    public RFConnectorPart(EnumFacing side) {
        super(side);
    }

    public RFConnectorPart(){
        super();
    }

    @Override
    public ItemStack getPickBlock(EntityPlayer player, PartMOP hit) {
        return new ItemStack(ModItems.energyConnector);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(boolean front) {
        return XNetClientModelLoader.spriteEnergy;
    }

}
