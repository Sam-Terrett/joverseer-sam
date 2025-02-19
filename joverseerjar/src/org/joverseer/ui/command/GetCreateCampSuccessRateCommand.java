package org.joverseer.ui.command;

import java.util.ArrayList;

import org.joverseer.domain.Character;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.support.GameHolder;
import org.springframework.richclient.command.ActionCommand;

public class GetCreateCampSuccessRateCommand extends ActionCommand {

    //dependencies
	GameHolder gameHolder;

    public GetCreateCampSuccessRateCommand(GameHolder gameHolder) {
        super("getCreateCampSuccessRateCommand");
        this.gameHolder = gameHolder;
    }

    @Override
	protected void doExecuteCommand() {
    	Game game = this.gameHolder.getGame();

    	for (int i=0; i<=game.getMaxTurn(); i++) {
    		Turn t = game.getTurn(i);
    		if (t == null) continue;
    		for (Character c : (ArrayList<Character>)t.getContainer(TurnElementsEnum.Character).getItems()) {
    			String orderResults = c.getOrderResults();
    			if (orderResults == null || orderResults.equals("")) continue;
    			orderResults = orderResults.replace("\n", " ");
    			orderResults = orderResults.replace("\r", " ");
    			orderResults = orderResults.replace("  ", " ");
    			orderResults = orderResults.replace("  ", " ");
    			orderResults = orderResults.replace("  ", " ");
    			if (orderResults.contains("to create a camp.")) {
	    			boolean success = orderResults.contains("A camp named");
	    			if (!success && !orderResults.contains("Continued efforts may succeed")) return;
	    			Turn prev = game.getTurn(i-1);
    				if (prev == null) continue;
    				Character co = (Character)prev.getContainer(TurnElementsEnum.Character).findFirstByProperty("name", c.getName());
    				if (co == null) continue;

    			}
    		}
    	}
    }
}
