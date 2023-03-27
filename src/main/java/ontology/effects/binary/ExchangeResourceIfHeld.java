package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;

import java.util.Iterator;

import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.avatar.oriented.ShootAvatar;
import ontology.effects.Effect;
import ontology.effects.unary.TransformTo;
import ontology.sprites.Resource;

/**
 * Created Eclipse IDE.
 * User: Israel Puerta Merino
 * Date: 08/02/2023
 * Time: 16:31
 * Class created by myself
 */
public class ExchangeResourceIfHeld extends TransformTo
{
    public String resource;
    public int resourceId;
    public int value;
    public int valueHeld;
    public String avatar;
    public int avatarId;

    public ExchangeResourceIfHeld(InteractionContent cnt) throws Exception
    {
    	super(cnt); 
        value = 1;
        valueHeld = 0;
        resourceId = -1;
        this.parseParameters(cnt);
        avatarId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(avatar);
        resourceId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(resource);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game) {
		if(sprite1 == null || sprite2 == null){
		    Logger.getInstance().addMessage(new Message(Message.WARNING, "Neither the 1st nor 2nd sprite can be EOS with ChangeResourceIfHeld interaction."));
		    return;
		}
		
		int numResourcesHeld = game.getNumSprites(itype);
	    int numResources = sprite1.getAmountResource(resourceId);
	    applyScore = false;
	
	    if(numResources + value <= game.getResourceLimit(resourceId)) {
		    if(numResourcesHeld > valueHeld) {
		        //System.out.println("Num recuros:" + Integer.toString(numResourcesHeld));
		        return;
		    }         
		    VGDLSprite newSprite = game.addSprite(itype, sprite1.getPosition());
	        if(newSprite != null) {
	    	    super.transformTo(newSprite, sprite1, sprite2, game);
	            
	            Iterator<VGDLSprite> spriteIt = game.getSpriteGroup(avatarId);
	            if(spriteIt != null) while(spriteIt.hasNext())
	            {
	                VGDLSprite s = spriteIt.next();
	                if (s.is_avatar) {
	                    ShootAvatar a = (ShootAvatar) s;
	                    a.modifyResource(resourceId, value);
	    	            applyScore=true;
	                }
	            }
	        } 
	    }
    }

}
