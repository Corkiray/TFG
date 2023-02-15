package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.avatar.MovingAvatar;
import ontology.avatar.oriented.ShootAvatar;
import ontology.effects.unary.TransformTo;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created Eclipse IDE.
 * User: Israel Puerta Merino
 * Date: 09/02/2023
 * Time: 12:51
 * Class created by myself
 */
public class TransformAndChangeResource extends TransformTo {

    public String avatar;
    public int avatarId;
    public String resource;
    public int resourceId;
    public int value = -1;

    public TransformAndChangeResource(InteractionContent cnt) throws Exception
    {
        super(cnt);
        this.parseParameters(cnt);
        avatarId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(avatar);
        resourceId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(resource);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game)
    {

	if(sprite1 == null || sprite2 == null){
	    Logger.getInstance().addMessage(new Message(Message.WARNING, "Neither the 1st nor 2nd sprite can be EOS with TransformAndChangeResource interaction."));
	    return;
	}
	
	    VGDLSprite newSprite = game.addSprite(itype, sprite1.getPosition());
        if(newSprite != null) {
    	    super.transformTo(newSprite, sprite1, sprite2, game);
            applyScore=true;
            
            Iterator<VGDLSprite> spriteIt = game.getSpriteGroup(avatarId);

            if(spriteIt != null) while(spriteIt.hasNext())
            {
                VGDLSprite s = spriteIt.next();
                if (s.is_avatar) {
                    ShootAvatar a = (ShootAvatar) s;
                    a.modifyResource(resourceId, value);
                }
            }
        } 
        else {
        	applyScore=false;
        }

    }

}
