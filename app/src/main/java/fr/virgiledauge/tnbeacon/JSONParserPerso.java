package fr.virgiledauge.tnbeacon;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class JSONParserPerso {
    private static Gson gson = new Gson();
    public static TNBeaconList getTNBeaconList(Context context,String string){
        try{
            return gson.fromJson(stringFromAsset(context,string),TNBeaconList.class);
        }catch (JsonParseException e){
            e.printStackTrace();
            Toast toast = Toast.makeText(context,"Fichier JSON non-conforme", Toast.LENGTH_LONG);
            toast.show();
        }
        return null;

    }
    private static String stringFromAsset(Context context, String filename){
        String result = null;
        try{
            InputStream inputStream = context.getAssets().open(filename);
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            char[] buffer = new char[inputStream.available()];
            reader.read(buffer);
            reader.close();
            result = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(context,"Fichier JSON non-trouvé", Toast.LENGTH_LONG);
            toast.show();
        }
        return result;
    }
}
