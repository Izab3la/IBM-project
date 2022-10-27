/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.autumn.annotations.FromGetParam;

/**
 *
 * @author piotr
 */
public class Server {
    
    private static final String TAG = "Autumn";
    public static final String VERSION = "1.0.0";

    private final Map<String, EndpointInstance> endpoints = new HashMap<>();
    private final List<String> endpointsHierarchy;

    private HttpServer underlying = null;
    
    private Logger logger;

    public void start(int port) throws IOException {
        logger.log(Logger.INFO, TAG, "Starting Autumn server at port " + port);
        underlying = HttpServer.create(new InetSocketAddress(port), 0);
        underlying.setExecutor(Executors.newCachedThreadPool());
        underlying.createContext("/", e -> {
            State state = new State(
                    e.getRequestHeaders(),
                    e.getResponseBody(),
                    e.getRequestBody(),
                    e.getRequestURI()) {
                @Override
                public void sendResponseHeaders(int statusCode) throws IOException {
                    e.getResponseHeaders().putAll(getOutboundHeaders());
                    e.sendResponseHeaders(statusCode, outputLength);
                }
            };
            
            state.setHeader("Connection", "close");
            state.setHeader("X-PoweredBy", "Autumn " + Server.VERSION);
            
            String path = e.getRequestURI().getPath();
            logger.log(Logger.INFO, TAG, "Incoming connection to path " + path);
            try {
                for (String possibleEndpointFormat : endpointsHierarchy) {
                    Map<String, String> pathParameters;
                    try{
                        pathParameters = Utils.parse(possibleEndpointFormat, path);
                    }catch(Exception ex){
                        continue;
                    }
                    if (pathParameters != null) {
                        Map<String, Object> getParameters = Utils.queryToMap(e.getRequestURI().getRawQuery());
                        Map<String, Object> cookies = Utils.queryToMap(e.getRequestHeaders().getFirst("Cookie"), "; ");
                        Map<String, Object> bodyParameters = null;
                        
                        EndpointInstance endpoint = endpoints.get(possibleEndpointFormat);
                        Method toInvoke = endpoint.method;
                        
                        if(!endpoint.allowedHTTPMethods.contains(e.getRequestMethod())){
                            state.sendString("AUTUMN: Invalid HTTP method for endpoint - expected " + String.join(", ", endpoint.allowedHTTPMethods), 400);
                            return;
                        }
                        
                        String contentType = e.getRequestHeaders().getFirst("Content-Type");
                        if(contentType != null && endpoint.tryParsingBody){
                            String tokens[] = contentType.split("; ");
                            String base = tokens[0];
                            String charset = "utf-8";
                            if(tokens.length > 1 && tokens[1].startsWith("charset=")){
                                charset = tokens[1].substring(8);
                            }
                            
                            ObjectMapper mapper = new ObjectMapper();
                            
                            int contentLength;
                            try{
                                contentLength = Integer.valueOf(e.getRequestHeaders().getFirst("Content-Length"));
                            }catch(NumberFormatException ex){
                                state.sendString("Cannot read data - invalid Content-Length header", 400);
                                return;
                            }
                            
                            switch(base){
                                case "application/x-www-form-urlencoded":
                                    bodyParameters = Utils.queryToMap(new String(Utils.readFully(e.getRequestBody(), contentLength), charset));
                                    state.setHasBodyBeenParsed(true);
                                    break;
                                //FIXME: This is a nightmare
                                case "application/json":
                                    JsonNode node = mapper.readTree(Utils.readFully(e.getRequestBody(), contentLength));
                                    bodyParameters = new HashMap<>();
                                    bodyParameters.put("<raw>", node);
                                    if(node.isObject()){
                                        ObjectNode oNode = (ObjectNode) node;
                                        Iterator<Entry<String, JsonNode>> subNodes = oNode.fields();
                                        while(subNodes.hasNext()){
                                            Entry<String, JsonNode> subNode = subNodes.next();
                                            bodyParameters.put(subNode.getKey(), subNode.getValue());
                                        }
                                    }
                                    state.setHasBodyBeenParsed(true);
                            }
                        }

                        Object parameters[] = new Object[toInvoke.getParameterCount()];

                        parameters[0] = state;
                        
                        endpoint.pathParamsToArgumentsMap.forEach((i, n) -> parameters[i] = pathParameters.get(n));
                        
                        for(int i : endpoint.cookiesToArgumentsMap.keySet()){
                            ParameterInfo in = endpoint.cookiesToArgumentsMap.get(i);
                            Object result = cookies.get(in.name);
                            if(result == null && !in.optional){
                                try{
                                    state.sendString("AUTUMN: Missing mandatory cookie " + in.name, 400);
                                    return;
                                }catch(IOException ex){}
                            }
                            parameters[i] = result;
                        }
                        
                        for(int i : endpoint.getParamsToArgumentsMap.keySet()){
                            ParameterInfo in = endpoint.getParamsToArgumentsMap.get(i);
                            Object result = getParameters.get(in.name);
                            if(result == null && !in.optional){
                                try{
                                    state.sendString("AUTUMN: Missing mandatory get parameter " + in.name, 400);
                                    return;
                                }catch(IOException ex){}
                            }
                            parameters[i] = result;
                        }
                        
                        for(int i : endpoint.bodyParamsToArgumentsMap.keySet()){
                            ParameterInfo in = endpoint.bodyParamsToArgumentsMap.get(i);
                            if((bodyParameters == null || bodyParameters.get(in.name) == null) && !in.optional){
                                try{
                                    state.sendString("AUTUMN: Missing mandatory body parameter " + in.name, 400);
                                    return;
                                }catch(IOException ex){}
                            }
                            parameters[i] = bodyParameters.get(in.name);
                        }

                        try {
                            toInvoke.invoke(endpoint.instance, parameters);
                            e.close();
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            try{
                                state.sendString("AUTUMN: The underlying endpoint has encountered a fatal error:\n\n\n" + Utils.traceToString(ex), 500);
                            }catch(IOException notImportant){}
                            logger.logException(Logger.ERROR, TAG, ex, "The underlying endpoint has encountered a fatal error (" + path + ")");
                        }
                        return;
                    }
                }
            } catch (Exception ex) {
                logger.logException(Logger.ERROR, TAG, ex);
            }
            try {
                logger.log(Logger.WARNING, TAG, "No matching endpoint found for path " + path);
                state.sendString("AUTUMN: No matching endpoint found", 404);
            } catch (IOException ex) {
            }
        });
        underlying.start();
    }

    public void stop() {
        if (underlying != null) {
            underlying.stop(5);
            underlying = null;
        }
    }

    public Server(String packageName, Logger logger, Class<?> constructTypes[], Object... constructParameters) {
        Reflections base = new Reflections(packageName,
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());
        
        this.logger = logger;

        //Step 1 - find all methods tagged by @Endpoint
        Set<Method> methods = base.getMethodsAnnotatedWith(Endpoint.class);
        Map<Class<?>, Object> instancesMap = new HashMap<>();
        methods.forEach(method -> {
            Class<?> declaringClass = method.getDeclaringClass();
            Endpoint endpointAnno = method.getAnnotation(Endpoint.class);
            Route routeAnno = declaringClass.getAnnotation(Route.class);
            Object instance = null;
            if (instancesMap.containsKey(declaringClass)) {
                instance = instancesMap.get(declaringClass);
            } else {
                try {
                    instance = declaringClass
                            .getConstructor(constructTypes)
                            .newInstance(constructParameters);
                    instancesMap.put(declaringClass, instance);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
            //Now got the instance and method. We can parse the arguments
            EndpointInstance endpoint = new EndpointInstance(instance, method, endpointAnno.allowedMethods());
            
            endpoint.tryParsingBody = endpointAnno.tryParsingBody();

            if (!method.getParameters()[0].getType().equals(State.class)) {
                throw new IllegalStateException("Function: " + method.getName() + " delcared in " + declaringClass.toString() + " has to have the first argument of type State");
            }

            for (int i = 1; i < method.getParameterCount(); i++) {
                Parameter param = method.getParameters()[i];
                if (param.getAnnotations().length != 1) {
                    throw new IllegalStateException("Function: " + method.getName() + " delcared in " + declaringClass.toString() + " had an invalid argument");
                }
                FromPath fromPath = param.getAnnotation(FromPath.class);
                FromGetParam getParam = param.getAnnotation(FromGetParam.class);
                FromCookie cookieParam = param.getAnnotation(FromCookie.class);
                FromBody bodyParam = param.getAnnotation(FromBody.class);
                if (fromPath != null) {
                    endpoint.pathParamsToArgumentsMap.put(i, fromPath.value());
                } else if (getParam != null) {
                    endpoint.getParamsToArgumentsMap.put(i, new ParameterInfo(getParam.value(), getParam.optional()));
                } else if (cookieParam != null){
                    endpoint.cookiesToArgumentsMap.put(i, new ParameterInfo(cookieParam.value(), cookieParam.optional()));
                } else if (bodyParam != null){
                    endpoint.bodyParamsToArgumentsMap.put(i, new ParameterInfo(bodyParam.value(), bodyParam.optional()));
                }
            }
            String endpointName = endpointAnno.value();
            String route = routeAnno == null ? "" : routeAnno.value();
            if(!endpointName.startsWith("/")) endpointName = "/" + endpointName;
            if(route.endsWith("/")) route = route.substring(0, route.length() - 1);
            endpoints.put(route + endpointName, endpoint);
        });
        
        endpointsHierarchy = new ArrayList<>(endpoints.size());
        endpoints.keySet()
                .stream()
                .sorted((a, b) -> Utils.count(b, "/") - Utils.count(a, "/"))
                .forEachOrdered(endpointsHierarchy::add);
    }

    private class EndpointInstance {
        private Object instance;
        private Method method;
        private List<String> allowedHTTPMethods;
        private boolean tryParsingBody;
        private Map<Integer, ParameterInfo> getParamsToArgumentsMap = new HashMap<>();
        private Map<Integer, String> pathParamsToArgumentsMap = new HashMap<>();
        private Map<Integer, ParameterInfo> cookiesToArgumentsMap = new HashMap<>();
        private Map<Integer, ParameterInfo> bodyParamsToArgumentsMap = new HashMap<>();

        public EndpointInstance(Object instance, Method method, String[] allowedHTTPMethods) {
            this.instance = instance;
            this.method = method;
            this.allowedHTTPMethods = Arrays.asList(allowedHTTPMethods);
        }
    }

    private class ParameterInfo {
        private String name;
        private boolean optional;

        public ParameterInfo(String name, boolean optional) {
            this.name = name;
            this.optional = optional;
        }

    }
}
