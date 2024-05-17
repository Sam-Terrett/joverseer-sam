package org.joverseer.tools.infoCollectors.artifacts;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.joverseer.domain.IBelongsToNation;
import org.joverseer.domain.IHasMapLocation;
import org.joverseer.domain.IHasTurnNumber;
import org.joverseer.metadata.domain.ArtifactInfo;
import org.joverseer.support.infoSources.InfoSource;

/**
 * Wraps information about an artifact. Used by the artifact info collector.
 *
 *
 * @author Marios Skounakis
 *
 * DONE: add a transient property 'accent-free-name' so we can match on accent agnostic names.
 */
public class ArtifactWrapper implements IHasMapLocation, IBelongsToNation, IHasTurnNumber {
    int hexNo;
    Integer nationNo;
    int turnNo;

    String name;
    int number;
    String owner;
    String power1;
    String power2;
    InfoSource infoSource;
    String alignment;
	transient String unAccentedName;
	
	boolean noZero;

	public ArtifactWrapper() {
	}
	/**
	 * Convenience constructor
	 * @param ai
	 */
	public ArtifactWrapper(ArtifactInfo ai) {
		this.setNumber(ai.getNo());
		this.setName(ai.getName());
		this.setOwner(ai.getOwner());
		this.setPower1(ai.getPower1());
		this.setPower2(ai.getPower2());
		this.setAlignment(ai.getAlignment());
	}
	//use this for finding by name (assuming no two artifacts just differ by accents).
	public String getUnAccentedName() {
		if (this.unAccentedName == null) {
			if (this.name != null) {
				this.unAccentedName =  Normalizer.normalize(this.name, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
			}
		}
		return this.unAccentedName;
	}

    public int getHexNo() {
        return this.hexNo;
    }

    public void setHexNo(int hexNo) {
        this.hexNo = hexNo;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.unAccentedName = null;
    }

    @Override
	public Integer getNationNo() {
        return this.nationNo;
    }

    @Override
	public void setNationNo(Integer nationNo) {
        this.nationNo = nationNo;
    }

    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
		// some data cleaning.
    	if (owner != null) {
    		if (owner.endsWith(",")) {
    			this.owner = owner.substring(0,owner.length()-1);
    		} else {
    			this.owner = owner;
    		}
    	}
        this.owner = owner;
    }

    public String getPower1() {
        return this.power1;
    }

    public void setPower1(String power1) {
        this.power1 = power1;
    }

    public String getPower2() {
        return this.power2;
    }

    public void setPower2(String power2) {
        this.power2 = power2;
    }

    @Override
	public int getTurnNo() {
        return this.turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    @Override
	public int getX() {
        return getHexNo() / 100;
    }

    @Override
	public int getY() {
        return getHexNo() % 100;
    }


    public InfoSource getInfoSource() {
        return this.infoSource;
    }


    public void setInfoSource(InfoSource infoSource) {
        this.infoSource = infoSource;
    }

    public String getAlignment() {
    	return this.alignment;
    }

    public void setAlignment(String alignment) {
    	this.alignment = alignment;
    }
    public boolean isOwned() {
		return (this.owner != null) && (this.owner.length() != 0);
    }
    
	public boolean wasNoZero() {
		return this.noZero;
	}
	
	public void setNoZero(boolean b) {
		this.noZero = b;
	}

}
