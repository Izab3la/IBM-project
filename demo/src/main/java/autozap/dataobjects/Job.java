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
public class Job {
    private int id = -1;
    
    private String friendlyName, friendlyUsername, password, internalName;
    private long timeCreated;
    private Map<ConfigParameter, Object> parameters = new HashMap<>();

    public Job(String friendlyName, String friendlyUsername, String password, String kubectlJobName) {
        this.friendlyName = friendlyName;
        this.friendlyUsername = friendlyUsername;
        this.password = password;
        this.internalName = kubectlJobName;
        this.timeCreated = System.currentTimeMillis();
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }
    
    public Map<ConfigParameter, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<ConfigParameter, Object> parameters) {
        this.parameters = parameters;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }
    
    
}
