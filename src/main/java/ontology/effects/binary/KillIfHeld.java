package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.effects.Effect;

/**
 * Created Eclipse IDE.
 * User: Israel Puerta Merino
 * Date: 08/02/2023
 * Time: 16:31
 * Class created by myself
 */
public class KillIfHeld extends Effect
{
    public boolean killResource;
    public String heldResource;
    public int heldResourceId;
    public int valueHeld;

    public KillIfHeld(InteractionContent cnt)
    {
        valueHeld = 0;
        killResource = true;
        this.parseParameters(cnt);
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
    applyScore = false;

    if(numResourcesHeld > valueHeld) {
        //System.out.println("Num recuros:" + Integer.toString(numResourcesHeld));
        return;
    }   
    applyScore = true;
      
	if(killResource)
        //boolean variable set to false to indicate the sprite was not transformed
        game.killSprite(sprite2, false);
    }
}