/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.pages;

import autozap.ConfigParameter;
import autozap.Utils;
import autozap.database.AbstractDatabase;
import autozap.dataobjects.Template;
import autozap.kubectlinterface.AbstractInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.Route;

import static autozap.Utils.createResponseString;
import org.autumn.annotations.FromGetParam;

/**
 *
 * @author piotr
 */
@Route("/api/templates")
public class TemplatePages {

    private AbstractDatabase database;
    private AbstractInterface kubeInterface;

    public TemplatePages(AbstractDatabase database, AbstractInterface kubeInterface) {
        this.database = database;
        this.kubeInterface = kubeInterface;
    }

    @Endpoint("/all")
    public void getAllTemplates(State state) throws IOException {
        try {
            state.sendString(Utils.MAPPER.writeValueAsString(database.getAllTemplates()), Utils.JSON);
        } catch (JsonProcessingException ex) {
            throw new IOException(ex);
        }
    }

    @Endpoint(value = "/create", allowedMethods = {"POST"}, tryParsingBody = true)
    public void createTemplate(
            State state,
            @FromBody("friendlyName") JsonNode nameNode,
            @FromBody("friendlyUsername") JsonNode usernameNode,
            @FromBody("password") JsonNode passwordNode,
            @FromBody("parameters") ObjectNode paramsNode
    ) throws IOException {
        Map<ConfigParameter, Object> parameters = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> overrideNodes = paramsNode.fields();
        while (overrideNodes.hasNext()) {
            Map.Entry<String, JsonNode> entry = overrideNodes.next();
            ConfigParameter param = ConfigParameter.valueOf(entry.getKey());
            if (param == null) {
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
                .forEach(n
                        -> errorBuilder
                        .append("Invalid value given for parameter: ")
                        .append(n)
                        .append('\n')
                );
        if (errorBuilder.length() > 0) {
            state.sendString(createResponseString(false, errorBuilder.toString()), 400);
            return;
        }
        Template created = new Template(nameNode.asText(), usernameNode.asText(), passwordNode.asText(), parameters);
        if (database.createTemplate(created)) {
            state.sendString(createResponseString(true));
        } else {
            state.sendString(createResponseString(false, "Failed to create template!"), 500);
        }
    }

    @Endpoint("/rename")
    public void renameTemplate(
            State state,
            @FromGetParam("id") String idStr,
            @FromGetParam("newName") String newName
    ) throws IOException {
        if(newName.length() < 3){
            state.sendString(createResponseString(false, "Name must be longer than 3 characters"), 400);
            return;
        }
        try {
            boolean status = database.renameTemplate(Integer.valueOf(idStr), newName);
            state.sendString(createResponseString(status), !status ? 500 : 200);
        } catch (NumberFormatException ex) {
            state.sendString(createResponseString(false, "Invalid id"));
        }
    }
    
    @Endpoint("/delete")
    public void deleteTemplate(
            State state,
            @FromGetParam("id") String idStr
    ) throws IOException{
        try {
            boolean status = database.deleteTemplate(Integer.valueOf(idStr));
            state.sendString(createResponseString(status), !status ? 400 : 200);
        } catch (NumberFormatException ex) {
            state.sendString(createResponseString(false, "Invalid id"));
        }
    }

    @Endpoint("/updateParameter")
    public void updateTemplateParameter(
            State state,
            @FromGetParam("id") String idSte,
            @FromGetParam("configParameter") String configParamName,
            @FromGetParam("newValue") String newValue
    ) throws IOException {
        int id;
        try {
            id = Integer.valueOf(idSte);
        } catch (NumberFormatException ex) {
            state.sendString(createResponseString(false, "Invalid id"), 400);
            return;
        }
        ConfigParameter configParam;
        try{
            configParam = ConfigParameter.valueOf(configParamName);
        }catch(Exception ex){
            state.sendString(createResponseString(false, "Invalid parameter"), 400);
            return;
        }
        Object value;
        try{
            switch(configParam.getType()){
                case BOOLEAN:
                    value = "true".equalsIgnoreCase(newValue);
                    break;
                case INTEGER:
                    value = Integer.valueOf(newValue);
                    break;
                case STRING:
                    value = newValue;
                    break;
                default:
                    state.sendString(createResponseString(false, "Invalid new value"), 400);
                    return;
            }
        }catch(Exception ex){
            state.sendString(createResponseString(false, "Invalid new value"), 400);
            return;
        }
        boolean status = database.setTemplateParameter(id, configParam, value);
        state.sendString(createResponseString(status), !status ? 400 : 200);
    }

}
