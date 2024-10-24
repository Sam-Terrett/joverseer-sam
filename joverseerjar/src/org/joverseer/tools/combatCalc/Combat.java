package org.joverseer.tools.combatCalc;

import java.io.Serializable;
import java.util.ArrayList;

import org.joverseer.domain.Army;
import org.joverseer.domain.ArmyElement;
import org.joverseer.domain.ArmyElementType;
import org.joverseer.domain.ArmyEstimate;
import org.joverseer.domain.ClimateEnum;
import org.joverseer.domain.HexInfo;
import org.joverseer.domain.IHasMapLocation;
import org.joverseer.domain.NationRelations;
import org.joverseer.domain.NationRelationsEnum;
import org.joverseer.domain.PopulationCenter;
import org.joverseer.game.Game;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.metadata.domain.Hex;
import org.joverseer.metadata.domain.HexTerrainEnum;
import org.joverseer.metadata.domain.Nation;
import org.joverseer.metadata.domain.NationAllegianceEnum;
import org.joverseer.support.GameHolder;
import org.joverseer.support.info.InfoUtils;
import org.joverseer.ui.support.dialogs.ErrorDialog;

/**
 * Represents a land combat for the combat calculator.
 * 
 * It holds all information pertinent to the execution of a combat, such as
 * - terrain
 * - climate
 * - involved armies and pop centers
 * - relations
 * 
 * 
 * @author Marios Skounakis
 *
 */
public class Combat implements Serializable, IHasMapLocation {
    private static final long serialVersionUID = 6784272689637435343L;
    public static int MAX_ARMIES = 10;
    public static int MAX_ALL = 11;
    
    HexTerrainEnum terrain;
    ClimateEnum climate;
    
    int hexNo;
    String description;

    CombatArmy[] side1 = new CombatArmy[MAX_ALL];
    CombatArmy[] side2 = new CombatArmy[MAX_ALL];
    CombatArmy[] otherSide = new CombatArmy[MAX_ALL];
    
    NationRelationsEnum[][] side1Relations = new NationRelationsEnum[MAX_ALL][MAX_ALL];
    NationRelationsEnum[][] side2Relations = new NationRelationsEnum[MAX_ALL][MAX_ALL];
     
    boolean[][] side1Attack = new boolean[MAX_ALL][MAX_ALL];
    boolean[][] side2Attack = new boolean[MAX_ALL][MAX_ALL];
    
    CombatPopCenter side1Pc = null;
    CombatPopCenter side2Pc = null;
    
    int rounds = 0;
    
    boolean attackPopCenter = true;

    private int maxRounds = 100 ; ; // Nice high number. Shouldn't ever get to this round.
    
    String log;
    
    public Combat() {
        for (int i=0; i<MAX_ALL; i++) {
            for (int j=0; j<MAX_ALL; j++) {
                this.side1Relations[i][j] = NationRelationsEnum.Disliked;
                this.side2Relations[i][j] = NationRelationsEnum.Disliked;
                this.side1Attack[i][j] = true;
                this.side2Attack[i][j] = true;
            }
            this.side1[i] = null;
            this.side2[i] = null;
        }
        this.terrain = HexTerrainEnum.plains;
        this.climate = ClimateEnum.Cool;
    }
    
    
    
    public int getRounds() {
		return this.rounds;
	}



	public void setRounds(int rounds) {
		this.rounds = rounds;
	}



	public static int computeNativeArmyStrength(CombatArmy ca, HexTerrainEnum terrain, ClimateEnum climate, boolean againstPopCenter) {
        return computeNativeArmyStrength(ca, terrain, climate, null, againstPopCenter);
    }
    
    public static int computeNativeArmyStrength(CombatArmy army, HexTerrainEnum terrain, ClimateEnum climate, Double lossesOverride, boolean againstPopCenter) {
        int strength = 0;
        
        //add up army modifiers
        int armyModifiers = army.getCommandRank() + army.getMorale() + CombatModifiers.getModifierFor(
                army.getNationNo(), terrain, climate) * 2;
        
        int troopModifiers = 0;
        
        for (ArmyElement armyElement : army.getElements()) {
        	int tacticMod = 100;
        	int terrainMod = 100;
        	
            Integer troopStrength = InfoUtils.getTroopStrength(armyElement.getArmyElementType(), "Attack");
            
            if (armyElement.getArmyElementType() == ArmyElementType.WarMachimes && againstPopCenter) {
            	troopStrength = 200;
            }
            
            if (troopStrength == null || troopStrength == 0)
                continue;
            
            if (armyElement.getArmyElementType() != ArmyElementType.WarMachimes) {
                tacticMod = !againstPopCenter ? InfoUtils.getTroopTacticModifier(armyElement.getArmyElementType(), army.getTactic()) : 100;
                terrainMod = InfoUtils.getTroopTerrainModifier(armyElement.getArmyElementType(), terrain);
            }
            
            troopModifiers = armyElement.getTraining() + armyElement.getWeapons() + tacticMod + terrainMod;
            double mod = (double) (armyModifiers + troopModifiers) / 800d;
            
            strength += troopStrength * armyElement.getNumber() * mod;
        }

        if (lossesOverride == null) {
            lossesOverride = army.getLosses();
        }
        
        strength = (int)(strength * (double)(100 - lossesOverride) / 100d);
        return strength;
    }
    
    public static int computNativeArmyConstitution(CombatArmy ca) {
        return computNativeArmyConstitution(ca, null);
    }

    /**
     * 
     * @param combatArmy
     * @param lossesOverride - This reduces the constitution by a percentage. If set to 25 then the constitution is reduced by 25% to 75% of what it would otherwise be. 
     * @return
     */
    public static int computNativeArmyConstitution(CombatArmy combatArmy, Double lossesOverride) {
        int constitution = 0;
        for (ArmyElement combatArmyElement : combatArmy.getElements()) {
            Integer unitConstitution = InfoUtils.getTroopStrength(combatArmyElement.getArmyElementType(), "Defense");
            if (unitConstitution == null)
                continue;
            constitution += unitConstitution * combatArmyElement.getNumber() * (double) (100 + combatArmyElement.getArmor()) / 100d;
        }
        if (lossesOverride == null) {
            lossesOverride = combatArmy.getLosses();
        }
        constitution = (int)(constitution * (double)(100 - lossesOverride) / 100d);
        return constitution;
    }

    public static int computeModifiedArmyStrength(HexTerrainEnum terrain, ClimateEnum climate, CombatArmy att,
            CombatArmy def) {
        int s = computeNativeArmyStrength(att, terrain, climate, false);
        int tacticVsTacticMod = InfoUtils.getTacticVsTacticModifier(att.getTactic(), def.getTactic());
        s = (int) (s * (double) tacticVsTacticMod / 100d);
        return s;
    }

    public static double computeLosses(CombatArmy ca, int enemyStrength) {
        int constit = computNativeArmyConstitution(ca, 0d);
        return Math.min((double)enemyStrength * 100 / (double)Math.max(constit,1), 100);
    }
    
    protected void addToLog(String msg) {
        this.log += msg + "\n";
    }
    
    public void runPcBattle(int attackerSide, int round) {
//        int defenderSide = (attackerSide == 0 ? 1 : 0);
        
        // compute str for attacker
        int warMachines = 0;
        int attackerStr = 0;
        int totalCon = 0;
        CombatPopCenter pc = (attackerSide == 0 ? this.side2Pc : this.side1Pc);
        double[] losses = new double[MAX_ARMIES];
        for (int i=0; i<MAX_ARMIES; i++) {
            if (attackerSide == 0) {
                if (this.side1[i] == null) continue;
                int str = computeNativeArmyStrength(this.side1[i], this.terrain, this.climate, true);
                // adjust for relations
                int relMod = CombatModifiers.getRelationModifier(this.side1Relations[i][MAX_ALL-1]);
                str = (int)(str * (double)relMod / 100d);
                attackerStr += str;
                ArmyElement wmEl = this.side1[i].getWM(); 
                int wm = 0;
                if (wmEl != null) wmEl.getNumber(); 
                warMachines += wm;
                totalCon += computNativeArmyConstitution(this.side1[i]);
                losses[i] = this.side1[i].getLosses();
                if (round == 0) {
                	attackerStr += this.side1[i].getOffensiveAddOns();
                }
            } else {
                if (this.side2[i] == null) continue;
                int str = computeNativeArmyStrength(this.side2[i], this.terrain, this.climate, true);
                // adjust for relations
                int relMod = CombatModifiers.getRelationModifier(this.side2Relations[i][MAX_ALL-1]);
                str = (int)(str * (double)relMod / 100d);
                attackerStr += str;
                warMachines += this.side2[i].getWM().getNumber();
                totalCon += computNativeArmyConstitution(this.side2[i]);
                losses[i] = this.side2[i].getLosses();
                if (round == 0) {
                	attackerStr += this.side2[i].getOffensiveAddOns();
                }
            }
        }
        
        // compute pop center defense and attack
        int popCenterStr = computePopCenterStrength(pc, warMachines);
        pc.setCaptured(popCenterStr <= attackerStr);
        pc.setStrengthOfAttackingArmies(attackerStr);
        for (int i=0; i<MAX_ARMIES; i++) {
            if (attackerSide == 0) {
                if (this.side1[i] == null) continue;
                double l = computeNewLossesFromPopCenter(this.side1[i], pc, this.side2Relations[MAX_ALL - 1][i], totalCon, warMachines, round);
                this.side1[i].setLosses(Math.min(this.side1[i].getLosses() + l, 100));
            } else {
                if (this.side2[i] == null) continue;
                double l = computeNewLossesFromPopCenter(this.side2[i], pc, this.side1Relations[MAX_ALL - 1][i], totalCon, warMachines, round);
                this.side2[i].setLosses(Math.min(this.side2[i].getLosses() + l, 100));
            }
        }
    }

    public void runArmyBattle() {
        this.rounds = 0;
        boolean finished = false;
        this.log = "";
        addToLog("Side 1:");
        for (int i = 0; i < this.side1.length && this.side1[i] != null; i++){
	     	addToLog(this.side1[i].getCommander());
	    }
        addToLog("Side 2:");
        for (int i = 0; i < this.side2.length && this.side2[i] != null; i++){
        	addToLog(this.side2[i].getCommander());
        }
        addToLog("Side Other:");
        for (int i = 0; i < this.otherSide.length && this.otherSide[i] != null; i++){
        	addToLog(this.otherSide[i].getCommander());
        }
        do {
            addToLog("Starting round " + this.rounds);
            double[] side1Losses = new double[MAX_ARMIES];
            double[] side2Losses = new double[MAX_ARMIES];

            // compute constitution for each side
            int side1Con = 0;
            int side2Con = 0;
            for (int i=0; i<MAX_ARMIES; i++) {
                CombatArmy army = this.side1[i];
                if (army == null) continue;
                int currentConstitution = computNativeArmyConstitution(army);
                int originalConstitution = computNativeArmyConstitution(army, 0d);
                addToLog("Side 1, army " + i + " con: " + currentConstitution + "/" + originalConstitution);
                side1Con += currentConstitution;
                side1Losses[i] = army.getLosses();
            }
            addToLog("Total Side 1 con: " + side1Con);
            addToLog("");
            for (int i=0; i<MAX_ARMIES; i++) {
                CombatArmy army = this.side2[i];
                if (army == null) continue;
                int constit = computNativeArmyConstitution(army);
                int sconstit = computNativeArmyConstitution(army, 0d);
                addToLog("Side 2, army " + i + " con: " + constit + "/" + sconstit);
                side2Con += constit;
                side2Losses[i] = army.getLosses();
            }
            addToLog("Total Side 2 con: " + side2Con);
            side1Con = Math.max(side1Con, 1);
            side2Con = Math.max(side2Con, 1);
            //if (side1Con == 1 || side2Con == 1) return;
            
            addToLog("");

            boolean side1Alive = false;
            boolean side2Alive = false;
            // compute losses for each army
            for (int i=0; i<MAX_ARMIES; i++) {
                CombatArmy ca1 = this.side1[i];
                if (ca1 == null) continue;
                
                for (int j=0; j<MAX_ARMIES; j++) {
                    CombatArmy ca2 = this.side2[j];
                    if (ca2 == null) continue;
                    
                    // losses for ca1
                    addToLog("");
                    addToLog("Computing Losses for 2," + j + " attacking 1," + i);
                    side1Losses[i] += computeNewLosses(this.terrain, this.climate, ca2, ca1, this.side2Relations[j][i], side1Con, this.rounds);
                    if (side1Losses[i] < 99.5) {
                        side1Alive = true;
                    }
                    
                    // losses for ca2
                    addToLog("");
                    addToLog("Computing Losses for 1," + i + " attacking 2," + j);
                    side2Losses[j] += computeNewLosses(this.terrain, this.climate, ca1, ca2, this.side1Relations[i][j], side2Con, this.rounds);
                    if (side2Losses[j] < 99.5) {
                        side2Alive = true;
                    }
                }
            }
            
            // assign losses to armies
            for (int i=0; i<MAX_ARMIES; i++) {
                CombatArmy ca1 = this.side1[i];
                if (ca1 == null) continue;
                ca1.setLosses(Math.min(side1Losses[i], 100));
                addToLog("Side 1 army " + i + " new con : " + computNativeArmyConstitution(ca1));
            }
            for (int i=0; i<MAX_ARMIES; i++) {
                CombatArmy ca2 = this.side2[i];
                if (ca2 == null) continue;
                ca2.setLosses(Math.min(side2Losses[i], 100));
                addToLog("Side 2 army " + i + " new con : " + computNativeArmyConstitution(ca2));
            }        
            
            this.rounds++;
            finished = !(side1Alive && side2Alive) || this.rounds >= this.maxRounds;
            addToLog("");
            addToLog("");
        } while (!finished);
        //System.out.println(this.log);
    }
    
    public void runWholeCombat() {
    	runArmyBattle();
    	if (getAttackPopCenter() && getSide2Pc() != null) runPcBattle(0, 1);
    }
    
    public int computePopCenterStrength(CombatPopCenter pc) {
        return computePopCenterStrength(pc, 0);
    }
    
    public int computePopCenterStrength(CombatPopCenter pc, int numberOfWarMachines) {
        int popDef = new int[]{0, 200, 500, 1000, 2500, 5000}[pc.getSize().getCode()];
        int fortDef = new int[]{0, 2000, 6000, 10000, 16000, 24000}[pc.getFort().getSize()];
        int wmStr = numberOfWarMachines * 200;
        fortDef = Math.max(fortDef - wmStr, 0);
        return (int)Math.round(((double)popDef + (double)fortDef) * (100d + (double)pc.getLoyalty()) / 100d);
    }
    
    public double computeNewLossesFromPopCenter(CombatArmy army,
            CombatPopCenter pc,
            NationRelationsEnum relations,
            int armySideTotalCon,
            int armyTotalWMs,
            int round) {
    	
    	this.log = "";
    	
        int relMod = CombatModifiers.getRelationModifier(relations);
        int defCon = computNativeArmyConstitution(army);
        int attStr = computePopCenterStrength(pc, armyTotalWMs);
        int defBonus = 0;
        // adjust by relations
        attStr = (int)((double)attStr * (double)relMod / 100d);
        // handle first round
        if (round == 0) {
            defBonus = army.getDefensiveAddOns();
        }
        double lossesFactor = (double)defCon / (double)armySideTotalCon;
        addToLog("Defender loss factor: " + lossesFactor);
        attStr = (int)(attStr * lossesFactor) - defBonus;
        if (attStr < 0) attStr = 0;
        double losses = computeLosses(army, attStr);
        addToLog("New losses: " + losses);
        //System.out.println(this.log);
        return losses;
    }
    
    public double computeNewLosses(HexTerrainEnum terrain1,
                                            ClimateEnum climate1,
                                            CombatArmy att,
                                            CombatArmy def,
                                            NationRelationsEnum relations,
                                            int defenderSideTotalCon,
                                            int round) {
        int relMod = CombatModifiers.getRelationModifier(relations);
        int defCon = computNativeArmyConstitution(def);
        int attStr = computeModifiedArmyStrength(terrain1, climate1, att, def);
        addToLog("Relations mod: " + relMod);
        addToLog("Attacker modified str: " + attStr);
        int attBonus = 0;
        int defBonus = 0;
        // adjust by relations
        attStr = (int)((double)attStr * (double)relMod / 100d);
        // handle first round
        if (round == 0) {
            attBonus = att.getOffensiveAddOns();
            defBonus = def.getDefensiveAddOns();
            attStr += attBonus;
            addToLog("First round - str: " + attBonus + " con: " + defBonus);
        }
        double lossesFactor = (double)defCon / (double)defenderSideTotalCon;
        addToLog("Defender loss factor: " + lossesFactor);
        attStr = (int)(attStr * lossesFactor) - defBonus;
        if (attStr < 0) attStr = 0;
        double losses = computeLosses(def, attStr);
        addToLog("New losses: " + losses);
        return losses;
    }

    
    public ClimateEnum getClimate() {
        return this.climate;
    }

    
    public void setClimate(ClimateEnum climate) {
        this.climate = climate;
    }

    
    public CombatArmy[] getSide1() {
        return this.side1;
    }

    
    public void setSide1(CombatArmy[] side1) {
        this.side1 = side1;
    }

    
    public boolean[][] getSide1Attack() {
        return this.side1Attack;
    }

    
    public void setSide1Attack(boolean[][] side1Attack) {
        this.side1Attack = side1Attack;
    }

    
    public NationRelationsEnum[][] getSide1Relations() {
        return this.side1Relations;
    }

    
    public void setSide1Relations(NationRelationsEnum[][] side1Relations) {
        this.side1Relations = side1Relations;
    }

    
    public CombatArmy[] getSide2() {
        return this.side2;
    }

    
    public void setSide2(CombatArmy[] side2) {
        this.side2 = side2;
    }

    
    public boolean[][] getSide2Attack() {
        return this.side2Attack;
    }

    
    public void setSide2Attack(boolean[][] side2Attack) {
        this.side2Attack = side2Attack;
    }

    
    public NationRelationsEnum[][] getSide2Relations() {
        return this.side2Relations;
    }

    
    public void setSide2Relations(NationRelationsEnum[][] side2Relations) {
        this.side2Relations = side2Relations;
    }

    public CombatArmy[] getOtherSide() {
    	return this.otherSide;
    }
    
    public HexTerrainEnum getTerrain() {
        return this.terrain;
    }
    
    public void setTerrain(HexTerrainEnum terrain) {
        this.terrain = terrain;
    }

    public boolean addToSide(int side, CombatArmy ca) {
        if (side == 0) {
            for (int i=0; i<this.side1.length; i++) {
                if (this.side1[i] == null) {
                    this.side1[i] = ca;
                    return true;
                }
            }
            return false;
        } else if (side == 1){
            for (int i=0; i<this.side2.length; i++) {
                if (this.side2[i] == null) {
                    this.side2[i] = ca;
                    return true;
                }
            }
            return false;
        } else {
            for (int i=0; i<this.otherSide.length; i++) {
                if (this.otherSide[i] == null) {
                    this.otherSide[i] = ca;
                    return true;
                }
            }
            return false;
        }
    }

    public boolean removeFromSide(int side, CombatArmy ca) {
        boolean found = false;
        if (side == 0) {
            for (int i=0; i<this.side1.length; i++) {
                if (this.side1[i] == ca) {
                    this.side1[i] = null;
                    found = true;
                }
                if (i > 0 && this.side1[i-1] == null && this.side1[i] != null) {
                    this.side1[i-1] = this.side1[i];
                    this.side1[i] = null;
                }
            }
            return found;
        } else if(side == 1){
            for (int i=0; i<this.side2.length; i++) {
                if (this.side2[i] == ca) {
                    this.side2[i] = null;
                    found = true;
                }
                if (i > 0 && this.side2[i-1] == null && this.side2[i] != null) {
                    this.side2[i-1] = this.side2[i];
                    this.side2[i] = null;
                }
            }
            return found;

        } else {
            for (int i=0; i<this.otherSide.length; i++) {
                if (this.otherSide[i] == ca) {
                    this.otherSide[i] = null;
                    found = true;
                }
                if (i > 0 && this.otherSide[i-1] == null && this.otherSide[i] != null) {
                    this.otherSide[i-1] = this.otherSide[i];
                    this.otherSide[i] = null;
                }
            }
            return found;

        }
    }
    
    public NationAllegianceEnum estimateAllegianceForSide(int side) {
        NationAllegianceEnum ret = null;
        Game g = GameHolder.instance().getGame();
        CombatArmy[] cas = (side == 0 ? this.side1 : this.side2);
        for (CombatArmy ca : cas) {
            if (ca == null) continue;
            if (ca.getNationNo() > 0) {
                NationRelations nr = g.getTurn().getNationRelations(ca.getNationNo());
                if (nr != null) {
                    if (ret == null) {
                        ret = nr.getAllegiance();
                    } else if (nr.getAllegiance() != ret) {
                        ret = null;
                        return ret;
                    } else {
                        // allegiances match, do nothing
                    }
                }
            }
        }
        // no allegiance given
        return ret;
    }

    
    public int getMaxRounds() {
        return this.maxRounds;
    }

    
    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    
    public String getDescription() {
        return this.description;
    }

    
    public void setDescription(String description) {
        this.description = description;
    }

    
    public int getHexNo() {
        return this.hexNo;
    }

    
    public void setHexNo(int hexNo) {
        this.hexNo = hexNo;
    }
    
    
    
    public int getArmyIndex(int side, CombatArmy a) {
        CombatArmy[] cas = (side == 0 ? this.side1 : this.side2);
        for (int i=0; i<cas.length; i++) {
            if (cas[i] == a) return i;
        }
        return -1;
    }
    
    @Override
	public int getX() {
    	return getHexNo() / 100;
    }
    
    @Override
	public int getY() {
    	return getHexNo() % 100;
    }

    
    public CombatPopCenter getSide1Pc() {
        return this.side1Pc;
    }

    
    public void setSide1Pc(CombatPopCenter side1Pc) {
        this.side1Pc = side1Pc;
    }

    
    public CombatPopCenter getSide2Pc() {
        return this.side2Pc;
    }

    
    public void setSide2Pc(CombatPopCenter side2Pc) {
        this.side2Pc = side2Pc;
    }

    public void loadTerrainAndClimateFromHex() {
    	HexInfo hi = GameHolder.instance().getGame().getTurn().getHexInfo(getHexNo());
    	if (hi != null) {
    		setClimate(hi.getClimate());
    	}
    	Hex hex = GameHolder.instance().getGame().getMetadata().getHex(getHexNo());
    	if (hex != null) {
    		setTerrain(hex.getTerrain());
    	}
    }
    
    public void setArmiesAndPCFromHex() {
		String strHex = String.valueOf(this.hexNo);
		//ArrayList<Army> allarmies = game.getTurn().getContainer(TurnElementsEnum.Army).findAllByProperties(new String[] { "hexNo" }, new Object[] { strHex });
		ArrayList<Army> fparmies = GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Army).findAllByProperties(new String[] { "hexNo", "nationAllegiance" }, new Object[] { strHex, NationAllegianceEnum.FreePeople });
		ArrayList<Army> dsarmies = GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Army).findAllByProperties(new String[] { "hexNo", "nationAllegiance" }, new Object[] { strHex, NationAllegianceEnum.DarkServants });
		ArrayList<Army> ntarmies = GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.Army).findAllByProperties(new String[] { "hexNo", "nationAllegiance" }, new Object[] { strHex, NationAllegianceEnum.Neutral });

//		if (ntarmies.size() > 0) {
//			ErrorDialog.showErrorDialog("createCombatForHexCommand.error.NetrualArmiesFound");
//		}

		PopulationCenter pc = (PopulationCenter) GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.PopulationCenter).findFirstByProperty("hexNo", this.hexNo);
//		if (pc != null && (pc.getNationNo() == 0 || pc.getNation().getAllegiance().equals(NationAllegianceEnum.Neutral))) {
//			ErrorDialog.showErrorDialog("createCombatForHexCommand.error.PopWithUnknownOrNeutralNationFound");
//		}

		ArrayList<Army> aside1;
		ArrayList<Army> aside2;
		ArrayList<Army> other = ntarmies;

		if (pc != null && pc.getNation().getAllegiance().equals(NationAllegianceEnum.FreePeople)) {
			aside2 = fparmies;
			aside1 = dsarmies;
		} else {
			aside1 = fparmies;
			aside2 = dsarmies;
		}
		
		for (int i=0; i<MAX_ALL; i++) {
	        this.side1[i] = null;
	        this.side2[i] = null;
	        this.otherSide[i] = null;
		}

		for (int i = 0; i < 3; i++) {
			ArrayList<Army> sideArmies;
			if (i != 2) sideArmies = i == 0 ? aside1 : aside2;
			else sideArmies = other;
			
			for (Army a : sideArmies) {
				CombatArmy ca;
				if (a.computeNumberOfMen() > 0) {
					//System.out.println("Adding Known Army "+a.getCommanderName());
					ca = new CombatArmy(a);
				} else {
					ArmyEstimate ae = (ArmyEstimate) GameHolder.instance().getGame().getTurn().getContainer(TurnElementsEnum.ArmyEstimate).findFirstByProperty("commanderName", a.getCommanderName());
					if (ae != null) {
						//System.out.println("Adding Army Estimate "+ae.getCommanderName());
						ca = new CombatArmy(ae);

					} else {
						//System.out.println("Adding Zero Army "+a.getCommanderName() + " troops "+a.getTroopCount());
						ca = new CombatArmy(a);

					}
				}
				this.addToSide(i, ca);
				ca.setBestTactic();
			}
		}

		if (pc != null) {
			CombatPopCenter cpc = new CombatPopCenter(pc);
			this.setSide2Pc(cpc);
		}
    }
    
    public void autoSetRelationsToHated() {
    	
    	for (int i=0; i<MAX_ALL; i++) {
    		for (int j=0; j<MAX_ALL; j++) {
    			this.side1Relations[i][j] = NationRelationsEnum.Hated;
    			this.side2Relations[i][j] = NationRelationsEnum.Hated;
    		}
    	}
    }

    public void autoDetectCombatArmyRelations(int side, int caInd) {
    	NationRelations nR = null;
    	if (side == 0) nR = GameHolder.instance().getGame().getTurn().getNationRelations(this.side1[caInd].getNationNo());
    	else if(side == 1) nR = GameHolder.instance().getGame().getTurn().getNationRelations(this.side2[caInd].getNationNo());
    	
    	if (side == 0 && nR != null) {
	    	for (int i = 0; i < MAX_ARMIES && this.side2[i] != null; i++) {
	    		this.side1Relations[caInd][i] = nR.getRelationsFor(this.side2[i].getNationNo());
	    		System.out.println(this.side1[caInd].getNation().getName() + " to " + this.side2[i].getNation().getName() + ": " +  nR.getRelationsFor(this.side2[i].getNationNo()));
	    	}
	    	if (this.side2Pc != null) this.side1Relations[caInd][10] = nR.getRelationsFor(this.side2Pc.getNationNo());
	    	if (this.side2Pc != null) System.out.println(nR.getRelationsFor(this.side2Pc.getNationNo()));
    	} else if (side == 1 && nR != null){
	    	for (int i = 0; i < MAX_ARMIES&& this.side1[i] != null; i++) {
	    		this.side2Relations[caInd][i] = nR.getRelationsFor(this.side1[i].getNationNo());
	    	}
	    	if (this.side1Pc != null) this.side2Relations[caInd][10] = nR.getRelationsFor(this.side1Pc.getNationNo());
    	}
    }
    
    public void autoSetCombatRelations() {
    	//Side1 relations
    	System.out.println("REACHED");
    	for (int i = 0; i < MAX_ARMIES && this.side1[i] != null; i++) {
    		this.autoDetectCombatArmyRelations(0, i);
    	}
    	
    	//Side2 relations
   		for (int i = 0; i < MAX_ARMIES && this.side2[i] != null; i++) {
    		this.autoDetectCombatArmyRelations(1, i);
    	}
    }

	public boolean getAttackPopCenter() {
		return this.attackPopCenter;
	}



	public void setAttackPopCenter(boolean attackPopCenter) {
		this.attackPopCenter = attackPopCenter;
	}
	
	
    
}
