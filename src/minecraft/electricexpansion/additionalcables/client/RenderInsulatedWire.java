package electricexpansion.additionalcables.client;

import org.lwjgl.opengl.GL11;

import electricexpansion.EECommonProxy;
import electricexpansion.ElectricExpansion;
import electricexpansion.additionalcables.cables.TileEntityInsulatedWire;
import universalelectricity.basiccomponents.ModelCopperWire;
import net.minecraft.src.Block;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

public class RenderInsulatedWire extends TileEntitySpecialRenderer 
{
	private ModelCopperWire model;

    public RenderInsulatedWire()
    {
        model = new ModelCopperWire();
    }
    public void renderAModelAt(TileEntityInsulatedWire tileEntity, double x, double y, double z, float f)
    {
    	String textureToUse = null;
    	int ID = tileEntity.getBlockType().blockID;
    	int meta = tileEntity.getBlockMetadata();
        if(meta != -1)
        {
        	if(ID == ElectricExpansion.insulatedWire)
        	{
        		if(meta == 0)
        			textureToUse = EECommonProxy.TEXTURES + "CopperWire.png";
        		else if(meta == 1)
        			textureToUse = EECommonProxy.TEXTURES + "InsulatedTinWire.png";
        		else if(meta == 2)
        			textureToUse = EECommonProxy.TEXTURES + "InsulatedSilverWire.png";
        		else if(meta == 3)
        			textureToUse = EECommonProxy.TEXTURES + "InsulatedHVWire.png";
        	}
        	else if(ID == ElectricExpansion.onSwitchWire)
        	{
        		if(meta == 0)
        			textureToUse = EECommonProxy.TEXTURES + "CopperSwitchWireOn.png";
        		else if(meta == 1)
        			textureToUse = EECommonProxy.TEXTURES + "TinSwitchWireOn.png";
        		else if(meta == 2)
        			textureToUse = EECommonProxy.TEXTURES + "SilverSwitchWireOn.png";
        		else if(meta == 3)
        			textureToUse = EECommonProxy.TEXTURES + "HVSwitchWireOn.png";
        	}	
        	else if(ID == ElectricExpansion.offSwitchWire)
        	{
        		if(meta == 0)
        			textureToUse = EECommonProxy.TEXTURES + "CopperSwitchWireOff.png";
        		else if(meta == 1)
        			textureToUse = EECommonProxy.TEXTURES + "TinSwitchWireOff.png";
        		else if(meta == 2)
        			textureToUse = EECommonProxy.TEXTURES + "SilverSwitchWireOff.png";
        		else if(meta == 3)
        			textureToUse = EECommonProxy.TEXTURES + "HVSwitchWireOff.png";
        	}	
        }
        
    	//Texture file
    	bindTextureByName(textureToUse);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        GL11.glScalef(1.0F, -1F, -1F);

        if (tileEntity.connectedBlocks[0] != null)
        {
            model.renderBottom();
        }

        if (tileEntity.connectedBlocks[1] != null)
        {
            model.renderTop();
        }

        if (tileEntity.connectedBlocks[2] != null)
        {
        	model.renderBack();
        }

        if (tileEntity.connectedBlocks[3] != null)
        {
        	model.renderFront();
        }

        if (tileEntity.connectedBlocks[4] != null)
        {
        	model.renderLeft();
        }

        if (tileEntity.connectedBlocks[5] != null)
        {
        	model.renderRight();
        }

        model.renderMiddle();
        GL11.glPopMatrix();
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double var2, double var4, double var6, float var8)
    {
        this.renderAModelAt((TileEntityInsulatedWire)tileEntity, var2, var4, var6, var8);
    }
}