package fr.virgiledauge.tnbeacon;

import java.io.InputStream;
import java.util.ArrayList;
import com.google.gson.*;

/**
 * Created by virgile on 26/05/15.
 */
public class TNBeaconList extends ArrayList {
    public TNBeaconList(String path){
        Gson gson = new Gson();

        try {

            BufferedReader br = new BufferedReader(
                    new FileReader("c:\\file.json"));

            //convert the json string back to object
            DataObject obj = gson.fromJson(br, DataObject.class);

            System.out.println(obj);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    }
}
