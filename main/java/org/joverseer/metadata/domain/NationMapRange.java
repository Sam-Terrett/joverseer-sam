package org.joverseer.metadata.domain;

import java.io.Serializable;
import java.awt.*;


public class NationMapRange implements Serializable {
    int nationNo;
    int tlX;
    int tlY;
    int brX;
    int brY;

    public int getBrX() {
        return brX;
    }

    public void setBrX(int brX) {
        this.brX = brX;
    }

    public int getBrY() {
        return brY;
    }

    public void setBrY(int brY) {
        this.brY = brY;
    }

    public int getTlX() {
        return tlX;
    }

    public void setTlX(int tlX) {
        this.tlX = tlX;
    }

    public int getTlY() {
        return tlY;
    }

    public void setTlY(int tlY) {
        this.tlY = tlY;
    }

    public Rectangle getRectangle() {
        return new Rectangle(getTlX(), getTlY(), getBrX() - getTlX(), getBrY() - getTlY());
    }

    public int getNationNo() {
        return nationNo;
    }

    public void setNationNo(int nationNo) {
        this.nationNo = nationNo;
    }
}
