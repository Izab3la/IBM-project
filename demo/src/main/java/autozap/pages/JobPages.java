/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.pages;

import autozap.ConfigParameter;
import autozap.Utils;
import autozap.database.AbstractDatabase;
import autozap.dataobjects.Job;
import autozap.dataobjects.Template;
import autozap.kubectlinterface.AbstractInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromGetParam;
import org.autumn.annotations.Route;

import static autozap.Utils.createResponseString;
import java.util.UUID;

/**
 *
 * @author piotr
 */

@Route("/api/jobs/")
public class JobPages {
    private AbstractDatabase database;
    private AbstractInterface kubeInterface;

    public JobPages(AbstractDatabase database, AbstractInterface kubeInterface) {
        this.database = database;
        this.kubeInterface = kubeInterface;
    }
    
    @Endpoint("/all")
    public void getAllJobs(State state) throws IOException{
        try {
            state.sendString(Utils.MAPPER.writeValueAsString(database.getAllJobs()), Utils.JSON);
        } catch (JsonProcessingException ex) {
            throw new IOException(ex);
        }
    }
    
    @Endpoint("/status")
    public void getJobStatus(
            State state,
            @FromGetParam("job") String jobName) throws IOException{
        state.sendString(
                Utils.MAPPER.writeValueAsString(kubeInterface.getJobState(jobName)), 
                Utils.JSON);
    }
    
    @Endpoint("/delete")
    public void deleteJob(
            State state,
            @FromGetParam("id") String idStr
    ) throws IOException{
        try {
            boolean status = database.deleteJob(Integer.valueOf(idStr));
            state.sendString(createResponseString(status), status ? 400 : 200);
        } catch (NumberFormatException ex) {
            state.sendString(createResponseString(false, "Invalid id"));
        }
    }

    
    @Endpoint( value = "/create", allowedMethods = { "POST" }, tryParsingBody = true )
    public void createJob(
            State state,
            @FromBody("friendlyName") JsonNode nameNode,
            @FromBody("friendlyUsername") JsonNode usernameNode,
            @FromBody("password") JsonNode passwordNode,
            @FromBody("parameters") ObjectNode paramsGiven
    ) throws IOException{
        Map<ConfigParameter, Object> parameters = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> nodes = paramsGiven.fields();
        while(nodes.hasNext()){
            Map.Entry<String, JsonNode> entry = nodes.next();
            ConfigParameter param = ConfigParameter.valueOf(entry.getKey());
            if(param == null){
                state.sendString(createResponseString(false, "Invalid parameter: " + entry.getKey()), 400);
                return;
            }
            parameters.put(param, Utils.jsonNodeParamToObject(param, entry.getValue()));
        }
        if(nameNode.asText().length() < 3){
            state.sendString(createResponseString(false, "Name must be longer than 3 characters"), 400);
            return;
        }
        StringBuilder errorBuilder = new StringBuilder();
        parameters
                .entrySet()
                .stream()
                .filter(n -> !n.getKey().validate(n.getValue()))
                .map(n -> n.getKey().name())
                .forEach(n -> 
                    errorBuilder
                            .append("Invalid value given for parameter: ")
                            .append(n)
                            .append('\n')
                );
        if(errorBuilder.length() > 0){
            state.sendString(createResponseString(false, errorBuilder.toString()), 400);
            return;
        }
        Job created = new Job(nameNode.asText(), usernameNode.asText(), passwordNode.asText(), UUID.randomUUID().toString());//
        created.setParameters(parameters);
        if(!kubeInterface.registerJob(created)){
            state.sendString(createResponseString(false, "Cannot create job: " + created.getFriendlyName()), 500);
            return;
        }
        // At this point the 'created' job should also have the internalName set.
        if(database.createJob(created)){
            state.sendString(createResponseString(true));
        }else{
            state.sendString(createResponseString(false, "Failed to create job"), 500);
        }
    }
}
