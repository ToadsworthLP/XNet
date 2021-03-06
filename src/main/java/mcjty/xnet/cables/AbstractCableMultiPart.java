package mcjty.xnet.cables;

import com.google.common.collect.Lists;
import elec332.core.world.WorldHelper;
import mcjty.xnet.api.IXNetCable;
import mcjty.xnet.api.IXNetComponent;
import mcjty.xnet.api.XNetAPI;
import mcjty.xnet.api.XNetAPIHelper;
import mcjty.xnet.varia.UniversalUnlistedProperty;
import mcmultipart.MCMultiPartMod;
import mcmultipart.client.multipart.ICustomHighlightPart;
import mcmultipart.multipart.*;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;
import java.util.List;

/**
 * Created by Elec332 on 1-3-2016.
 */
public abstract class AbstractCableMultiPart extends Multipart implements ISlottedPart, IXNetCable, INormallyOccludingPart, ICustomHighlightPart {

    // Properties that indicate if there is a connection to certain direction.
    public static final IUnlistedProperty<Boolean> NORTH = new UniversalUnlistedProperty<>("north", Boolean.class);
    public static final IUnlistedProperty<Boolean> SOUTH = new UniversalUnlistedProperty<>("south", Boolean.class);
    public static final IUnlistedProperty<Boolean> WEST = new UniversalUnlistedProperty<>("west", Boolean.class);
    public static final IUnlistedProperty<Boolean> EAST = new UniversalUnlistedProperty<>("east", Boolean.class);
    public static final IUnlistedProperty<Boolean> UP = new UniversalUnlistedProperty<>("up", Boolean.class);
    public static final IUnlistedProperty<Boolean> DOWN = new UniversalUnlistedProperty<>("down", Boolean.class);

    public AbstractCableMultiPart(){
        this.connectedSides = EnumSet.noneOf(EnumFacing.class);
    }

    private final EnumSet<EnumFacing> connectedSides;

    public abstract boolean isAdvanced();

    @Override
    public EnumSet<PartSlot> getSlotMask() {
        return EnumSet.of(PartSlot.CENTER);
    }

    @Override
    public void onNeighborBlockChange(Block block) {
        super.onNeighborBlockChange(block);
        if (checkConnections()) {
            WorldHelper.markBlockForRenderUpdate(getWorld(), getPos());
        }
    }

    @Override
    public void onPartChanged(IMultipart part) {
        super.onPartChanged(part);
        if (checkConnections()) {
            WorldHelper.markBlockForRenderUpdate(getWorld(), getPos());
        }
    }

    @Override
    public void onNeighborTileChange(EnumFacing facing) {
        super.onNeighborTileChange(facing);
        if (checkConnections()) {
            WorldHelper.markBlockForRenderUpdate(getWorld(), getPos());
        }
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new ExtendedBlockState(MCMultiPartMod.multipart, new IProperty[0], new IUnlistedProperty[] { NORTH, SOUTH, WEST, EAST, UP, DOWN });
    }

    @Override
    public IBlockState getExtendedState(IBlockState state) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        checkConnections();
        boolean north = isConnected(EnumFacing.NORTH);
        boolean south = isConnected(EnumFacing.SOUTH);
        boolean west = isConnected(EnumFacing.WEST);
        boolean east = isConnected(EnumFacing.EAST);
        boolean up = isConnected(EnumFacing.UP);
        boolean down = isConnected(EnumFacing.DOWN);

        return extendedBlockState.withProperty(NORTH, north).withProperty(SOUTH, south).withProperty(WEST, west).withProperty(EAST, east).withProperty(UP, up).withProperty(DOWN, down);
    }

    private boolean isConnected(EnumFacing facing){
        return connectedSides.contains(facing);
    }

    private boolean checkConnections(){
        TileEntity me = getWorld().getTileEntity(getPos());
        List<EnumFacing> old = Lists.newArrayList(connectedSides);
        connectedSides.clear();
        for (EnumFacing facing : EnumFacing.VALUES){
            if (XNetAPIHelper.getComponentAt(me, facing) != null){
                connectedSides.add(facing);
                continue;
            }
            TileEntity tile = WorldHelper.getTileAt(getWorld(), getPos().offset(facing));
            if (tile != null && tile.hasCapability(XNetAPI.XNET_CABLE_CAPABILITY, null)){
                connectedSides.add(facing);
            }
        }
        return connectedSides.containsAll(old) && old.containsAll(connectedSides);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == XNetAPI.XNET_CABLE_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == XNetAPI.XNET_CABLE_CAPABILITY ? (T) this : super.getCapability(capability, facing);
    }

    /*
     * Block stuff
     */

    @Override
    public float getHardness(PartMOP hit) {
        return 1.3f;
    }

    @Override
    public Material getMaterial() {
        return Material.GLASS;
    }

    protected abstract AxisAlignedBB[] getHitBoxes();

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> list) {
        AxisAlignedBB[] hitBoxes = getHitBoxes();
        list.add(hitBoxes[6]);
        for (EnumFacing facing : EnumFacing.VALUES){
            if (connected(facing)){
                list.add(hitBoxes[facing.ordinal()]);
            }
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return super.getRenderBoundingBox();
    }

    @Override
    public void addCollisionBoxes(AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        AxisAlignedBB[] hitBoxes = getHitBoxes();
        if (mask.intersectsWith(hitBoxes[6])){
            list.add(hitBoxes[6]);
        }
        for (EnumFacing facing : EnumFacing.VALUES){
            int i = facing.ordinal();
            if (connected(facing) && mask.intersectsWith(hitBoxes[i])){
                list.add(hitBoxes[i]);
            }
        }
    }

    @Override
    public void addOcclusionBoxes(List<AxisAlignedBB> list) {
        list.add(getHitBoxes()[6]);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean drawHighlight(PartMOP hit, EntityPlayer player, float partialTicks) {
        drawHighlight(getHitBoxes());
        return true;
    }


    @SideOnly(Side.CLIENT)
    private void drawHighlight(AxisAlignedBB[] hitboxes) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        AxisAlignedBB aabb;
        for (EnumFacing facing : connectedSides) {
            aabb = hitboxes[facing.ordinal()];
            RenderGlobal.drawSelectionBoundingBox(aabb.expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)/*.offset(-d0, -d1, -d2)*/);
        }
        aabb = hitboxes[6];
        RenderGlobal.drawSelectionBoundingBox(aabb.expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)/*.offset(-d0, -d1, -d2)*/);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private boolean connected(EnumFacing side){
        return connectedSides.contains(side);
    }

    @Override
    public boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canConnectTo(IXNetCable otherCable) {
        return true;
    }

    @Override
    public boolean canConnectToSide(EnumFacing facing) {
        return true;
    }
}
