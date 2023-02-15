package ontology.effects.binary;

import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import core.content.InteractionContent;
import core.game.Game;
import core.logging.Logger;
import core.logging.Message;
import ontology.avatar.oriented.ShootAvatar;
import ontology.effects.Effect;

import java.awt.*;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 04/11/13
 * Time: 15:56
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class RefreshVisionState extends Effect
{
    public int range;
    public String avatar;
    public int avatarId;

    public RefreshVisionState(InteractionContent cnt)
    {
        this.parseParameters(cnt);
        avatarId = VGDLRegistry.GetInstance().getRegisteredSpriteValue(avatar);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game)
    {
        if(sprite1 == null || sprite2 == null){
            Logger.getInstance().addMessage(new Message(Message.WARNING, "Neither 1st not 2nd sprite can be EOS with Align interaction."));
            return;
        }
        
        Iterator<VGDLSprite> spriteIt = game.getSpriteGroup(avatarId);

        if(spriteIt != null) while(spriteIt.hasNext())
        {
            VGDLSprite s = spriteIt.next();
            if (s.is_avatar) {
                String state = "true";
            	int distance = Math.abs(sprite2.rect.x - s.rect.x) + Math.abs(sprite2.rect.y - s.rect.y);
                //System.out.print("Distancia:"+Integer.toString(distance)+"\n");
            	if(distance <= range*36) {
                	state = "false"; 
                }
                sprite1.invisible = state;
                sprite1.hidden = state;
                sprite1.update(game);
            }
        }
    }
}
