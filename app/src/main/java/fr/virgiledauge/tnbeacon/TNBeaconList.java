package fr.virgiledauge.tnbeacon;

import java.util.ArrayList;

/**
 * Created by virgile on 26/05/15.
 */
public class TNBeaconList extends ArrayList<TNBeaconData> {
    @Override
    public boolean add(TNBeaconData object) {
        if(!this.contains(object)){
           return super.add(object);
        }
        return true;
    }

    @Override
    public boolean contains(Object object) {
        for(TNBeaconData item: this){
            if(item.equals((TNBeaconData)(object))){
                return true;
            }
        }
        return false;
    }
}
