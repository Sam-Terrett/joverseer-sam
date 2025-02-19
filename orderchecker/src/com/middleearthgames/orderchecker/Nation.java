// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) braces fieldsfirst safe 
// Source File Name:   Nation.java

package com.middleearthgames.orderchecker;

import java.text.Normalizer;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import com.middleearthgames.orderchecker.Relations.NationRelationsEnum;
import com.middleearthgames.orderchecker.io.Data;

// Referenced classes of package com.middleearthgames.orderchecker:
//            Character, Army, PopCenter, Order, 
//            Main

public class Nation
{

	private int nation;
    private int capitalHex;
    private int game;
    private int turn;
    private int secret;
    private String gameType;
    private String dueDate;
    private String player;
    private Vector names;
    private Vector characters;
    private Vector popcenters;
    private Vector armies;
    private Vector nationsParsed;
    private Vector orderList;
    private Vector requestList;
    private Vector companies;
    private Relations relations = null;
    
    private int dataGameType;

    public Vector getOrderList() {
		return this.orderList;
	}
    public Order getOrder(int index) {
    	return (Order)this.orderList.elementAt(index);
    }

    public Nation()
    {
        this.nation = -1;
        this.capitalHex = -1;
        this.game = -1;
        this.turn = -1;
        this.secret = -1;
        this.gameType = null;
        this.dueDate = null;
        this.player = null;
        this.names = new Vector();
        this.characters = new Vector();
        this.popcenters = new Vector();
        this.armies = new Vector();
        this.nationsParsed = new Vector();
        this.orderList = new Vector();
        this.requestList = new Vector();
        this.companies = new Vector();
        this.relations = new Relations();
    }

    public String implementPhase(int phase, Main main)
    {
        if(phase == 1)
        {
        	this.dataGameType = main.getData().getGameType();
            if(is2950Game() && this.dataGameType != Data.GAME_2950 || is1650Game() && this.dataGameType != Data.GAME_1650)
            {
                Main.displayErrorMessage("Game type selection does not match that found in the turn file.\n\nProcessing will continue...");
            }
            Collections.sort(((java.util.List) (this.characters)));
            for(int i = this.characters.size()-1; i >= 0 ; i--)
            {
                Character character = (Character)this.characters.get(i);
                character.initStateInformation(main.getNation());
            }

            for(int i = this.armies.size() - 1 ; i >= 0 ; i--)
            {
                Army army = (Army)this.armies.get(i);
                army.initStateInformation(main.getNation());
            }

            for(int i = this.popcenters.size() -1 ; i >= 0;i--)
            {
                PopCenter pc = (PopCenter)this.popcenters.get(i);
                pc.initStateInformation(main.getNation(),main);
            }

            this.orderList.removeAllElements();
            for(int i = this.characters.size() - 1 ; i >= 0 ; i--)
            {
                Character character = (Character)this.characters.get(i);
                if(character.getNation() == this.nation)
                {
                    character.collectOrders(phase, this.orderList);
                }
            }

        }
        for(int i = 0; i < this.orderList.size(); i++)
        {
            Order order = (Order)this.orderList.get(i);
            if(order.getDone() && (phase != 1)) // make sure that we don't skip phase 1 on a repeat.
            {
                continue;
            }
            String result = order.implementPhase(phase,main.getRuleSet());
            if(result != null)
            {
                return order.getParent().getName() + " order "+ Integer.toString(order.getOrder())+" "+result;
            }
        }

        return null;
    }

    public boolean isProcessingDone()
    {
        for(int i = 0; i < this.orderList.size(); i++)
        {
            Order order = (Order)this.orderList.get(i);
            if(!order.getDone())
            {
                return false;
            }
        }

        return true;
    }

    boolean isStateDone(int state)
    {
        for(int i = 0; i < this.orderList.size(); i++)
        {
            Order order = (Order)this.orderList.get(i);
            if(!order.getStateDone(state))
            {
                return false;
            }
        }

        return true;
    }

    boolean isPartialStateDone(int state, int value)
    {
        for(int i = 0; i < this.orderList.size(); i++)
        {
            Order order = (Order)this.orderList.get(i);
            if(!order.getStateDone(state) && order.getOrder() < value)
            {
                return false;
            }
        }

        return true;
    }

    void printOrderStates()
    {
        for(int i = 0; i < this.orderList.size(); i++)
        {
            Order order = (Order)this.orderList.get(i);
            order.printStateInformation();
        }

    }

    void printStateInformation(Main main)
    {
        for(int i = 0; i < this.characters.size(); i++)
        {
            Character character = (Character)this.characters.get(i);
            character.printStateInformation(0,this);
            character.printStateInformation(9999,this);
        }

        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            army.printStateInformation(0,this,main);
            army.printStateInformation(9999,this,main);
        }

    }

    public Vector getArmyRequests(Main main)
    {
        this.requestList.removeAllElements();
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getNation() != this.nation)
            {
                continue;
            }
            String msg;
            if(canPopCenterSupportTroops(army.getLocation(),main))
            {
                army.setFoodRequired(1);
                msg = "FOOD:Will " + army.getCommander() + "'s army have 1 food?";
            } else
            {
                int food = army.getFoodRequirement() + 1;
                army.setFoodRequired(food);
                msg = "FOOD:Will " + army.getCommander() + "'s army be considered fed? (needs " + food + " food)";
            }
            JCheckBox box = new JCheckBox(msg, false);
            this.requestList.add(((Object) (box)));
        }

        return this.requestList;
    }

   public void processArmyRequests(Vector list)
    {
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getNation() == this.nation)
            {
                JCheckBox box = (JCheckBox)list.remove(0);
                army.setHasEnoughFood(box.isSelected());
            }
        }

    }

    public Vector getInfoRequests()
    {
        Vector totalList = new Vector();
        for(int i = 0; i < this.characters.size(); i++)
        {
            Character character = (Character)this.characters.get(i);
            if(character.getNation() == this.nation)
            {
                character.getInfoRequests(totalList);
            }
        }

        return totalList;
    }

    public void addTreeNodes(JTree tree, DefaultMutableTreeNode parent)
    {
        int size = this.characters.size();
        for(int i = 0; i < size; i++)
        {
            Character character = (Character)this.characters.get(i);
            if(character.getNation() == this.nation)
            {
                character.addTreeNodes(tree, parent);
            }
        }

    }

    public Character findCharacterById(String id)
    {
        int size = this.characters.size();
        for(int i = 0; i < size; i++)
        {
            Character character = (Character)this.characters.get(i);
            if(character.getId().equalsIgnoreCase(id))
            {
                return character;
            }
        }

        return null;
    }

    Character findCharacterByFullName(String name)
    {
        int size = this.characters.size();
        for(int i = 0; i < size; i++)
        {
            Character character = (Character)this.characters.get(i);
            if(character.getName().equalsIgnoreCase(name))
            {
                return character;
            }
        }

        return null;
    }

    Vector findCharactersByArmy(Army army, int order)
    {
        Vector list = new Vector();
        for(int i = 0; i < this.characters.size(); i++)
        {
            Character character = (Character)this.characters.get(i);
            if(character.getArmy(order) == army)
            {
                list.add(((Object) (character)));
            }
        }

        return list;
    }

    public Army findArmyByCommander(String name)
    {
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getCommander().equalsIgnoreCase(name))
            {
                return army;
            }
        }

        return null;
    }

    Army findCharacterInArmy(String name)
    {
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getCommander().equalsIgnoreCase(name) || army.isCharacterInArmy(name))
            {
                return army;
            }
        }

        return null;
    }

    Character findCharacterIdInArmy(String id)
    {
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            Character character = army.getCharacterIdInArmy(id);
            if(character != null)
            {
                return character;
            }
        }

        return null;
    }
    
    public Company findCompanyByCommander(String name) {
        String charNameID = name;
        if(name.length() >= 5)
        {
        	charNameID = (name.substring(0, 5));
        }
        charNameID = charNameID.toLowerCase();
        charNameID = Normalizer.normalize(charNameID, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        for(int i = 0; i < this.companies.size(); i++)
        {
            Company company = (Company)this.companies.get(i);
            if(company.getCommander().equalsIgnoreCase(charNameID))
            {
                return company;
            }
        }

        return null;
    }

    public String findCompanyCommanderByCharacterWith(String name) {
    	if (name == null) return null;
        String charNameID = name;
        if(name.length() >= 5)
        {
        	charNameID = (name.substring(0, 5));
        }
        charNameID = charNameID.toLowerCase();
        charNameID = Normalizer.normalize(charNameID, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        
    	int size = this.companies.size();
    	for (int i = 0; i < size; i++) {
    		Company c = (Company) this.companies.get(i);
    		if(c.getCharacterIdInComp(charNameID) != null) {
    			return c.getCommander();
    		}
    	}
    	return null;	
    }
    
    public Company findCompanyByCharacterWith(String name) {
    	return this.findCompanyByCommander(this.findCompanyCommanderByCharacterWith(name));
    }
    
    public Company findCompanyByCharacter(String name) {
    	int size = this.companies.size();
    	for (int i = 0; i < size; i++) {
    		Company c = (Company) this.companies.get(i);
    		if(c.getCharacterIdInComp(name) != null) {
    			return c;
    		}
    	}
    	return null;
    }
    
    public boolean isCharacterInACompany(String name) {
    	if (this.findCompanyByCharacter(name) != null) return true;
    	return false;
    }
    
    public PopCenter findPopulationCenter(int location)
    {
        int size = this.popcenters.size();
        for(int i = 0; i < size; i++)
        {
            PopCenter pc = (PopCenter)this.popcenters.get(i);
            if(pc.getLocation() == location)
            {
                return pc;
            }
        }

        return null;
    }

    PopCenter findOwnedPopulationCenter(int location)
    {
        int size = this.popcenters.size();
        for(int i = 0; i < size; i++)
        {
            PopCenter pc = (PopCenter)this.popcenters.get(i);
            if(pc.getLocation() == location && pc.getNation() == this.nation)
            {
                return pc;
            }
        }

        return null;
    }

    boolean isEnemyArmyPresent(int location,Main main)
    {
        return isEnemyArmyPresent(this.nation, location,main);
    }

    boolean isEnemyArmyPresent(int nationNum, int location,Main main)
    {
        int size = this.armies.size();
        for(int i = 0; i < size; i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getLocation() == location && main.isEnemy(nationNum, army.getNation()))
            {
                return true;
            }
        }

        return false;
    }

    boolean isNeutralArmyPresent(int location,Main main)
    {
        int size = this.armies.size();
        for(int i = 0; i < size; i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getLocation() == location && main.isNeutral(army.getNation()))
            {
                return true;
            }
        }

        return false;
    }

    private boolean canPopCenterSupportTroops(int location,Main main)
    {
        PopCenter pc = findOwnedPopulationCenter(location);
        if(pc == null)
        {
            return false;
        }
        int totalFood = pc.getFoodProvided();
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getLocation() == location && (army.getNation() == this.nation || main.isFriend(this.nation, army.getNation())))
            {
                totalFood -= army.getFoodRequirement();
            }
        }

        return totalFood >= 0;
    }

    int totalTroopsAtLocation(int location, int value)
    {
        int troops = 0;
        int size = this.armies.size();
        for(int i = 0; i < size; i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getLocation() == location && army.getNation() == this.nation)
            {
                troops += army.getTotalTroops(value);
            }
        }

        return troops;
    }

    int capturingNation(PopCenter pc, int value,Main main)
    {
        int troopCount[] = new int[26];
        for(int i = 0; i < this.armies.size(); i++)
        {
            Army army = (Army)this.armies.get(i);
            if(army.getLocation() == pc.getLocation() && main.isEnemy(pc.getNation(), army.getNation()))
            {
                troopCount[army.getNation()] += army.getTotalTroops(value);
            }
        }

        int highest = -1;
        int index = -1;
        for(int i = 1; i <= 25; i++)
        {
            if(troopCount[i] > highest)
            {
                highest = troopCount[i];
                index = i;
            }
        }

        return index;
    }

    private boolean is2950Game()
    {
        int index = this.gameType.indexOf("2950");
        return index != -1;
    }

    private boolean is1650Game()
    {
        int index = this.gameType.indexOf("1650");
        return index != -1;
    }

    public int getMaxRank(int rank)
    {
        int maxRank = 30;
        boolean game1650 = this.dataGameType == Data.GAME_1650;
        boolean game2950 = this.dataGameType == Data.GAME_2950;
        switch(rank)
        {
        default:
            break;

        case 0: // '\0'
            if(game1650 || game2950)
            {
                switch(this.nation)
                {
                case 3: // '\003'
                case 6: // '\006'
                case 11: // '\013'
                case 17: // '\021'
                case 19: // '\023'
                case 20: // '\024'
                case 24: // '\030'
                case 25: // '\031'
                    maxRank = 40;
                    break;
                }
                if(game1650)
                {
                    switch(this.nation)
                    {
                    case 5: // '\005'
                        maxRank = 40;
                        break;
                    }
                }
                if(game2950)
                {
                    switch(this.nation)
                    {
                    case 22: // '\026'
                        maxRank = 40;
                        break;
                    }
                }
            }
            break;

        case 1: // '\001'
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 14: // '\016'
            case 16: // '\020'
            case 23: // '\027'
                maxRank = 40;
                break;
            }
            break;

        case 2: // '\002'
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 2: // '\002'
            case 17: // '\021'
                maxRank = 40;
                break;
            }
            break;

        case 3: // '\003'
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 4: // '\004'
            case 7: // '\007'
            case 15: // '\017'
                maxRank = 40;
                break;
            }
            if(!game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 20: // '\024'
                maxRank = 40;
                break;
            }
            break;
        }
        return maxRank;
    }

    public boolean hasSpellPrereq(int spell)
    {
        boolean prerequisite = false;
        boolean game1650 = this.dataGameType == Data.GAME_1650;
        boolean game2950 = this.dataGameType == Data.GAME_2950;
        switch(spell)
        {
        default:
            break;

        case 244: 
            if(game1650 || game2950)
            {
                switch(this.nation)
                {
                case 11: // '\013'
                case 20: // '\024'
                    prerequisite = true;
                    break;
                }
            }
            break;

        case 246: 
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 15: // '\017'
            case 16: // '\020'
                prerequisite = true;
                break;
            }
            break;

        case 248: 
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 18: // '\022'
                prerequisite = true;
                break;
            }
            break;

        case 314: 
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 12: // '\f'
                prerequisite = true;
                break;
            }
            break;

        case 508: 
            if(!game1650 && !game2950)
            {
                break;
            }
            switch(this.nation)
            {
            case 3: // '\003'
            case 13: // '\r'
            case 19: // '\023'
                prerequisite = true;
                break;
            }
            break;

        case 512: 
            if(game1650 || game2950)
            {
                switch(this.nation)
                {
                case 11: // '\013'
                case 15: // '\017'
                case 18: // '\022'
                    prerequisite = true;
                    break;
                }
            }
            if(!game1650)
            {
                break;
            }
            switch(this.nation)
            {
            case 20: // '\024'
                prerequisite = true;
                break;
            }
            break;
        }
        return prerequisite;
    }

    public void SetNation(@SuppressWarnings("hiding") int nation)
    {
        this.nation = nation;
    }

    public void setCapital(int hex)
    {
        this.capitalHex = hex;
    }

    public void setGame(int game)
    {
        this.game = game;
    }

    public void setTurn(int turn)
    {
        this.turn = turn;
    }

    public void setSecret(int secret)
    {
        this.secret = secret;
    }

    public void setGameType(String type)
    {
        this.gameType = type;
    }

    public void setDueDate(String date)
    {
        this.dueDate = date;
    }

    public void setPlayer(String player)
    {
        this.player = player;
    }

    public void addNation(String name)
    {
        this.names.add(((Object) (name)));
    }

    public void addCharacter(Character character)
    {
        this.characters.add(((Object) (character)));
    }

    public void removeCharacter(Character character)
    {
        this.characters.remove(((Object) (character)));
    }

    public void addPopulationCenter(PopCenter pc)
    {
        this.popcenters.add(((Object) (pc)));
    }

    public void removePopulationCenter(PopCenter pc)
    {
        this.popcenters.remove(((Object) (pc)));
    }

    public void addCompany(Company comp) {
    	this.companies.add((Object) comp);
    }
    
    public void removeCompany(Company comp) {
    	this.companies.remove((Object) comp);
    }
    
    public void addArmy(Army army)
    {
        this.armies.add(((Object) (army)));
    }

    public void removeArmy(Army army)
    {
        this.armies.remove(((Object) (army)));
    }

    public void addNationParsed(int nationNumber)
    {
        this.nationsParsed.add(((Object) (Integer.valueOf(nationNumber))));
    }

    public int getNation()
    {
        return this.nation;
    }

    int getCapital()
    {
        return this.capitalHex;
    }

    public int getGame()
    {
        return this.game;
    }

    public int getTurn()
    {
        return this.turn;
    }

    public int getSecret()
    {
        return this.secret;
    }

    public String getGameType()
    {
        return this.gameType;
    }

    public String getDueDate()
    {
        return this.dueDate;
    }

    public String getPlayer()
    {
        return this.player;
    }

    public int getCharacterCount()
    {
        return this.characters.size();
    }

    public int getArmyCount()
    {
        return this.armies.size();
    }

    public int getPopulationCenterCount()
    {
        return this.popcenters.size();
    }

    public Vector getNationParsed()
    {
        return this.nationsParsed;
    }
    
    public void setNationRelations(int relationInt) {
    	
    	this.getRelations().setRelationsFor(this.getNation(), relationInt);
    }
    
    public Relations getRelations() {
    	return this.relations;
    }
    
    public NationRelationsEnum getRelationFor(int nationNum) {
    	return this.getRelations().getRelationsFor(nationNum);
    }

    public String getNationName(int nationNumber)
    {
        if(nationNumber >= 0 && nationNumber < this.names.size())
        {
            StringBuffer name = new StringBuffer();
            name.append((String)this.names.get(nationNumber));
            name.append(" (" + nationNumber + ")");
            return name.toString();
        } else
        {
            return "";
        }
    }

    public boolean isNationComplete()
    {
        if(this.nation == -1 || this.capitalHex == -1 || this.game == -1 || this.turn == -1 || this.secret == -1 || this.names == null || this.gameType == null || this.dueDate == null || this.player == null)
        {
            return false;
        }
        int size = this.characters.size();
        for(int i = 0; i < size; i++)
        {
            Character character = (Character)this.characters.get(i);
            if(!character.isCharacterComplete())
            {
                return false;
            }
        }

        size = this.popcenters.size();
        for(int i = 0; i < size; i++)
        {
            PopCenter pc = (PopCenter)this.popcenters.get(i);
            if(!pc.isPopCenterComplete())
            {
                return false;
            }
        }

        size = this.armies.size();
        for(int i = 0; i < size; i++)
        {
            Army army = (Army)this.armies.get(i);
            if(!army.IsArmyComplete())
            {
                return false;
            }
        }

        return true;
    }
}
