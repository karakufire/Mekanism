package mekanism.client;

import org.lwjgl.opengl.GL11;

import mekanism.common.TileEntityMetallurgicInfuser;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderMetallurgicInfuser extends TileEntitySpecialRenderer
{
	private ModelMetallurgicInfuser model = new ModelMetallurgicInfuser();
	
	@Override
	public void renderTileEntityAt(TileEntity var1, double var2, double var4, double var6, float var8)
	{
		renderAModelAt((TileEntityMetallurgicInfuser) var1, var2, var4, var6, 1F);
	}
	
	private void renderAModelAt(TileEntityMetallurgicInfuser tileEntity, double x, double y, double z, float f)
	{
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5f, (float) y + 1.5f, (float) z + 0.5f);
		bindTextureByName("/mods/mekanism/render/MetallurgicInfuser.png");
		
	    switch(tileEntity.facing)
	    {
		    case 2: GL11.glRotatef(90, 0.0F, 1.0F, 0.0F); break;
			case 3: GL11.glRotatef(270, 0.0F, 1.0F, 0.0F); break;
			case 4: GL11.glRotatef(180, 0.0F, 1.0F, 0.0F); break;
			case 5: GL11.glRotatef(0, 0.0F, 1.0F, 0.0F); break;
	    }
		
	    GL11.glTranslatef(0, -1.5F, 0);
		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		model.render(0.0625F);
		GL11.glPopMatrix();
	}
}
