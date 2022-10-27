/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author piotr
 */
public class Utils {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String JSON = "application/json";
    
    public static Object jsonNodeParamToObject(ConfigParameter param, JsonNode source){
        switch(param.getType()){
            case STRING:
                return source.asText();
            case INTEGER:
                return source.asInt();
            case BOOLEAN:
                return source.asBoolean();
        }
        return null;
    }
    
    public static String createResponseString(boolean successful, String... data){
        ObjectNode node = MAPPER.createObjectNode();
        node.put("ok", successful);
        if(data.length > 0){
            node.put("log", String.join("\n", data));
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            return "{\"ok\": false, \"log\": \"Double error - log couldn't be prepared\"}";
        }
    }
    
    private static String preprocessString(String source, Map<String, String> consts, Map<ConfigParameter, Object> configParameters){
        if(!source.contains("{{")) return source;
        Matcher matcher = Pattern.compile("(\\{\\{[\\s]*(.+?)[\\s]*\\?(.+?)[\\s]*:[\\s]*(.+?)[\\s]*\\}\\})").matcher(source);
        while(matcher.find()){
            String fullGroup = matcher.group(1).trim();
            String expression = matcher.group(2).trim();
            String constOnTrue = matcher.group(3).trim();
            String constOnFalse = matcher.group(4).trim();
            
            ConfigParameter checked = ConfigParameter.valueOf(expression);
            Object value = configParameters.getOrDefault(checked, checked.getDefault());
            
            boolean conditionMet = 
                    checked.getType() == ConfigType.BOOLEAN ? ((Boolean) value) :
                    checked.getType() == ConfigType.INTEGER ? ((Integer) value) != 0 :
                    checked.getType() == ConfigType.STRING ? ((String) value).isEmpty() :
                    false;
            
            String constName = conditionMet ? constOnTrue : constOnFalse;
            String constValue = consts.get(constName);
            if(constValue == null) throwPreprocessYAMLInvalid(source, "No such constant: " + constName);
            source = source.replace(fullGroup, constValue);
        }

        for(ConfigParameter key : ConfigParameter.values()){
            source = source.replaceAll("\\{\\{[\\s]*" + key.name() + "[\\s]*\\}\\}" , configParameters.getOrDefault(key, key.getDefault()).toString());
        }
        return source;
    }
    
    private static void throwPreprocessYAMLInvalid(String expr, String... reason){
        throw new IllegalArgumentException("Expression: " + expr + " is not a valid AutoZap expression" + (reason.length > 0 ? (" (" + reason[0] + "}") : ""));
    }
    
    public static boolean processYAMLTrees(Map source, Map<ConfigParameter, Object> configParameters){
        Map<String, String> strConsts = new HashMap<>();
        try{
            Map sourceMap = (Map) source.get("autozap_strconsts");
            if(sourceMap != null){
                for(Object key : sourceMap.keySet()){
                    strConsts.put(key.toString(), sourceMap.get(key).toString());
                }
            }
        }catch(Exception ex){
            throw new IllegalArgumentException("'autozap_strconsts' must be a String => String map. " + ex.getMessage());
        }    
        source.remove("autozap_strconsts");
        return processYAMLTrees(source, strConsts, configParameters);
    }
    
    public static void copyFile(File source, File target) throws IOException{
        if(source.isFile()){
            org.autumn.Utils.copyStream(new FileInputStream(source), new FileOutputStream(target), 4096);
        }else{
            target.mkdir();
            for(File subFile : source.listFiles()){
                copyFile(subFile, new File(target, subFile.getName()));
            }
        }
    }
    
    private static boolean processYAMLTrees(Object obj, Map<String, String> stringConsts, Map<ConfigParameter, Object> configParameters){
        if(obj instanceof List){
            List list = (List) obj;
            List newList = new ArrayList<Object>(list.size());
            int skipToDepth = -1;
            int currentDepth = 0;
            for(int i = 0; i<list.size(); i++){
                Object elem = list.get(i);
                if(elem instanceof String){
                    if(((String) elem).startsWith("~") && ((String) elem).endsWith("~")){
                        String expression = ((String)elem).substring(1, ((String)elem).length() - 1).trim();
                        if("end".equals(expression)){
                            if(currentDepth == 0) throwPreprocessYAMLInvalid(expression, "Cannot go lower than depth 0");
                           --currentDepth;
                           if(skipToDepth == currentDepth) skipToDepth = -1;
                           continue;
                        }
                        String exprTok[] = expression.split(" ");
                        if(exprTok.length < 2){
                            throwPreprocessYAMLInvalid(expression);
                        }
                        boolean negate = false;
                        if("unless".equals(exprTok[0])){
                            negate = true;
                        }else if(!"if".equals(exprTok[0])){
                            throwPreprocessYAMLInvalid(expression);
                        }
                        
                        ConfigParameter parameterChecked = ConfigParameter.valueOf(exprTok[1]);
                        if(parameterChecked == null){
                            throwPreprocessYAMLInvalid(expression);
                        }
                        
                        Object value = configParameters.getOrDefault(parameterChecked, parameterChecked.getDefault());
                        
                        boolean conditionMet = 
                                parameterChecked.getType() == ConfigType.BOOLEAN ? ((Boolean) value) :
                                parameterChecked.getType() == ConfigType.INTEGER ? ((Integer) value) != 0 :
                                parameterChecked.getType() == ConfigType.STRING ? ((String) value).isEmpty() :
                                false;
                        
                        conditionMet ^= negate;
                        if(!conditionMet){
                            skipToDepth = currentDepth;
                        }
                        ++currentDepth;
                        continue;
                    }
                }
                
                if(skipToDepth == -1){
                    if(elem instanceof String){
                        elem = preprocessString((String) elem, stringConsts, configParameters);
                    }else{
                        processYAMLTrees(elem, stringConsts, configParameters);
                    }
                    newList.add(elem);
                }
            }
            
            list.clear();
            list.addAll(newList);
            return true;
        }else if(obj instanceof Map){
            ((Map) obj).keySet().forEach((key) -> {
                Object value = ((Map) obj).get(key);
                if(value instanceof String){
                    value = preprocessString((String) value, stringConsts, configParameters);
                    ((Map) obj).put(key, value);
                }else processYAMLTrees(value, stringConsts, configParameters);
            });
            return true;
        }
        return false;
    }

    public static void deleteAll(File file) {
        if(!file.isDirectory()){
            file.delete();
        }else{
            Arrays.asList(file.listFiles()).forEach(Utils::deleteAll);
            file.delete();
        }
    }
}
