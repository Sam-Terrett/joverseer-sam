package org.joverseer.tools.combatCalc;

import java.io.Serializable;
import java.util.ArrayList;

import org.joverseer.domain.Army;
import org.joverseer.domain.ArmyElement;
import org.joverseer.domain.ArmyElementType;
import org.joverseer.domain.ArmyEstimate;
import org.joverseer.domain.ArmyEstimateElement;
import org.joverseer.domain.Character;
import org.joverseer.domain.ClimateEnum;
import org.joverseer.game.Game;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.metadata.domain.HexTerrainEnum;
import org.joverseer.metadata.domain.Nation;
import org.joverseer.support.GameHolder;
import org.joverseer.support.NationMap;
import org.joverseer.tools.infoCollectors.characters.AdvancedCharacterWrapper;
import org.joverseer.tools.infoCollectors.characters.CharacterAttributeWrapper;
import org.joverseer.tools.infoCollectors.characters.CharacterInfoCollector;

/**
 * Represents an army for a land combat for the combat calculator
 * 
 * Holds all the information pertinent to an army such as:
 * - commander and rank
 * - nation number
 * - elements
 * - morale
 * - tactic
 * - losses in the combat
 *  
 * @author Marios Skounakis
 *
 */
public class CombatArmy implements Serializable {
    private static final long serialVersionUID = -6792883257780173753L;
    String commander;
    int nationNo;
    ArrayList<ArmyElement> elements = new ArrayList<ArmyElement>();
    int morale;
    int commandRank;
    TacticEnum tactic;
    double losses; // Stored as a percent of the original constitution?
    int strOfAttackingArmy = 0;


    int offensiveAddOns;
    int defensiveAddOns;
    
    String region = "Default";

    public String getCommander() {
        return this.commander;
    }

    public void setCommander(String commander) {
        this.commander = commander;
    }

    public int getCommandRank() {
        return this.commandRank;
    }

    public void setCommandRank(int commandRank) {
        this.commandRank = commandRank;
    }

    public int getDefensiveAddOns() {
        return this.defensiveAddOns;
    }

    public void setDefensiveAddOns(int defensiveAddOns) {
        this.defensiveAddOns = defensiveAddOns;
    }

    public ArrayList<ArmyElement> getElements() {
        return this.elements;
    }

    public void setElements(ArrayList<ArmyElement> elements) {
        this.elements = elements;
    }

    public void addElement(ArmyElement armyElement) {
    	this.elements.add(armyElement) ;
    }
    
    public int getMorale() {
        return this.morale;
    }

    public void setMorale(int morale) {
        this.morale = morale;
    }

    public int getNationNo() {
        return this.nationNo;
    }

    public void setNationNo(int nationNo) {
        this.nationNo = nationNo;
    }

    public int getOffensiveAddOns() {
        return this.offensiveAddOns;
    }

    public void setOffensiveAddOns(int offensiveAddOns) {
        this.offensiveAddOns = offensiveAddOns;
    }

    public TacticEnum getTactic() {
        return this.tactic;
    }

    public void setTactic(TacticEnum tactic) {
        this.tactic = tactic;
    }
    
    public void setRegion(String reg) {
    	this.region = reg;
    }

    public String getRegion() {
    	return this.region;
    }
    
    public double getLosses() {
        return this.losses;
    }

    public void setLosses(double losses) {
        this.losses = losses;
    }
    
    public void setStrOfAttackingArmy(int str) {
    	this.strOfAttackingArmy = str;
    }
    
    public int getStrOfAttackingArmy() {
    	return this.strOfAttackingArmy;
    }
    
    public void addStrToAttackingArmy(int toAdd) {
    	this.strOfAttackingArmy += toAdd;
    }
    
    public ArmyElement getArmyElement(ArmyElementType aet) {
        for (ArmyElement ae : getElements()) {
            if (ae.getArmyElementType() == aet) return ae;
        }
        return null;
    }
    
    public ArmyElement getHC() {
        return getArmyElement(ArmyElementType.HeavyCavalry); 
    }

    public ArmyElement getLC() {
        return getArmyElement(ArmyElementType.LightCavalry); 
    }

    public ArmyElement getHI() {
        return getArmyElement(ArmyElementType.HeavyInfantry); 
    }

    public ArmyElement getLI() {
        return getArmyElement(ArmyElementType.LightInfantry); 
    }

    public ArmyElement getAR() {
        return getArmyElement(ArmyElementType.Archers); 
    }

    public ArmyElement getMA() {
        return getArmyElement(ArmyElementType.MenAtArms); 
    }

    public ArmyElement getWM() {
        return getArmyElement(ArmyElementType.WarMachimes); 
    }

    public Nation getNation() {
        return NationMap.getNationFromNo(getNationNo());
    }

    public void setNation(Nation nation) {
        setNationNo(nation.getNumber());
    }
    
    public void addArmyElement(ArmyElement ae) {
    	getElements().add(ae);
    }

    
    public CombatArmy() {
        for (ArmyElementType aet : ArmyElementType.values()) {
            ArmyElement ae = new ArmyElement(aet, 0);
            getElements().add(ae);
        }
        this.tactic = TacticEnum.Standard;
    }
    
    public CombatArmy(CombatArmy ca) {
    	GameHolder.instance().getGame();
        for (ArmyElementType aet : ArmyElementType.values()) {
            ArmyElement ae = ca.getArmyElement(aet);
            if (ae == null) {
                ae = new ArmyElement(aet, 0);
            }
            ArmyElement nae = new ArmyElement(ae.getArmyElementType(), (int)(ae.getNumber() * (100 - ca.getLosses()) / 100));
            nae.setWeapons(ae.getWeapons());
            nae.setTraining(ae.getTraining());
            nae.setArmor(ae.getArmor());
            getElements().add(nae);
        }
        
        setNationNo(ca.getNationNo());
        setCommander(ca.getCommander());
        setMorale(ca.getMorale());
        setCommandRank(ca.getCommandRank());
        setTactic(TacticEnum.Standard);
    }

    public CombatArmy(Army a) {
        Game g = GameHolder.instance().getGame();
        boolean knownTroops = false;
        
        for (ArmyElementType aet : ArmyElementType.values()) {
            ArmyElement ae = a.getElement(aet);
            if (ae == null) {
                ae = new ArmyElement(aet, 0);
            } else {
            	knownTroops = true;
            }
            ArmyElement nae = new ArmyElement(ae.getArmyElementType(), ae.getNumber());
            nae.setWeapons(ae.getWeapons());
            nae.setTraining(ae.getTraining());
            nae.setArmor(ae.getArmor());
            getElements().add(nae);
        }
        
        if (knownTroops == false && a.getTroopCount()>0) {
        	//Assume troops are HI
            ArmyElement nae = this.getHI();
            nae.setNumber(a.getTroopCount());
            nae.setWeapons(10);
            nae.setTraining(25);
            nae.setArmor(0);      	
        }
        
        Character c = (Character) g.getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("name",
                a.getCommanderName());
        if (c != null) {
            setCommandRank(c.getCommandTotal());
        } else {
            setCommandRank(50);
        }
        setNationNo(a.getNationNo());
        setCommander(a.getCommanderName());
        setMorale(a.getMorale());
        setTactic(TacticEnum.Standard);
    }
    
    public CombatArmy(ArmyEstimate a) {
    	this(a, 0);
    }
    
    public CombatArmy(ArmyEstimate a, int lossOptimismFactor) {
    	double left = 1d;
    	for (int l : a.getLosses(lossOptimismFactor)) {
    		left *= (100d - (double)l) / 100d;
    	}
    	
        Game g = GameHolder.instance().getGame();
        for (ArmyElementType aet : ArmyElementType.values()) {
            ArmyEstimateElement ae = a.getRegiment(aet);
            if (ae == null) {
                ae = new ArmyEstimateElement();
                ae.setType(aet);
                ae.setNumber(0);
            }
            ArmyElement nae = new ArmyElement(ae.getType(), (int)Math.round((double)ae.getNumber() * left));
            nae.setWeapons(ae.getWeapons());
            nae.setTraining(ae.getTraining());
            nae.setArmor(ae.getArmor());
            getElements().add(nae);
        }
        
        setCommandRank(50);
        Character c = (Character) g.getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("name",
                a.getCommanderName());
        if (c != null && c.getCommandTotal() > 0) {
            setCommandRank(c.getCommandTotal());
        } else {
        	AdvancedCharacterWrapper acw = CharacterInfoCollector.instance().getCharacterForTurn(a.getCommanderName(), g.getCurrentTurn());
        	if (acw != null) {
        		CharacterAttributeWrapper caw = acw.getCommand();
        		if (caw != null && caw.getTotalValue() != null && (Integer)caw.getTotalValue() > 0) {
        			setCommandRank((Integer)caw.getTotalValue());
        		} else if (caw != null && caw.getValue() != null && (Integer)caw.getValue() > 0) {
        			setCommandRank((Integer)caw.getValue());
        		}
        	}
        }
        setNationNo(a.getNationNo());
        setCommander(a.getCommanderName());
        setMorale(a.getMorale());
        setTactic(TacticEnum.Standard);
    }
    
    public void setBestTactic() {
    	TacticEnum bestTactic = null;;
    	int bestStr = -1;
    	for (TacticEnum t : TacticEnum.values()) {
    		setTactic(t);
    		int str = Combat.computeNativeArmyStrength(this, HexTerrainEnum.plains, ClimateEnum.Cool, false);
    		if (str > bestStr) {
    			bestTactic = t;
    			bestStr = str;
    		}
    	}
    	setTactic(bestTactic);
    }
    
    public String getCode() {
    	String r = "";
    	r = "CombatArmy ca = new CombatArmy();\n";
    	r += "ca.setNationNo(" + getNationNo() + ");\n";
    	r += "ca.setCommandRank(" + getCommandRank() + ");\n";
    	r += "ca.setCommander('" + getCommander() + "');\n";
    	r += "ca.setMorale(" + getMorale() + ");\n";
    	r += "ca.setDefensiveAddOns(" + getDefensiveAddOns() + ");\n";
    	r += "ca.setOffensiveAddOns(" + getOffensiveAddOns() + ");\n";
    	r += "ArmyElement ae;\n";
    	for (ArmyElementType aet : ArmyElementType.values()) {
    		ArmyElement ae = getArmyElement(aet);
    		if (ae != null && ae.getNumber() > 0) {
    			r += "ae = new ArmyElement();\n";
    			r += "ae.setArmyElementType(ArmyElementType." + ae.getArmyElementType() + ");\n";
    			r += "ae.setNumber(" + ae.getNumber() + ");\n";
    			r += "ae.setTraining(" + ae.getTraining() + ");\n";
    			r += "ae.setWeapons(" + ae.getWeapons() + ");\n";
    			r += "ae.setArmor(" + ae.getArmor() + ");\n";
    			r += "ca.addElement(ae);\n";
    		}
    	}
    	r = r.replace("'", "\"");
    	return r;
    }
    
    public boolean completeInfo() {
    	if (this.computeNumberOfTroops() == 0) return false;
    	if (this.getCommandRank() == 0) return false;
    	if (this.getMorale() == 0) return false;
    	
    	return this.completeTroopElementInfo();
    }
    
    public boolean completeTroopElementInfo() {
    	if (this.getHC().getNumber() != 0) {
    		if(this.getHC().getWeapons() == 0) return false;
    		if(this.getHC().getTraining() == 0) return false;
    	}
        if (this.getLC().getNumber() != 0) {
            if (this.getLC().getWeapons() == 0) return false;
            if (this.getLC().getTraining() == 0) return false;
        }
        if (this.getHI().getNumber() != 0) {
            if (this.getHI().getWeapons() == 0) return false;
            if (this.getHI().getTraining() == 0) return false;
        }
        if (this.getLI().getNumber() != 0) {
            if (this.getLI().getWeapons() == 0) return false;
            if (this.getLI().getTraining() == 0) return false;
        }
        if (this.getAR().getNumber() != 0) {
            if (this.getAR().getWeapons() == 0) return false;
            if (this.getAR().getTraining() == 0) return false;
        }
        if (this.getMA().getNumber() != 0) {
            if (this.getMA().getWeapons() == 0) return false;
            if (this.getMA().getTraining() == 0) return false;
        }
        return true;
    }
    
    public int computeNumberOfTroops() {
    	int ret = 0;
    	for (ArmyElement ae : getElements()) {
    		if (ae.getArmyElementType().isTroop()) {
    			ret += ae.getNumber();
    		}
    	}
    	return ret;
    }
    
}
