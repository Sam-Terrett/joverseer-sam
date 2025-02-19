package org.joverseer.support.readers.newXml;


import java.util.ArrayList;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RegexRules;
import org.apache.commons.digester.SetNestedPropertiesRule;
import org.apache.commons.digester.SimpleRegexMatcher;
import org.apache.log4j.Logger;
import org.joverseer.JOApplication;
import org.joverseer.domain.Army;
import org.joverseer.domain.ArmyElementType;
import org.joverseer.domain.ArmyEstimate;
import org.joverseer.domain.ArmySizeEnum;
import org.joverseer.domain.Challenge;
import org.joverseer.domain.Character;
import org.joverseer.domain.ClimateEnum;
import org.joverseer.domain.Combat;
import org.joverseer.domain.Company;
import org.joverseer.domain.Diplo;
import org.joverseer.domain.Encounter;
import org.joverseer.domain.FortificationSizeEnum;
import org.joverseer.domain.HexInfo;
import org.joverseer.domain.InformationSourceEnum;
import org.joverseer.domain.NationRelations;
import org.joverseer.domain.Order;
import org.joverseer.domain.PlayerInfo;
import org.joverseer.domain.PopulationCenter;
import org.joverseer.domain.PopulationCenterSizeEnum;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.metadata.GameMetadata;
import org.joverseer.metadata.GameTypeEnum;
import org.joverseer.metadata.SNAEnum;
import org.joverseer.metadata.domain.ArtifactInfo;
import org.joverseer.metadata.domain.ClimateModifier;
import org.joverseer.metadata.domain.HexTerrainEnum;
import org.joverseer.metadata.domain.Nation;
import org.joverseer.metadata.domain.NationAllegianceEnum;
import org.joverseer.metadata.domain.TerrainModifier;
import org.joverseer.support.StringUtils;
import org.joverseer.support.Container;
import org.joverseer.support.infoSources.DerivedFromOrderResultsInfoSource;
import org.joverseer.support.infoSources.DoubleAgentInfoSource;
import org.joverseer.support.infoSources.InfoSource;
import org.joverseer.support.infoSources.PdfTurnInfoSource;
import org.joverseer.support.infoSources.XmlExtraTurnInfoSource;
import org.joverseer.support.readers.pdf.CombatWrapper;
import org.joverseer.support.readers.pdf.OrderResult;
import org.springframework.richclient.progress.ProgressMonitor;

public class TurnNewXmlReader implements Runnable {
	static Logger logger = Logger.getLogger(TurnNewXmlReader.class);

	TurnInfo turnInfo = null;
	Digester digester = null;
	Turn turn = null;
	InfoSource infoSource = null;
	ArrayList<PopulationCenter> currentNationPops = new ArrayList<PopulationCenter>();
	Game game;
	String filename;
	ProgressMonitor monitor;
	boolean errorOccured = false;
	int nationNo;

	public TurnNewXmlReader(Game game, String filename, int nationNo) {
		this.game = game;
		this.filename = filename;
		this.nationNo = nationNo;
	}

	public ProgressMonitor getMonitor() {
		return this.monitor;
	}

	public void setMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
	}

	public boolean getErrorOccured() {
		return this.errorOccured;
	}

	public void setErrorOccured(boolean errorOccured) {
		this.errorOccured = errorOccured;
	}

	public void readFile(String fileName) throws Exception {
		try {
			SetNestedPropertiesRule snpr;

			this.digester = new Digester();
			this.digester.setValidating(false);
			this.digester.setRules(new RegexRules(new SimpleRegexMatcher()));
			this.digester.addObjectCreate("METurn", TurnInfo.class);
			// parse properties
			// set season changing
			this.digester.addSetProperties("METurn/More/TurnInfo/Season", "changing", "seasonChanging");
			// set nested properties
			this.digester.addRule("METurn/More/TurnInfo", snpr = new SetNestedPropertiesRule(new String[] { "Season", "NationAlignment", "NPCsRecruited" }, new String[] { "season", "alignment", "NPCsRecruited"}));
			snpr.setAllowUnknownChildElements(true);

			this.digester.addObjectCreate("METurn/More/TurnInfo/GameInfo", "org.joverseer.support.Container");
			this.digester.addSetNext("METurn/More/TurnInfo/GameInfo", "setGameInfo");
			this.digester.addObjectCreate("METurn/More/TurnInfo/GameInfo/Option", "org.joverseer.support.readers.newXml.GameInfoOptionWrapper");
			this.digester.addSetProperties("METurn/More/TurnInfo/GameInfo/Option", "name", "name");
			this.digester.addSetProperties("METurn/More/TurnInfo/GameInfo/Option", "value", "value");
			this.digester.addSetNext("METurn/More/TurnInfo/GameInfo/Option", "addItem", "org.joverseer.support.readers.newXml.GameInfoOptionWrapper");

			this.digester.addObjectCreate("METurn/More/TurnInfo/Modifiers", "org.joverseer.support.Container");
			this.digester.addSetNext("METurn/More/TurnInfo/Modifiers", "setModifiers");
			this.digester.addObjectCreate("METurn/More/TurnInfo/Modifiers/Modifier", "org.joverseer.support.readers.newXml.TurnInfoModifierWrapper");
			this.digester.addSetProperties("METurn/More/TurnInfo/Modifiers/Modifier", "climate", "climate");
			this.digester.addSetProperties("METurn/More/TurnInfo/Modifiers/Modifier", "terrain", "terrain");
			this.digester.addCallMethod("METurn/More/TurnInfo/Modifiers/Modifier", "setModifier", 1);
			this.digester.addCallParam("METurn/More/TurnInfo/Modifiers/Modifier", 0);			
			this.digester.addSetNext("METurn/More/TurnInfo/Modifiers/Modifier", "addItem", "org.joverseer.support.readers.newXml.TurnInfoModifierWrapper");

			
			// parse PCs from old format.
			// create container for pcs
			this.digester.addObjectCreate("METurn/PopCentres", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/PopCentres", "setOldPopCentres");
			// create pc
			this.digester.addObjectCreate("METurn/PopCentres/PopCentre", "org.joverseer.support.readers.xml.PopCenterWrapper");
			// set hex
			this.digester.addSetProperties("METurn/PopCentres/PopCentre", "HexID", "hexID");
			// set nested properties
			this.digester.addRule("METurn/PopCentres/PopCentre", snpr = new SetNestedPropertiesRule(new String[] { "Name", "Nation", "NationAllegience", "Size", "FortificationLevel", "Size", "Dock", "Capital", "Hidden", "Loyalty", "InformationSource", "Hidden" }, new String[] { "name", "nation", "nationAllegience", "size", "fortificationLevel", "size", "dock", "capital", "hidden", "loyalty", "informationSource", "hidden" }));
			// add to container
			this.digester.addSetNext("METurn/PopCentres/PopCentre", "addItem", "org.joverseer.support.readers.xml.PopCenterWrapper");

			// create container for Hostages
			this.digester.addObjectCreate("METurn/Hostages", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/Hostages", "setHostages");
			// create hostage wrapper
			this.digester.addObjectCreate("METurn/Hostages/Hostage", "org.joverseer.support.readers.newXml.HostageWrapper");
			// set id
			this.digester.addSetProperties("METurn/Hostages/Hostage", "NameID", "nameId");
			// set nested properties
			this.digester.addRule("METurn/Hostages/Hostage", snpr = new SetNestedPropertiesRule(new String[] { "Nation", "HeldBy", "Location" }, new String[] { "nation", "heldBy", "location" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/Hostages/Hostage", "addItem", "org.joverseer.support.readers.newXml.HostageWrapper");

			// create container for Double Agents
			this.digester.addObjectCreate("METurn/DoubleAgents", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/DoubleAgents", "setDoubleAgents");
			// create hostage wrapper
			this.digester.addObjectCreate("METurn/DoubleAgents/DoubleAgent", "org.joverseer.support.readers.newXml.DoubleAgentWrapper"); // set
			// id
			this.digester.addSetProperties("METurn/DoubleAgents/DoubleAgent", "NameID", "name");
			// set nested properties
			this.digester.addRule("METurn/DoubleAgents/DoubleAgent", snpr = new SetNestedPropertiesRule(new String[] { "Nation", "Line", "Location" }, new String[] { "nation", "report", "hexNo" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/DoubleAgents/DoubleAgent", "addItem", "org.joverseer.support.readers.newXml.DoubleAgentWrapper");

			// create container for Non Hidden Artifactss
			this.digester.addObjectCreate("METurn/ArtifactInfo/NonHiddenArtifacts", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/ArtifactInfo/NonHiddenArtifacts", "setNonHiddenArtifacts");
			// create artifact wrapper
			this.digester.addObjectCreate("METurn/ArtifactInfo/NonHiddenArtifacts/Artifact", "org.joverseer.support.readers.newXml.ArtifactWrapper");
			// set id
			this.digester.addSetProperties("METurn/ArtifactInfo/NonHiddenArtifacts/Artifact", "ID", "id");
			// set nested properties
			this.digester.addRule("METurn/ArtifactInfo/NonHiddenArtifacts/Artifact", snpr = new SetNestedPropertiesRule(new String[] { "Name", "MageSkill", "CommandSkill", "EmmisarySkill", "AgentSkill", "StealthSkill", "CombatSkill", "Alignment", "Latent", "Item" }, new String[] { "name", "mage", "command", "emissary", "agent", "stealth", "combat", "alignment", "latent", "item" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/ArtifactInfo/NonHiddenArtifacts/Artifact", "addItem", "org.joverseer.support.readers.newXml.ArtifactWrapper");
			// create container for Hidden Artifactss
			this.digester.addObjectCreate("METurn/ArtifactInfo/HiddenArtifacts", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/ArtifactInfo/HiddenArtifacts", "setHiddenArtifacts");
			// create artifact wrapper
			this.digester.addObjectCreate("METurn/ArtifactInfo/HiddenArtifacts/Artifact", "org.joverseer.support.readers.newXml.ArtifactWrapper");
			// set id
			this.digester.addSetProperties("METurn/ArtifactInfo/HiddenArtifacts/Artifact", "ID", "id");
			// set nested properties
			this.digester.addRule("METurn/ArtifactInfo/HiddenArtifacts/Artifact", snpr = new SetNestedPropertiesRule(new String[] { "Name", "MageSkill", "CommandSkill", "EmmisarySkill", "AgentSkill", "StealthSkill", "CombatSkill", "Alignment", "Latent", "Item" }, new String[] { "name", "mage", "command", "emissary", "agent", "stealth", "combat", "alignment", "latent", "item" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/ArtifactInfo/HiddenArtifacts/Artifact", "addItem", "org.joverseer.support.readers.newXml.ArtifactWrapper");

			// create container for Recons
			this.digester.addObjectCreate("METurn/Recons", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/Recons", "setRecons");
			// create recon wrapper
			this.digester.addObjectCreate("METurn/Recons/Recon", "org.joverseer.support.readers.newXml.ReconWrapper");
			// add to container
			this.digester.addSetNext("METurn/Recons/Recon", "addItem", "org.joverseer.support.readers.newXml.ReconWrapper");
			// create recon hex wrapper
			this.digester.addObjectCreate("METurn/Recons/Recon/Hex", "org.joverseer.support.readers.newXml.HexWrapper");
			// set nested properties
			this.digester.addRule("METurn/Recons/Recon/Hex", snpr = new SetNestedPropertiesRule(new String[] { "HexID", "Terrain", "PopCenterSize", "Forts", "Ports", "Roads", "Bridges", "Fords", "MinorRivers", "MajorRivers" }, new String[] { "hexID", "terrain", "popCenterSize", "forts", "ports", "roads", "bridges", "fords", "minorRivers", "majorRivers" }));
			snpr.setAllowUnknownChildElements(true);
			// add to recon wrapper
			this.digester.addSetNext("METurn/Recons/Recon/Hex", "addHexWrapper", "org.joverseer.support.readers.newXml.HexWrapper");

			// create container for Pop Centers
			this.digester.addObjectCreate("METurn/More/PopCentres", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/More/PopCentres", "setPopCentres");
			// create pop center wrapper
			this.digester.addObjectCreate("METurn/More/PopCentres/PopCentre", "org.joverseer.support.readers.newXml.PopCenterWrapper");
			// set hex no
			this.digester.addSetProperties("METurn/More/PopCentres/PopCentre", "HexID", "hexNo");
			// set nested properties
			this.digester.addRule("METurn/More/PopCentres/PopCentre", snpr = new SetNestedPropertiesRule(new String[] { "Sieged", "Terrain", "Climate" }, new String[] { "sieged", "terrain", "climate" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/More/PopCentres/PopCentre", "addItem", "org.joverseer.support.readers.newXml.PopCenterWrapper");
			// create production wrapper
			this.digester.addObjectCreate("METurn/More/PopCentres/PopCentre/Product", "org.joverseer.support.readers.newXml.ProductionWrapper");
			// set type
			this.digester.addSetProperties("METurn/More/PopCentres/PopCentre/Product", "type", "type");
			// set nested properties
			this.digester.addRule("METurn/More/PopCentres/PopCentre/Product", snpr = new SetNestedPropertiesRule(new String[] { "CurrentStores", "ExpProduction" }, new String[] { "currentStores", "expProduction" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/More/PopCentres/PopCentre/Product", "addProduct", "org.joverseer.support.readers.newXml.ProductionWrapper");
			// add foreign characters
			this.digester.addCallMethod("METurn/More/PopCentres/PopCentre/ForeignCharacters/ForeignCharacter", "addForeignCharacter", 1);
			this.digester.addCallParam("METurn/More/PopCentres/PopCentre/ForeignCharacters/ForeignCharacter", 0);

			// create container for Nation Relations
			this.digester.addObjectCreate("METurn/NationRelations", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/NationRelations", "setNationRelations");
			// create nation relation wrapper
			this.digester.addObjectCreate("METurn/NationRelations/NationRelation", "org.joverseer.support.readers.newXml.NationRelationWrapper");
			// set hex no
			this.digester.addSetProperties("METurn/NationRelations/NationRelation", "ID", "nationNo");
			// set relation
			this.digester.addCallMethod("METurn/NationRelations/NationRelation", "setRelation", 1);
			this.digester.addCallParam("METurn/NationRelations/NationRelation", 0);
			// add to container
			this.digester.addSetNext("METurn/NationRelations/NationRelation", "addItem", "org.joverseer.support.readers.newXml.NationRelationWrapper");

			// create container for SNAs
			this.digester.addObjectCreate("METurn/SpecialNationAbilities", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/SpecialNationAbilities", "setSnas");
			// create SNA wrapper
			this.digester.addObjectCreate("METurn/SpecialNationAbilities/Ability", "org.joverseer.support.readers.newXml.SNAWrapper");
			// set code
			this.digester.addRule("METurn/SpecialNationAbilities/Ability", snpr = new SetNestedPropertiesRule(new String[] { "Code", }, new String[] { "code", }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/SpecialNationAbilities/Ability", "addItem", "org.joverseer.support.readers.newXml.SNAWrapper");

			// create container for Companies
			this.digester.addObjectCreate("METurn/Companies", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/Companies", "setCompanies");
			// create pop center wrapper
			this.digester.addObjectCreate("METurn/Companies/Company", "org.joverseer.support.readers.newXml.CompanyWrapper");
			// set hex no
			this.digester.addSetProperties("METurn/Companies/Company", "HexID", "hexNo");
			// set nested properties
			this.digester.addRule("METurn/Companies/Company", snpr = new SetNestedPropertiesRule(new String[] { "CompanyCO" }, new String[] { "commander" }));
			snpr.setAllowUnknownChildElements(true);
			// set members
			this.digester.addCallMethod("METurn/Companies/Company/CompanyMember", "addMember", 1);
			this.digester.addCallParam("METurn/Companies/Company/CompanyMember", 0);
			// add to container
			this.digester.addSetNext("METurn/Companies/Company", "addItem", "org.joverseer.support.readers.newXml.CompanyWrapper");

			// create container for armies
			this.digester.addObjectCreate("METurn/More/Armies", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/More/Armies", "setArmies");
			// create army wrapper
			this.digester.addObjectCreate("METurn/More/Armies/Army", "org.joverseer.support.readers.newXml.ArmyWrapper");
			// set commander
			this.digester.addSetProperties("METurn/More/Armies/Army", "Commander", "commander");
			// set nested properties
			this.digester.addRule("METurn/More/Armies/Army", snpr = new SetNestedPropertiesRule(new String[] { "Food", "Morale", "Warships", "Transports", "Warships", "Climate", "Warmachines" }, new String[] { "food", "morale", "warships", "transports", "warships", "climate", "warmachines" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/More/Armies/Army", "addItem", "org.joverseer.support.readers.newXml.ArmyWrapper");

			// create army regiment wrapper
			this.digester.addObjectCreate("METurn/More/Armies/Army/Troops", "org.joverseer.support.readers.newXml.ArmyRegimentWrapper");
			// set troop type
			this.digester.addSetProperties("METurn/More/Armies/Army/Troops", "Type", "troopType");
			// set nested properties
			this.digester.addRule("METurn/More/Armies/Army/Troops", snpr = new SetNestedPropertiesRule(new String[] { "Number", "Training", "Weapons", "Armor", "Description" }, new String[] { "number", "training", "weapons", "armor", "description" }));
			snpr.setAllowUnknownChildElements(true);
			// add regiment to army
			this.digester.addSetNext("METurn/More/Armies/Army/Troops", "addRegiment", "org.joverseer.support.readers.newXml.ArmyRegimentWrapper");

			// create container for anchored ships
			this.digester.addObjectCreate("METurn/AnchoredShips", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/AnchoredShips", "setAnchoredShips");
			// create army wrapper
			this.digester.addObjectCreate("METurn/AnchoredShips/Ships", "org.joverseer.support.readers.newXml.AnchoredShipsWrapper");
			// set hex no
			this.digester.addSetProperties("METurn/AnchoredShips/Ships", "HexID", "hexId");
			// set nested properties
			this.digester.addRule("METurn/AnchoredShips/Ships", snpr = new SetNestedPropertiesRule(new String[] { "Transports", "Warships" }, new String[] { "transports", "warships" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/AnchoredShips/Ships", "addItem", "org.joverseer.support.readers.newXml.AnchoredShipsWrapper");

			// create container for challenges
			this.digester.addObjectCreate("METurn/Challenges", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/Challenges", "setChallenges");
			// create challenge wrappper
			this.digester.addObjectCreate("METurn/Challenges/Challenge", "org.joverseer.support.readers.newXml.ChallengeWrapper");
			// add line
			this.digester.addCallMethod("METurn/Challenges/Challenge/Lines/Line", "addLine", 1);
			this.digester.addCallParam("METurn/Challenges/Challenge/Lines/Line", 0);
			// add to container
			this.digester.addSetNext("METurn/Challenges/Challenge", "addItem", "org.joverseer.support.readers.newXml.ChallengeWrapper");

			// create container for battles
			this.digester.addObjectCreate("METurn/BattleReports", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/BattleReports", "setBattles");
			// create battle wrapper
			this.digester.addObjectCreate("METurn/BattleReports/BattleReport", "org.joverseer.support.readers.newXml.BattleWrapper");
			// create line
			this.digester.addObjectCreate("METurn/BattleReports/BattleReport/Lines/Line", "org.joverseer.support.readers.newXml.BattleLine");
			// set text
			this.digester.addCallMethod("METurn/BattleReports/BattleReport/Lines/Line", "setText", 1);
			this.digester.addCallParam("METurn/BattleReports/BattleReport/Lines/Line", 0);
			// set nested properties
			this.digester.addRule("METurn/BattleReports/BattleReport/Lines/Line", snpr = new SetNestedPropertiesRule(new String[] { "CommanderReport", "SummaryReport" }, new String[] { "commanderReport", "summaryReport" }));
			snpr.setAllowUnknownChildElements(true);
			// set nested properties
			this.digester.addRule("METurn/BattleReports/BattleReport/Lines/Line/TroopReport/TroopRow", snpr = new SetNestedPropertiesRule(new String[] { "TroopType", "WeaponType", "Armor", "Formations" }, new String[] { "troopType", "weaponType", "armor", "formation" }));
			snpr.setAllowUnknownChildElements(true);
			// add lines
			this.digester.addSetNext("METurn/BattleReports/BattleReport/Lines/Line", "addLine", "org.joverseer.support.readers.newXml.BattleLine");
			// add to container
			this.digester.addSetNext("METurn/BattleReports/BattleReport", "addItem", "org.joverseer.support.readers.newXml.BattleWrapper");

			// character messages
			this.digester.addObjectCreate("METurn/More/Characters/CharacterMessages", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/More/Characters/CharacterMessages", "setCharMessages");
			// create character message wrapper
			this.digester.addObjectCreate("METurn/More/Characters/CharacterMessages/CharacterMessage", "org.joverseer.support.readers.newXml.CharacterMessageWrapper");
			// set char
			this.digester.addSetProperties("METurn/More/Characters/CharacterMessages/CharacterMessage", "CharID", "charId");
			// add lines
			this.digester.addCallMethod("METurn/More/Characters/CharacterMessages/CharacterMessage/Lines/Line", "addLine", 1);
			this.digester.addCallParam("METurn/More/Characters/CharacterMessages/CharacterMessage/Lines/Line", 0);
			// add line for current position
			this.digester.addCallMethod("METurn/More/Characters/CharacterMessages/CharacterMessage/Lines/Line/CurrentLocation", "addLine", 1);
			this.digester.addCallParam("METurn/More/Characters/CharacterMessages/CharacterMessage/Lines/Line/CurrentLocation", 0);
			// add to container
			this.digester.addSetNext("METurn/More/Characters/CharacterMessages/CharacterMessage", "addItem", "org.joverseer.support.readers.newXml.CharacterMessageWrapper");

			// create container for Encounters
			this.digester.addObjectCreate("METurn/EncounterMessages", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/EncounterMessages", "setEncounters");
			// create encounter wrapper
			this.digester.addObjectCreate("METurn/EncounterMessages/Encounter", "org.joverseer.support.readers.newXml.EncounterWrapper");
			// set attributes
			this.digester.addSetProperties("METurn/EncounterMessages/Encounter", "CharID", "charId");
			this.digester.addSetProperties("METurn/EncounterMessages/Encounter", "Hex", "hex");
			this.digester.addSetProperties("METurn/EncounterMessages/Encounter", "Reacting", "reacting");
			// set nested properties
			this.digester.addRule("METurn/EncounterMessages/Encounter", snpr = new SetNestedPropertiesRule(new String[] { "EncounterHeader", "EncounterText" }, new String[] { "header", "text" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/EncounterMessages/Encounter", "addItem", "org.joverseer.support.readers.newXml.EncounterWrapper");

			// create container for Hexes
			this.digester.addObjectCreate("METurn/Hexes", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/Hexes", "setHexes");
			// create hex wrapper
			this.digester.addObjectCreate("METurn/Hexes/Hex", "org.joverseer.support.readers.newXml.HexWrapper");
			// set attributes
			// set nested properties
			this.digester.addRule("METurn/Hexes/Hex", snpr = new SetNestedPropertiesRule(new String[] { "HexID", "Terrain", "PopcenterName", "PopcenterSize", "Forts", "Ports", "Roads", "Bridges", "Fords", "MinorRivers", "MajorRivers" }, new String[] { "hexID", "terrain", "popCenterName", "popCenterSize", "forts", "ports", "roads", "bridges", "fords", "minorRivers", "majorRivers" }));
			snpr.setAllowUnknownChildElements(true);
			// add to container
			this.digester.addSetNext("METurn/Hexes/Hex", "addItem", "org.joverseer.support.readers.newXml.HexWrapper");

			// create container for OrdersGiven
			this.digester.addObjectCreate("METurn/OrdersGiven", "org.joverseer.support.Container");
			// add container to turn info
			this.digester.addSetNext("METurn/OrdersGiven", "setOrdersGiven");
			// create OrdersGiven object
			this.digester.addObjectCreate("METurn/OrdersGiven/OrdersGivenToChar", "org.joverseer.support.readers.newXml.OrdersGiven");
			// set attributes
			this.digester.addSetProperties("METurn/OrdersGiven/OrdersGivenToChar", "CharacterName", "characterName");
			// add to container
			this.digester.addSetNext("METurn/OrdersGiven/OrdersGivenToChar", "addItem", "org.joverseer.support.readers.newXml.OrdersGiven");
			// create OrderWrapper
			this.digester.addObjectCreate("METurn/OrdersGiven/OrdersGivenToChar/Order", "org.joverseer.support.readers.newXml.OrderWrapper");
			// add to OrdersGiven
			this.digester.addSetNext("METurn/OrdersGiven/OrdersGivenToChar/Order", "addOrder", "org.joverseer.support.readers.newXml.OrderWrapper");
			// set nested properties
			this.digester.addRule("METurn/OrdersGiven/OrdersGivenToChar/Order", snpr = new SetNestedPropertiesRule(new String[] { "OrderNumber" }, new String[] { "orderNumber" }));
			snpr.setAllowUnknownChildElements(true);
			// create OrderParameterWrapper
			this.digester.addObjectCreate("METurn/OrdersGiven/OrdersGivenToChar/Order/Additional", "org.joverseer.support.readers.newXml.OrderParameterWrapper");
			// add to OrdersWrapper
			this.digester.addSetNext("METurn/OrdersGiven/OrdersGivenToChar/Order/Additional", "addParameter", "org.joverseer.support.readers.newXml.OrderParameterWrapper");
			// set properties
			this.digester.addSetProperties("METurn/OrdersGiven/OrdersGivenToChar/Order/Additional", "SeqNo", "seqNo");
			// set text
			this.digester.addCallMethod("METurn/OrdersGiven/OrdersGivenToChar/Order/Additional", "setParameter", 1);
			this.digester.addCallParam("METurn/OrdersGiven/OrdersGivenToChar/Order/Additional", 0);
			// create OrderParameterWrapper
			this.digester.addObjectCreate("METurn/OrdersGiven/OrdersGivenToChar/Order/Movement", "org.joverseer.support.readers.newXml.OrderMovementParameterWrapper");
			// add to OrdersWrapper
			this.digester.addSetNext("METurn/OrdersGiven/OrdersGivenToChar/Order/Movement", "addParameter", "org.joverseer.support.readers.newXml.OrderParameterWrapper");
			// set properties
			this.digester.addSetProperties("METurn/OrdersGiven/OrdersGivenToChar/Order/Movement", "SeqNo", "seqNo");
			// set text
			this.digester.addCallMethod("METurn/OrdersGiven/OrdersGivenToChar/Order/Movement", "setParameter", 1);
			this.digester.addCallParam("METurn/OrdersGiven/OrdersGivenToChar/Order/Movement", 0);

			this.turnInfo = (TurnInfo) this.digester.parse(fileName);
		}

		catch (Exception exc) {
			// TODO fix
			logger.error(exc);
			throw new Exception("Error parsing Xml Turn file.", exc);
		}
	}

	@Override
	public void run() {
		try {
			readFile(this.filename);
		} catch (Exception exc) {
			this.monitor.subTaskStarted("Error : failed to read xml file (" + exc.getMessage() + ")");
			this.errorOccured = true;
		}
		try {
			if (this.turnInfo == null) {
				return;
			}
			this.turnInfo.setNationNo(this.nationNo);
			updateGame(this.game);
			this.game.setCurrentTurn(this.game.getMaxTurn());
			Thread.sleep(100);
		} catch (Exception exc) {
			this.errorOccured = true;
		}
	}

	@SuppressWarnings("hiding")
	public void updateGame(Game game) throws Exception {
		try {
			this.infoSource = new XmlExtraTurnInfoSource(game.getMaxTurn(), this.nationNo);

			this.turn = game.getTurn(game.getMaxTurn());
			// update turn from turnInfo here.
			
			if (this.turnInfo.gameInfo !=null) {
				if (getMonitor() != null) {
					getMonitor().worked(0);
					getMonitor().subTaskStarted("Updating game setup...");
				}
				try {
					updateGameSetup(game);
				} catch (Exception exc) {
					logger.error(exc);
					this.errorOccured = true;
					getMonitor().subTaskStarted("Error: " + exc.getMessage());
				}
			}

			if (getMonitor() != null) {
				getMonitor().worked(0);
				getMonitor().subTaskStarted("Updating pop centers...");
			}
			try {
				updatePopCenters(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(10);
				getMonitor().subTaskStarted("Updating artifacts...");
			}
			try {
				updateArtifacts(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(20);
				getMonitor().subTaskStarted("Updating relations...");
			}
			try {
				updateRelations(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(30);
				getMonitor().subTaskStarted("Updating companies...");
			}
			try {
				updateCompanies(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(40);
				getMonitor().subTaskStarted("Updating battles...");
			}
			try {
				updateBattles(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(45);
				getMonitor().subTaskStarted("Updating armies...");
			}
			try {
				updateArmies(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(50);
				getMonitor().subTaskStarted("Updating anchored ships...");
			}
			try {
				updateAnchoredShips(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(55);
				getMonitor().subTaskStarted("Updating characters...");
			}
			try {
				updateCharacterMessages(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(60);
				getMonitor().subTaskStarted("Updating encounters...");
			}
			try {
				updateEncounters(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(65);
				getMonitor().subTaskStarted("Updating hostages...");
			}
			try {
				updateHostages(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			try {
				updateClimates(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}

			if (getMonitor() != null) {
				getMonitor().worked(70);
				getMonitor().subTaskStarted("Updating double agents...");
			}
			try {
				updateDoubleAgents(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(75);
				getMonitor().subTaskStarted("Updating challenges...");
			}
			try {
				updateChallenges(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(80);
				getMonitor().subTaskStarted("Updating hexes...");
			}
			try {
				updateHexes(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(90);
				getMonitor().subTaskStarted("Updating SNAs...");
			}
			try {
				updateSNAs(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(95);
				getMonitor().subTaskStarted("Updating Orders Given...");
			}
			try {
				updateOrdersGiven(game);
			} catch (Exception exc) {
				logger.error(exc);
				this.errorOccured = true;
				getMonitor().subTaskStarted("Error: " + exc.getMessage());
			}
			if (getMonitor() != null) {
				getMonitor().worked(100);
			}
		} catch (Exception exc) {

		}
	}
	
	private void updateGameSetup(Game game1) throws Exception {
			if (logger.isDebugEnabled()) {
			for (GameInfoOptionWrapper option : (ArrayList<GameInfoOptionWrapper>) this.turnInfo.gameInfo.items) {
				logger.debug("gameInfo "+ option.name + "="+ option.value);
			}
			
		}
		
		if (this.turnInfo.modifiers != null) {
			//remove default modifiers for this nation
			try {
				game1.getMetadata().clearModifiers(this.turnInfo.nationNo);
			} catch(Exception exc) {
				// No modifiers set for this nation anyway.
			}
			
			for (TurnInfoModifierWrapper modifier : (ArrayList<TurnInfoModifierWrapper>) this.turnInfo.modifiers.getItems()) {
				
				if (modifier.getClimate() != null) {
					ClimateModifier m = new ClimateModifier();

					int ClimateID = Integer.parseInt(modifier.getClimate());
					
					m.setNationNo(this.turnInfo.nationNo);
					m.setModifier(modifier.getModifier());
					
					switch(ClimateID) {
					
					case 1:
						m.setClimate(ClimateEnum.Polar);
						break;
						
					case 2:
						m.setClimate(ClimateEnum.Severe);
						break;
		
					case 3:
						m.setClimate(ClimateEnum.Cold);
						break;
					
					case 4:
						m.setClimate(ClimateEnum.Cool);
						break;
					
					case 5:
						m.setClimate(ClimateEnum.Mild);
						break;
						
					case 6:
						m.setClimate(ClimateEnum.Warm);
						break;
						
					case 7:
						m.setClimate(ClimateEnum.Hot);
						break;
						
					}			
					
					game1.getMetadata().getClimateModifiers().addItem(m);

				}
				else if (modifier.getTerrain() != null) {
					int TerrainID = Integer.parseInt(modifier.getTerrain());
					
					TerrainModifier m = new TerrainModifier();
					
					m.setNationNo(this.turnInfo.nationNo);
					m.setModifier(modifier.getModifier());
					
					switch(TerrainID) {
					// The Terrain in the data file is different to the JO Enum
					case 0:
						m.setTerrain(HexTerrainEnum.ocean);
						break;

					case 1:
						m.setTerrain(HexTerrainEnum.sea);
						break;
						
					case 2:
						m.setTerrain(HexTerrainEnum.shore);
						break;

					case 3:
						m.setTerrain(HexTerrainEnum.plains);
						break;
					
					case 4:
						m.setTerrain(HexTerrainEnum.hillsNrough);
						break;
					
					case 5:
						m.setTerrain(HexTerrainEnum.forest);
						break;
						
					case 6:
						m.setTerrain(HexTerrainEnum.desert);
						break;
						
					case 7:
						m.setTerrain(HexTerrainEnum.swamp);
						break;
						
					case 8:
						m.setTerrain(HexTerrainEnum.mountains);
						break;
						
					}
					
					game1.getMetadata().getTerrainModifiers().addItem(m);
				}
			}
		}
		
		boolean error = false;
		String errorMessage = "";
		
		Container armies = this.turn.getContainer(TurnElementsEnum.Army);
		for (GameInfoOptionWrapper option : (ArrayList<GameInfoOptionWrapper>) this.turnInfo.gameInfo.findAllByProperty("name", "NoArmiesNation")) {
			//These nations need all armies removing from the game				
			try {
				armies.removeAllByProperties("nationNo", Integer.parseInt(option.value));
			} catch(Exception exc) {
				error = true;
				errorMessage += "Error removing armies for nation " + option.value + "\n";				
			}	
		}			

		Container pcs = this.turn.getContainer(TurnElementsEnum.PopulationCenter);
		ArrayList<PopulationCenter> removepcs = new ArrayList<PopulationCenter>();		

		for (GameInfoOptionWrapper option : (ArrayList<GameInfoOptionWrapper>) this.turnInfo.gameInfo.findAllByProperty("name", "NoPCsNation")) {
			//These nations need all PCs removing from the game				
			try {
				//Build List of PCs to remove - removeAllByProperties throws an error so do it in two stages
				for (PopulationCenter pc : (ArrayList<PopulationCenter>) pcs.findAllByProperty("nationNo", Integer.parseInt(option.value))) {
					removepcs.add(pc);
				}
				
				//Remove PCs from this turn
				for (PopulationCenter r : removepcs) {
					pcs.removeItem(r);
				}
			
			} catch(Exception exc) {
				error = true;
				errorMessage += "Error removing PCs for nation " + option.value + "\n";	
			}
		}			

		
		Container cs = game1.getMetadata().getCharacters();
		for (GameInfoOptionWrapper option : (ArrayList<GameInfoOptionWrapper>) this.turnInfo.gameInfo.findAllByProperty("name", "NoStartingCharactersNation")) {
			//These nations need all starting characters removing from the game
			
			try {				
				cs.removeAllByProperties("nationNo", Integer.parseInt(option.value));
			} catch (Exception exc) {
				error = true;
				errorMessage += "Error removing characters for nation " + option.value + "\n";					
			}
		}
		
		GameInfoOptionWrapper option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "NoPCsNation");
		if (option != null && option.value != null) {
			GameMetadata gm = game1.getMetadata();
			String[] notInNations = option.value.split(",");
			for(String nn : notInNations) {
				try {
					int nni = Integer.parseInt(nn.trim());				
					Nation n = gm.getNations().get(nni);
					n.setEliminated(true);
				} catch (Exception exc) {
					error = true;
					errorMessage += "Error setting eliminated (not in game) for nation " + nn + "\n";	
				}
			}
		}
		
		option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "EvilNations");
		if (option != null && option.value != null) {
			GameMetadata gm = game1.getMetadata();
			NationRelations nr;
			String[] evilNations = option.value.split(",");
			for(String en : evilNations) {
				try {
					int eni = Integer.parseInt(en.trim());				
					Nation n = gm.getNations().get(eni);
					nr = game1.getTurn().getNationRelations(eni);
					n.setAllegiance(NationAllegianceEnum.DarkServants);
					if (nr.getAllegiance() != n.getAllegiance()) {
						nr.setAllegiance(n.getAllegiance());
					}
				} catch (Exception exc) {
					error = true;
					errorMessage += "Error setting allegiance for nation " + en + "\n";	
				}
			}
		}
		
		//Set diplo character limit
		option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "DiploLimit");
		if(option != null && option.value != null) {
			try {
				int charLimit = Integer.parseInt(option.value);
				Diplo.charPerNation = charLimit;
			} catch (Exception exc) {
				error = true;
				errorMessage += "Error setting diplo character limit: " + option.value + "\n";
			}
		}
		
		//Check to see if diplo is due this turn, setting a flag in PlayerInfo to indicate ths
		option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "DiploDue");
		if (option != null && option.value != null) {
			PlayerInfo pi = this.game.getTurn().getPlayerInfo(this.game.getMetadata().getNationNo());
			String[] turnsDue = option.value.split(",");
			for (int i = 0; i < turnsDue.length; i++) {
				try {
					int t = Integer.parseInt(turnsDue[i]);
					if (t - 1 == this.game.getCurrentTurn() && pi != (null)) {
						pi.setDiploDue(true);
						break;
					}
					
				} catch (Exception exc) {
					error = true;
					errorMessage += "Error setting the diplo due for turn:" + turnsDue[i] + "\n";
				}
			}
		}
		
		
		option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "GoodNations");
		if (option != null && option.value != null) {
			GameMetadata gm = game1.getMetadata();
			NationRelations nr;
			String[] goodNations = option.value.split(",");
			for(String en : goodNations) {
				try {
					int eni = Integer.parseInt(en.trim());				
					Nation n = gm.getNations().get(eni);
					nr = game1.getTurn().getNationRelations(eni);
					n.setAllegiance(NationAllegianceEnum.FreePeople);
					if (nr.getAllegiance() != n.getAllegiance()) {
						nr.setAllegiance(n.getAllegiance());
					}
				} catch (Exception exc) {
					error = true;
					errorMessage += "Error setting allegiance for nation " + en + "\n";	
				}	
			}
		}
		
		option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "NeutralNations");
		if (option != null && option.value != null) {
			GameMetadata gm = game1.getMetadata();
			NationRelations nr;
			String[] neutralNations = option.value.split(",");
			for(String en : neutralNations) {
				try {
					int eni = Integer.parseInt(en.trim());				
					Nation n = gm.getNations().get(eni);
					nr = game1.getTurn().getNationRelations(eni);
					n.setAllegiance(NationAllegianceEnum.Neutral);
					if (nr.getAllegiance() != n.getAllegiance()) {
						nr.setAllegiance(n.getAllegiance());
					}
				} catch (Exception exc) {
					error = true;
					errorMessage += "Error setting allegiance for nation " + en + "\n";	
				}
			}
		}		
		
		if (error) {
			throw new Exception(errorMessage);
		}
		
		
	}
	
    private ClimateEnum translateClimate(String climate) {
        if (climate == null) return null;
        if (climate.equals("Polar")) return ClimateEnum.Polar;
        if (climate.equals("Severe")) return ClimateEnum.Severe;
        if (climate.equals("Cold")) return ClimateEnum.Cold;
        if (climate.equals("Cool")) return ClimateEnum.Cool;
        if (climate.equals("Mild")) return ClimateEnum.Mild;
        if (climate.equals("Warm")) return ClimateEnum.Warm;
        if (climate.equals("Hot")) return ClimateEnum.Hot;
        return null;
    }
    private void updateClimates(Game game1) throws Exception {

        Container pcws = this.turnInfo.getPopCentres();
        if (pcws ==null) return;
        Container<HexInfo> his = this.turn.getHexInfos();
        for (PopCenterWrapper pcw : (ArrayList<PopCenterWrapper>)pcws.getItems()) {
            HexInfo hi = his.findFirstByProperty("hexNo", pcw.getHexNo());
            ClimateEnum climate = translateClimate(pcw.getClimate());
            if (climate != null) {
                hi.setClimate(climate);
            }
        }

        Container aws = this.turnInfo.getArmies();
		Container cs = this.turn.getContainer(TurnElementsEnum.Character);
        for (ArmyWrapper aw : (ArrayList<ArmyWrapper>)aws.getItems()) {
			Character c = (Character) cs.findFirstByProperty("name", aw.getCommander());
			if (c != null) {
				HexInfo hi = his.findFirstByProperty("hexNo", c.getHexNo());
				ClimateEnum climate = translateClimate(aw.getClimate());
				if (climate != null) {
					hi.setClimate(climate);
				}
			}
        }

    }

	private void updateChallenges(Game game1) throws Exception {
		Container challenges = this.turnInfo.getChallenges();
		for (ChallengeWrapper cw : (ArrayList<ChallengeWrapper>) challenges.getItems()) {
			cw.parse();
			if (cw.getHexNo() > 0) {
				Challenge c = this.turn.findChallenge(cw.getCharacter());
				if (c != null) {
					this.turn.getContainer(TurnElementsEnum.Challenge).removeItem(c);
				}
				c = new Challenge();
				c.setHexNo(cw.getHexNo());
				c.setCharacter(cw.getCharacter());
				c.setDescription(cw.getDescription());
				this.turn.getContainer(TurnElementsEnum.Challenge).addItem(c);
			}

		}
	}

	private void updateHexes(Game game1) throws Exception {
		Container hws = this.turnInfo.getHexes();
		if (hws == null)
			return;
		for (HexWrapper hw : (ArrayList<HexWrapper>) hws.getItems()) {
			hw.updateGame(game1);
		}
		Container rws = this.turnInfo.getRecons();
		for (ReconWrapper rw : (ArrayList<ReconWrapper>) rws.getItems()) {
			rw.updateGame(game1);
		}
	}

	private void updateEncounters(Game game1) throws Exception {
		Container ews = this.turnInfo.getEncounters();
		if (ews == null)
			return;
		Container encounters = game1.getTurn().getContainer(TurnElementsEnum.Encounter);
		for (EncounterWrapper ew : (ArrayList<EncounterWrapper>) ews.getItems()) {
			Encounter ne = ew.getEncounter();
			if (ne == null)
				continue;
			Character c = (Character) game1.getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("name", ne.getCharacter());
			if (c == null) {
				continue;
			}
			Encounter e = (Encounter) encounters.findFirstByProperties(new String[] { "character", "hexNo" }, new Object[] { c.getName(), Integer.parseInt(ew.getHex()) });

			if (e != null) {
				encounters.removeItem(e);
			}
			ne.setCharacter(c.getName());
			encounters.addItem(ne);
		}
	}

	private void updateHostages(Game game1) throws Exception {
		Container hws = this.turnInfo.getHostages();
		for (HostageWrapper hw : (ArrayList<HostageWrapper>) hws.getItems()) {
			hw.updateGame(game1, this.turn, this.infoSource);
		}
	}

	private void updateDoubleAgents(Game game1) throws Exception {
		Container daws = this.turnInfo.getDoubleAgents();
		DoubleAgentInfoSource dais = new DoubleAgentInfoSource(this.turnInfo.getTurnNo(), this.turnInfo.getNationNo());
		Container cs = this.turn.getContainer(TurnElementsEnum.Character);
		for (DoubleAgentWrapper daw : (ArrayList<DoubleAgentWrapper>) daws.getItems()) {
			Character c = (Character) cs.findFirstByProperty("name", daw.getName());
			if (c == null) {
				// add character
				c = daw.getCharacter();
				c.setInfoSource(dais);
				cs.addItem(c);
			}
			// set nation if applicable
			Nation n = game1.getMetadata().getNationByName(daw.getNation());
			if (n != null) {
				c.setNationNo(n.getNumber());
			}
			// set order results if applicable
			if (c.getOrderResults() == null || c.getOrderResults().equals("")) {
				c.setOrderResults(daw.getReport());
			}
			
			c.setDoubleAgent(true, this.turnInfo.getNationNo());
			
		}

	}

	private void updateCharacterMessages(Game game1) throws Exception {
		Container nrws = this.turnInfo.getCharMessages();
		Container cs = this.turn.getContainer(TurnElementsEnum.Character);
		for (CharacterMessageWrapper cmw : (ArrayList<CharacterMessageWrapper>) nrws.getItems()) {
			// TODO: sort this bit of a hack...without this 'dwar' doesn't get any order results.
			// judging by the hack in CharacterMessageWrapper.SetCharId  there's probably something else that got fixed by that.
			String id = cmw.getCharId();
			Character c = (Character) cs.findFirstByProperty("id", id);
			if (c == null) {
				while (id.length() < 5) {
					id = id + " ";
				}
				c = (Character) cs.findFirstByProperty("id", id);
			}
			if (c != null) {
				InfoSource ifs = new DerivedFromOrderResultsInfoSource(this.turn.getTurnNo(), this.turnInfo.nationNo, c.getName());
				cmw.updateCharacter(c, game1);
				GameMetadata gm = JOApplication.getMetadata();

				for (OrderResult or : cmw.getOrderResults(ifs,gm)) {
					or.updateGame(game1, this.turn, this.turnInfo.nationNo, c.getName());
				}
			}
		}

	}

	private void updateAnchoredShips(Game game1) throws Exception {
		String commanderName = "[Anchored Ships]";
		Container asws = this.turnInfo.getAnchoredShips();
		Container<Army> armies = this.turn.getArmies();
		if (asws == null)
			return;
		for (AnchoredShipsWrapper asw : (Iterable<AnchoredShipsWrapper>) asws) {
			String hexNo = String.valueOf(asw.getHexId());
			Army a = armies.findFirstByProperties(new String[] { "commanderName", "hexNo", "nationNo" }, new Object[] { commanderName, hexNo, this.turnInfo.getNationNo() });
			if (a == null) {
				a = new Army();
				a.setNavy(true);
				a.setSize(ArmySizeEnum.unknown);
				a.setCommanderName(commanderName);
				a.setCommanderTitle("");
				a.setHexNo(hexNo);
				a.setNationNo(this.turnInfo.getNationNo());
				NationAllegianceEnum allegiance = NationAllegianceEnum.Neutral;
				NationRelations nr = game1.getTurn().getNationRelations(this.turnInfo.getNationNo());
				if (nr != null) {
					allegiance = nr.getAllegiance();
				}
				a.setNationAllegiance(allegiance);
				a.setInformationSource(InformationSourceEnum.exhaustive);
				a.setInfoSource(new PdfTurnInfoSource(this.turnInfo.getTurnNo(), this.turnInfo.getNationNo()));
				a.setElement(ArmyElementType.Warships, asw.getWarships());
				a.setElement(ArmyElementType.Transports, asw.getTransports());
				armies.addItem(a);
			}
		}
	}

	private void updateRelations(Game game1) throws Exception {
		Container nrws = this.turnInfo.getNationRelations();

		Nation nation = game1.getMetadata().getNationByNum(this.nationNo);
		if (nation == null) {
			throw new Exception("Failed to find nation with number " + this.nationNo);
		}
		nation.setNPCsRecruited(this.turnInfo.getNPCsRecruited());
		Container<NationRelations> nrs = this.turn.getNationRelations();
		NationRelations nr = nrs.findFirstByProperty("nationNo", this.nationNo);

		if (this.turnInfo.getAlignment() == 1) {
			nation.setAllegiance(NationAllegianceEnum.FreePeople);
		} else if (this.turnInfo.getAlignment() == 2) {
			nation.setAllegiance(NationAllegianceEnum.DarkServants);
		} else if (this.turnInfo.getAlignment() == 3) {
			nation.setAllegiance(NationAllegianceEnum.Neutral);
		}
		if (nr == null) {
			throw new Exception("Failed to retrieve NationRelations object for nation " + this.nationNo);
		}
		nr.setAllegiance(nation.getAllegiance());

		String problematicNations = "";
		for (NationRelationWrapper nrw : (ArrayList<NationRelationWrapper>) nrws.getItems()) {
			Nation n = game1.getMetadata().getNationByNum(nrw.getNationNo());
			if (n == null) {
				problematicNations += (problematicNations.equals("") ? "" : ", ") + nrw.getNationNo();
			} else {
				nr.setRelationsFor(n.getNumber(), NationRelationWrapper.fromString(nrw.getRelation()));
			}
		}
		if (!problematicNations.equals("")) {
			throw new Exception("Failed to update relations with nations " + problematicNations + " because the nation names were invalid.");
		}
	}

	//@org.assertj.core.util.VisibleForTesting
	void updateBattles(Game game1) {
		Container bws = this.turnInfo.getBattles();
		Container combats = this.turn.getContainer(TurnElementsEnum.Combat);
		for (BattleWrapper bw : (ArrayList<BattleWrapper>) bws.getItems()) {
			bw.parse();
			Combat c = (Combat) combats.findFirstByProperty("hexNo", bw.getHexNo());
			if (c == null) {
				c = new Combat();
				c.setHexNo(bw.getHexNo());
				c.addNarration(this.turnInfo.getNationNo(), bw.getText());
				combats.addItem(c);
			} else {
				c.addNarration(this.turnInfo.getNationNo(), bw.getText());
			}
			CombatWrapper cw = new CombatWrapper();
			cw.setHexNo(bw.getHexNo());
			cw.parseAll(bw.getText());
			
			for (ArmyEstimate ae : cw.getArmyEstimates(game1)) {
				ArmyEstimate eae = (ArmyEstimate) game1.getTurn().getContainer(TurnElementsEnum.ArmyEstimate).findFirstByProperty("commanderName", ae.getCommanderName());
				if (eae != null) {
					game1.getTurn().getContainer(TurnElementsEnum.ArmyEstimate).removeItem(eae);
				}
				game1.getTurn().getContainer(TurnElementsEnum.ArmyEstimate).addItem(ae);
			}
			
			if (cw.getPopCenterOutcome()=="captured" || cw.getPopCenterOutcome()=="destroyed") {
				try {
					Container pcs = this.turn.getContainer(TurnElementsEnum.PopulationCenter);
					PopulationCenter pc = (PopulationCenter) pcs.findFirstByProperty("hexNo", cw.getHexNo());
					
					if (pc!=null) {
						if (cw.getPopOutcomeNation() != null) {
							Nation newOwner = game1.getMetadata().getNationByName(cw.getPopOutcomeNation());
							pc.setNation(newOwner);
						} else {
							//it's probably a ruin.
						}
						
						//Update size and fortifications
						if (cw.getPopCenterOutcomeSize() != null) {
							PopulationCenterSizeEnum newSize = PopulationCenterSizeEnum.getFromLabel(cw.getPopCenterOutcomeSize()); 
							if (newSize == PopulationCenterSizeEnum.ruins) {
								if (pc.getInfoSource().getTurnNo() != this.turn.getTurnNo()) {
									// old information is from a previous turn
									pc.setSize(newSize);
									pc.setNationNo(0);
									pc.setInformationSource(InformationSourceEnum.some);
								} else {
									// it's possible it got camped on this turn. and combats come before camping and nation reports.
									if (!pc.getInformationSource().isMoreDetailedThan(InformationSourceEnum.some)) {
										pc.setSize(newSize);
										pc.setNationNo(0);
										pc.setInformationSource(InformationSourceEnum.some);
									}
								}
							} else {
								pc.setSize(newSize);
							}
						}
						
						if (cw.getPopCenterOutcomeFort() != null) {
							pc.setFortification(FortificationSizeEnum.getFromText(cw.getPopCenterOutcomeFort()));
						}
					}
				} catch (Exception exc) {
					logger.error(exc);
				}
			}

		}
	}

	private void updateCompanies(Game game1) {
		Container cws = this.turnInfo.getCompanies();
		Container cs = this.turn.getContainer(TurnElementsEnum.Company);
		for (CompanyWrapper cw : (ArrayList<CompanyWrapper>) cws.getItems()) {
			Company newC = cw.getCompany();
			newC.setInfoSource(this.infoSource);
			Company oldC = (Company) cs.findFirstByProperty("commander", newC.getCommander());
			if (oldC != null) {
				cs.removeItem(oldC);
			}
			cs.addItem(newC);
		}
	}

	private void updateArmies(Game game1) {
		Container aws = this.turnInfo.getArmies();
		Container as = this.turn.getContainer(TurnElementsEnum.Army);
		for (ArmyWrapper aw : (ArrayList<ArmyWrapper>) aws.getItems()) {
			Army a = (Army) as.findFirstByProperty("commanderName", aw.getCommander());
			if (a != null) {
				aw.updateArmy(a);
			}
		}
	}

	private void updatePopCenters(Game game1) throws Exception {
/*		Container oldpcws = this.turnInfo.getOldPopCentres();
		if (oldpcws == null) {
			throw new Exception("Error: no PCs found");
		}
		//TODO: fix this for spotting changed capital.
		TurnXmlReader.updateOldPCs(this.turn, this.turnInfo.getTurnNo(),this.turnInfo.getNationNo(), -1,oldpcws.getItems(),this.infoSource, this.currentNationPops, this.game.getMetadata());
*/		Container pcws = this.turnInfo.getPopCentres();
		if (pcws == null) {
			throw new Exception("Error: no PCs found... should you have set old XML format?");
		}
		Container pcs = this.turn.getContainer(TurnElementsEnum.PopulationCenter);
		for (PopCenterWrapper pcw : (ArrayList<PopCenterWrapper>) pcws.getItems()) {
			PopulationCenter pc = (PopulationCenter) pcs.findFirstByProperty("hexNo", pcw.getHexNo());
			pcw.updatePopCenter(pc);
			for (String foreignCharacter : pcw.getForeignCharacters()) {
				Character c = this.turn.getCharByName(foreignCharacter);
				if (c == null) {
					c = new Character();
					c.setName(foreignCharacter);
					c.setId(Character.getIdFromName(foreignCharacter));
					c.setNationNo(0);
					c.setInfoSource(this.infoSource);
					c.setInformationSource(InformationSourceEnum.limited);
					c.setHexNo(pcw.getHexNo());
					this.turn.getCharacters().addItem(c);
				}
			}
		}
	}

	private void updateOrdersGiven(Game game1) {
		Container ogs = this.turnInfo.getOrdersGiven();
		Turn t = game1.getTurn(game1.getCurrentTurn() - 1);

		if (t == null)
			return;
		
		for (OrdersGiven og : (ArrayList<OrdersGiven>) ogs.getItems()) {
			Character c = t.getCharById(og.getCharacterName());
			if (c == null) c = t.getCharById(og.getCharacterName().trim());	//Another hack as some Ids trim some don't
			if (c != null) {
				for (Order o : c.getOrders()) {
					o.clear();
				}
				for (int i = 0; i < og.getOrders().size(); i++) {
					OrderWrapper ow = og.getOrders().get(i);
					try {
						int orderNo = Integer.parseInt(ow.getOrderNumber());
						Order o = c.getOrders()[i];
						o.setOrderNo(orderNo);
						for (int j = 0; j < ow.getParameters().size(); j++) {
							String param = ow.getParameters().get(j);
							if (param.equals("^"))
								param = "-";
							o.setParameter(j, param);
						}
					} catch (Exception e) {
						// nothing
					}
				}
			}
		}
	}

	private void updateArtifacts(Game game1) {
		Container aws0 = this.turnInfo.getNonHiddenArtifacts();
		Container aws1 = this.turnInfo.getHiddenArtifacts();
		ArrayList<ArtifactWrapper> aws = new ArrayList<ArtifactWrapper>();
		aws.addAll(aws0.getItems());
		aws.addAll(aws1.getItems());
		// for FA game, update artifact numbers
		// where the starting artifact Ids are just guesses, as the game randomises.
		boolean doUpdateId = game1.getMetadata().getGameType() == GameTypeEnum.gameFA
				|| game1.getMetadata().getGameType() == GameTypeEnum.gameKS
				|| game1.getMetadata().getGameType() == GameTypeEnum.gameCME
				|| game1.getMetadata().getGameType() == GameTypeEnum.gameCMF;

		if (this.turnInfo.gameInfo !=null) {
			//there are some game customizations going on.
			if (this.turnInfo.gameInfo.size() > 0) {
				GameInfoOptionWrapper option = (GameInfoOptionWrapper)this.turnInfo.gameInfo.findFirstByProperty("name", "ShuffleArtefacts");
				if (option.value.equals("1")) {
					if (null == this.game.getParameter("ShuffledArtefacts")) {
						// this is the first time we have seen this
						if (!doUpdateId) {
							// and it's an unusual game type.
							// so all the assumptions about the artifact numbers are wrong.
							game1.getMetadata().resetArtifactNumbers();
						}
					}
					// mark the game as using randomised artifacts, so we don't splat the numbers again.
					this.game.setParameter("ShuffledArtefacts", "1");
					doUpdateId = true;
				}
			}
		}
		for (ArtifactWrapper aw : aws) {
			try { // so that we just skip for any bad artifact.
				if (doUpdateId) {
					String artiNameInAscii = StringUtils.normalizeNamesForComparison(aw.getName().trim());
					boolean found = false;
					for (ArtifactInfo ai : game1.getMetadata().getArtifacts().getItems()) {
						if (ai.getUnAccentedName().equalsIgnoreCase(artiNameInAscii)) {
							found = true;
							ai.setNo(aw.getId());
							break;
						}
					}
					if (!found) {
						// add artifact
						ArtifactInfo ai = new ArtifactInfo();
						ai.setName(aw.getName().trim());
						ai.setNo(aw.getId());
						game1.getMetadata().getArtifacts().addItem(ai);
					}
				}


				// for all arties update powers
				ArtifactInfo ai = game1.getMetadata().findFirstArtifactByNumber(aw.getId());
				if (ai != null && aw.getPower() != null && !aw.getPower().equals("")) {
					// parse power
					// TODO handle open seas, scry etc
					String power = aw.getPower();

					if (!power.equals(ai.getPower1())) {
						power += "*"; // mark power as updated for this game
						if (ai.getPowers().size() == 0) {
							ai.getPowers().add(power);
						} else {
							ai.getPowers().set(0, power);
						}
					}
				}

				// update hidden artifacts
				// TODO - existed in pdf, does not exist in New Xml
				// if (aws1.contains(aw)) {
				// Artifact a =
				// (Artifact)game.getTurn().getContainer(TurnElementsEnum.Artifact).findFirstByProperty("number",
				// aw.getId());
				// if (a == null) {
				// a = new Artifact();
				// a.setNumber(aw.getId());
				// a.setName(aw.getName().trim());
				// //a.setHexNo(aw.get());
				// //a.setOwner(turnInfo.getNationName());
				// a.setInfoSource(infoSource);
				// game.getTurn().getContainer(TurnElementsEnum.Artifact).addItem(a);
				// }
				// }
			} catch (Exception exc) {
				logger.error(exc);
			}
		}

	}

	@SuppressWarnings("hiding")
	private void updateSNAs(Game game) {
		Container snaws = this.turnInfo.getSnas();
		ArrayList<SNAEnum> snas = new ArrayList<SNAEnum>();
		for (SNAWrapper snw : (ArrayList<SNAWrapper>) snaws.getItems()) {
			snas.add(SNAEnum.getSnaFromNumber(snw.getCode().intValue()));
		}
		Nation n = game.getMetadata().getNationByNum(this.nationNo);
		n.setSnas(snas);
		n.setNPCsRecruited(this.turnInfo.NPCsRecruited);
	}
}
