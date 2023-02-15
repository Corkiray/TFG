package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.effects.Effect;
import ontology.sprites.Resource;

/**
 * Created Eclipse IDE.
 * User: Israel Puerta Merino
 * Date: 08/02/2023
 * Time: 16:31
 * Class created by myself
 */
public class ChangeResourceIfHeld extends Effect
{
    public String resource;
    public int resourceId;
    public boolean killResource;
    public String heldResource;
    public int heldResourceId;
    public int value;
    public int valueHeld;

    public ChangeResourceIfHeld(InteractionContent cnt)
    {
        value = 1;
        valueHeld = 0;
        resourceId = -1;
        killResource = true;
        this.parseParameters(cnt);
        resourceId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(resource);
        is_kill_effect = killResource;
        heldResourceId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(heldResource);

    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game) {
	if(sprite1 == null || sprite2 == null){
	    Logger.getInstance().addMessage(new Message(Message.WARNING, "Neither the 1st nor 2nd sprite can be EOS with ChangeResourceIfHeld interaction."));
	    return;
	}
	
	int numResourcesHeld = game.getNumSprites(heldResourceId);
    int numResources = sprite1.getAmountResource(resourceId);
    applyScore = false;
	
    if(numResources + value <= game.getResourceLimit(resourceId)) {
	    if(numResourcesHeld > valueHeld) {
	        //System.out.println("Num recuros:" + Integer.toString(numResourcesHeld));
	        return;
	    }   
        sprite1.modifyResource(resourceId, value);
        applyScore = true;
      
	    if(killResource)
	        //boolean variable set to false to indicate the sprite was not transformed
	        game.killSprite(sprite2, false);
	    }
    }

}
