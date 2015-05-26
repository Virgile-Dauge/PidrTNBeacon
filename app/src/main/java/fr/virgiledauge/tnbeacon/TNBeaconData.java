package fr.virgiledauge.tnbeacon;

/**
 * Created by virgile on 26/05/15.
 */
public class TNBeaconData {
    private String ID, texte;
    private int px,py;
    public TNBeaconData(String ID, String texte, int px, int py){
        this.ID = ID;
        this.texte = texte;
        this.px = px;
        this.py = py;
    }
    public String getID() {
        return ID;
    }

    public String getTexte() {
        return texte;
    }

    public int getPx() {
        return px;
    }

    public int getPy() {
        return py;
    }

    @Override
    public boolean equals(Object o) {
        return ID.equals(((TNBeaconData)o).getID());
    }

    @Override
    public String toString() {
        return ID +"px: "+px+"py: "+py;
    }
}
