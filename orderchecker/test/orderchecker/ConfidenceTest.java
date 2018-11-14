package orderchecker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Vector;

import com.middleearthgames.orderchecker.Main;
import com.middleearthgames.orderchecker.Nation;
import com.middleearthgames.orderchecker.Order;
import com.middleearthgames.orderchecker.Ruleset;
import com.middleearthgames.orderchecker.io.Data;
import com.middleearthgames.orderchecker.io.ImportRulesCsv;

public class ConfidenceTest {
	final static String RANK_RULE ="RANK,0";
	final static String COMMAND_RANK_MIN=",0,1";
	final static String AGENT_RANK_MIN=",1,1";
	final static String EMISSARY_RANK_MIN=",2,1";
	final static String MAGE_RANK_MIN=",3,1";
	final static String EXCLUSIVE = ",1";
	final static String NOT_EXCLUSIVE = ",0";
	final static String AT_CAPITAL=",1";
	final static String ANYWHERE=",0";
	@Test
	public void readRulesTest() {
		Data data = new Data();
		final Main main = new Main(true,data);
		Main.main = main;
		Ruleset ruleSet = new Ruleset();
		main.setRuleSet(ruleSet);
		ImportRulesCsv rules = new ImportRulesCsv("ruleset.csv", ruleSet);
		boolean result = rules.getRules();
		if (!result) {
			// maybe we're running under eclipse.
			rules.closeFile();
			rules = new ImportRulesCsv("bin/metadata/orderchecker/ruleset.csv", ruleSet);
			result = rules.getRules();
			assertTrue("Failed to read ruleset", result);
		}
		String error = rules.parseRules();
		rules.closeFile();
		assertNull(error);
	}

	//use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
	private InputStreamReader makeStreamReader(String s) {
        InputStream is = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));
        return new InputStreamReader(is);
	}
	
	private void invokeOrderchecker(Main main) {
		String error = main.getNation().implementPhase(1, main);
		assertNull(error);
		
		boolean done;
		int safety;
		Vector requests = main.getNation().getArmyRequests(main);
		main.getNation().processArmyRequests( requests );
		done = false;
		safety = 0;
		do {
			if (done || safety >= 20) {
				break;
			}
			safety++;
			error = main.getNation().implementPhase( 2, main );
			assertNull(error);
			requests = main.getNation().getInfoRequests();
//			parseInfoRequests(requests);
			if (requests.size() > 0) {
//				this.infoRequests(main, requests); //hook
			}
			done = main.getNation().isProcessingDone();
		} while (true);
		assertNotEquals("order checking stuck in loop",20, safety);
		
	}
	/**
	 * Dont end the string with \r\n
	 * @param rulesAsSingleString
	 * @return
	 */
	private Main createRuleset(String rulesAsSingleString) {
		Data data = new Data();
		final Main main = new Main(true,data);
		Ruleset ruleSet = new Ruleset();
		main.setRuleSet(ruleSet);
		ImportRulesCsv rules = new ImportRulesCsv("", ruleSet);
		boolean result = rules.openStream(makeStreamReader(rules.makeRuleString(rulesAsSingleString)));
		assertTrue(result);
		String error = rules.parseRules();
		rules.closeFile();
		assertNull(error);
		return main;
	}
	private Nation createNation(Main main) {
		Nation nation = new Nation();
		main.setNation(nation);
		main.getData().setGameType("1650");
		nation.setGameType("1650");
		return nation;
	}
	/**
	 * Create character and add it to the nation.
	 * @param nation
	 * @param id
	 * @param name
	 * @return
	 */
	private com.middleearthgames.orderchecker.Character createCharacter(Nation nation,String id,String name) {
		com.middleearthgames.orderchecker.Character character = new com.middleearthgames.orderchecker.Character(id);
		character.setName(name);
		nation.addCharacter(character);
		return character;
	}
	private void assertOrderResultsCounts(Order order,int error,int help,int info,int warning) {
		assertTrue("No rules applied",order.getRules().size() > 0);
		assertEquals("Error results count",error,order.getErrorResults().size());
		assertEquals("Help results count",help,order.getHelpResults().size());
		assertEquals("Info results count",info,order.getInfoResults().size());
		assertEquals("Warning results count",warning,order.getWarnResults().size());
	}
	@Test
	public void simpleTest() {
		final Main main = createRuleset("605,GrdLoc,PC,0,1,\n" +
			    "605,,"+RANK_RULE+AGENT_RANK_MIN+",0,1"+ANYWHERE);

		Main.main = main;
		
		Nation nation = createNation(main);

		com.middleearthgames.orderchecker.Character character = createCharacter(nation,"testc","no name");
		
		Order order1= new Order(character,605);
		Order order2= new Order(character,605);
		character.addOrder(order1);
		character.addOrder(order2);
		
		invokeOrderchecker(main);

		assertOrderResultsCounts(nation.getOrder(0),3,0,0,1);
		assertEquals("605 error","Could not determine the location.",order1.getErrorResults().get(0));
		assertEquals("605 error","no name has no agent rank.",order1.getErrorResults().get(1));
		assertEquals("605 error","Can't have a duplicate order",order1.getErrorResults().get(2));
		assertEquals("605 warning","no name should have at least a agent rank of 1.",order1.getWarnResults().get(0));
		assertOrderResultsCounts(nation.getOrder(1),3,0,0,1);
	}

	@Test
	public void agentTest() {
		final Main main = createRuleset("605,GrdLoc,PC,0,1,\n" +
			    "605,,"+RANK_RULE+AGENT_RANK_MIN+",0"+EXCLUSIVE+ANYWHERE);

		Nation nation = createNation(main);

		com.middleearthgames.orderchecker.Character character = createCharacter(nation,"testA","Agent Orange");
		Order order1= new Order(character,605);
		character.addOrder(order1);
		
		invokeOrderchecker(main);
		
		assertOrderResultsCounts(order1,2,0,0,1);
		assertEquals("605 error","Could not determine the location.",order1.getErrorResults().get(0));
		assertEquals("605 error","Agent Orange has no agent rank.",order1.getErrorResults().get(1));
		assertEquals("605 warning","Agent Orange should have at least a agent rank of 1.",order1.getWarnResults().get(0));
		
		character.setAgentRank(1);
		invokeOrderchecker(main);
		
		assertOrderResultsCounts(nation.getOrder(0),1,0,0,0);
		assertEquals("605 error","Could not determine the location.",order1.getErrorResults().get(0));
	}
	@Test
	public void commandTest() {
		final Main main = createRuleset("745,CreCmpy,COMMANDERNOT,0,0,0,5\r\n" + 
				"745,,LAND,0,,,\r\n" + 
				"745,,SETCOMMAND,0,1,1," 
//				+ "745,," + RANK_RULE + COMMAND_RANK_MIN + ",0" + EXCLUSIVE + ANYWHERE) // RANK is implied by SETCOMMAND
				);

		Nation nation = createNation(main);

		com.middleearthgames.orderchecker.Character character = createCharacter(nation,"testA","Agent Orange");
		Order order1= new Order(character,745);
		character.addOrder(order1);
		
		invokeOrderchecker(main);
		
		assertOrderResultsCounts(nation.getOrder(0),3,0,0,0);
		assertEquals("745 error","Could not determine the location.",order1.getErrorResults().get(0));
		assertEquals("745 error","Could not determine the location.",order1.getErrorResults().get(1));
		assertEquals("745 error","Agent Orange has no command rank.",order1.getErrorResults().get(2));

		character.setCommandRank(1);
		invokeOrderchecker(main);

		assertOrderResultsCounts(nation.getOrder(0),2,0,0,0);
		assertEquals("745 error","Could not determine the location.",order1.getErrorResults().get(0));
		assertEquals("745 error","Could not determine the location.",order1.getErrorResults().get(1));
	}
	@Test
	public void emissaryTest() {
		final Main main = createRuleset("585,Uncover," + RANK_RULE + EMISSARY_RANK_MIN + ",0" + EXCLUSIVE + ANYWHERE);

		Nation nation = createNation(main);

		com.middleearthgames.orderchecker.Character character = createCharacter(nation,"testA","Agent Orange");
		Order order1= new Order(character,585);
		character.addOrder(order1);
		
		invokeOrderchecker(main);
		
		assertOrderResultsCounts(nation.getOrder(0),1,0,0,1);
		assertEquals("585 error","Agent Orange has no emissary rank.",order1.getErrorResults().get(0));
		assertEquals("585 warning","Agent Orange should have at least a emissary rank of 1.",order1.getWarnResults().get(0));

		character.setEmissaryRank(1);
		invokeOrderchecker(main);

		assertOrderResultsCounts(nation.getOrder(0),0,0,0,0);
	}
	@Test
	public void mageTest() {
		final Main main = createRuleset("710,PrenMgy,PC,0,0,,\r\n" + 
				"710,," + RANK_RULE + MAGE_RANK_MIN + ",0" + EXCLUSIVE + ANYWHERE);

		Nation nation = createNation(main);

		com.middleearthgames.orderchecker.Character character = createCharacter(nation,"testA","Agent Orange");
		Order order1= new Order(character,710);
		character.addOrder(order1);
		
		invokeOrderchecker(main);
		
		assertOrderResultsCounts(nation.getOrder(0),2,0,0,1);
		assertEquals("710 error","Could not determine the location.",order1.getErrorResults().get(0));
		assertEquals("710 error","Agent Orange has no mage rank.",order1.getErrorResults().get(1));
		assertEquals("710 warning","Agent Orange should have at least a mage rank of 1.",order1.getWarnResults().get(0));

		character.setMageRank(1);
		invokeOrderchecker(main);

		assertOrderResultsCounts(nation.getOrder(0),1,0,0,0);
		assertEquals("710 error","Could not determine the location.",order1.getErrorResults().get(0));
	}
	

}