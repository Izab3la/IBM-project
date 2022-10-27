/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap;

import java.net.URL;
import java.util.function.Predicate;

/**
 *
 * @author piotr
 */
public enum ConfigParameter {
    DEBUG(ConfigType.BOOLEAN, false, null, "Debug"),
    SPIDER(ConfigType.BOOLEAN, true, null, "Spider"),
    RECURSIVE(ConfigType.BOOLEAN, true, null, "Recurse"),
    SC_XSS(ConfigType.BOOLEAN, true, null, "Scan: XSS"),
    SC_SQLI(ConfigType.BOOLEAN, true, null, "Scan: SQL Injection"),
    SC_RXSS(ConfigType.BOOLEAN, false, null, "Scan: Reflective XSS"),
    SC_PXSS(ConfigType.BOOLEAN, false, null, "Scan: Perisistent XSS"),
    TARGET(ConfigType.STRING, "", str -> {
        try{
            new URL(str.toString()).toURI();
            return true;
        }catch(Exception ex){
            return false;
        }
    }, "Target Host"),
    USERNAME(ConfigType.STRING, "", null, "Username"),
    PASSWORD(ConfigType.STRING, "", null, "Password");
    
    private final ConfigType type;
    private final Object defaultValue;
    private final String friendlyName;
    
    private final Predicate<Object> validator;

    private ConfigParameter(ConfigType type, Object defaultValue, Predicate<Object> validator, String friendlyName) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.validator = validator;
        this.friendlyName = friendlyName;
    }
    
    public String getFriendlyName(){
        return friendlyName;
    }
    
    public boolean validate(Object object){
        return validator == null ? true : validator.test(object);
    }
    
    public Object getDefault(){
        return defaultValue;
    }
    
    public ConfigType getType(){
        return type;
    }
    
}
