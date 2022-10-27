/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.dataobjects;

import autozap.ConfigParameter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author piotr
 */
public class Template {
    private Map<ConfigParameter, Object> parameters;
    private String friendlyName, friendlyUsername, password;
    private int id = -1;

    public Template(String friendlyName, String friendlyUsername, String password, Map<ConfigParameter, Object> parametersSet) {
        this.parameters = parametersSet;
        this.friendlyName = friendlyName;
        this.friendlyUsername = friendlyUsername;
        this.password = password;
    }
    
    public Map<ConfigParameter, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<ConfigParameter, Object> parameters) {
        this.parameters = parameters;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyUsername() {
        return friendlyUsername;
    }

    public void setFriendlyUsername(String friendlyUsername) {
        this.friendlyUsername = friendlyUsername;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    
}
