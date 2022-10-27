/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.pages;

import autozap.database.AbstractDatabase;
import autozap.kubectlinterface.AbstractInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.autumn.State;
import org.autumn.Utils;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromPath;

/**
 *
 * @author piotr
 */
public class MiscPages {
    
   public MiscPages(AbstractDatabase database, AbstractInterface kubeInterface){}
   private static final File BASE = new File("./base");
   
    private static Map<String, String> extMap;

    static {
        try {
            extMap = java.nio.file.Files.readAllLines(new File("mappings").toPath())
                    .stream()
                    .map(n -> n.split(" "))
                    .collect(Collectors.toMap(n -> n[0], n -> n[1]));
        } catch (IOException ex) {
            extMap = new HashMap<>();
            ex.printStackTrace();
        }
    } 
   
   @Endpoint("/{path}")
   public void file(State state, @FromPath("path") String path) throws IOException{
        if(path == null || path.isEmpty()) path = "index.html";
        if(path.contains("..")) {
            state.sendString("Not found", 404);
            return;
        }
        File fsFile = new File(BASE, path);
        if(!fsFile.exists()){
            state.sendString("Not found", 404);
            return;
        }
        
        String extension = "";
        if (path.contains(".")) {
            extension = path.substring(path.lastIndexOf(".") + 1);
        }
        
        if(extension.contains("/")) extension = "";
        
        if (extMap.containsKey(extension)) {
            state.setHeader("Content-Type", extMap.get(extension));
        }

        state.setOutputLength((int) fsFile.length());
        state.sendResponseHeaders(200);
        Utils.copyStream(new FileInputStream(fsFile), state.getOutput(), 4096);

   }
}
