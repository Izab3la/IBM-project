/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.pages;

import autozap.ConfigParameter;
import autozap.Utils;
import autozap.database.AbstractDatabase;
import autozap.kubectlinterface.AbstractInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.Route;

/**
 *
 * @author piotr
 */

@Route("/api/info")
public class InfoPages {
    public InfoPages(AbstractDatabase db, AbstractInterface iface){}
    
    @Endpoint("/configParameters")
    public void configParameters(State state) throws IOException{
        state.sendString(Utils.MAPPER.writeValueAsString(Arrays.asList(ConfigParameter.values())
            .stream()
            .map(n -> {
                ObjectNode node = Utils.MAPPER.createObjectNode();
                node.set("default", Utils.MAPPER.convertValue(n.getDefault(), JsonNode.class));
                node.put("name", n.name());
                node.put("type", n.getType().name());
                node.put("friendlyName", n.getFriendlyName());
                node.put("friendlyUsername", n.getFriendlyUsername());
                node.put("password", n.getPassword());
                return node;
            })
            .collect(Collectors.toList())
        ), Utils.JSON);
    }
}
