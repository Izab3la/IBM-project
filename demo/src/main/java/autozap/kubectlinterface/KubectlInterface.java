/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.kubectlinterface;

import autozap.ConfigParameter;
import autozap.Utils;
import autozap.dataobjects.Job;
import autozap.dataobjects.JobState;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author piotr
 */
public class KubectlInterface implements AbstractInterface {

    private static final File SOURCE_CONTAINER = new File("./kube-owasp-zap.org");
//    private static final String KUBECTL_FILE = "./iface/kubectl";
//    private static final String HELM_FILE = "./iface/helm";
    
    private String[] exec(String cmdFormat, Map<String, String> vars) throws Exception{
        String tokens[] = cmdFormat.split(" ");
        for(int i = 0; i<tokens.length; i++){
            if(tokens[i].startsWith("$")){
                tokens[i] = vars.get(tokens[i].substring(1));
            }
        }
        System.out.println("Running shell command: " + (String.join(" ", tokens)));
        Process proc = Runtime.getRuntime().exec(tokens);
        
        proc.waitFor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        org.autumn.Utils.copyStream(proc.getInputStream(), baos, 4096);
        org.autumn.Utils.copyStream(proc.getErrorStream(), baosErr, 4096);
        String out[] = new String[]{ baos.toString(), baosErr.toString() };
        System.out.println("Stdout:");
        System.out.println(out[0]);
        System.out.println("Stderr:");
        System.out.println(out[1]);
        return out;
    }

    @Override
    public boolean registerJob(Job job) {
        try{
            final File newRoot = new File(System.getProperty("java.io.tmpdir"), job.getInternalName());
            final File valuesFile;
            String val = null;
            if(job.getFriendlyUsername().equals("")) {
                val = "values.yaml";
            } else {
                val = "valuesAuth.yaml";
            }
            valuesFile = new File(newRoot, val);
            
            
            Utils.copyFile(SOURCE_CONTAINER, newRoot);
            
            String rawContents = Files.readString(valuesFile.toPath());
            Map<String, String> replacements = new HashMap<>();
            job.getParameters().entrySet().forEach(n -> {
                try {
                    replacements.put(n.getKey().name(), Utils.MAPPER.writeValueAsString(n.getValue()));
                } catch (JsonProcessingException ex) {
                    // Discard.
                }
            });
            
            Map<ConfigParameter, String> internalNames = Map.of(
                    ConfigParameter.SC_XSS, "xss",
                    ConfigParameter.SC_SQLI, "sqli",
                    ConfigParameter.SC_RXSS, "xss_reflected",
                    ConfigParameter.SC_PXSS, "xss_persistent"
            );
            
            String scanTypes = internalNames
                    .entrySet()
                    .stream()
                    .filter(n -> (Boolean) job.getParameters().getOrDefault(n.getKey(), false))
                    .map(n -> n.getValue())
                    .collect(Collectors.joining("\n  - "));
            
            replacements.put("SCANTYPES", scanTypes.isEmpty() ? "" : "  - " + scanTypes);
            
            for(Map.Entry<String, String> remapEntry : replacements.entrySet()){
                rawContents = rawContents.replace("@@" + remapEntry.getKey() + "@@", remapEntry.getValue());
            }
            
            valuesFile.delete();
            Files.writeString(valuesFile.toPath(), rawContents, StandardOpenOption.CREATE_NEW);
            
            String out[] = exec("$helm install $jobname $path --namespace owasp-zap", 
                Map.of(
                    "kbctl", getKubectl(),//KUBECTL_FILE, 
                    "helm", getHelm(),//HELM_FILE, 
                    "jobname", job.getInternalName(), 
                    "path", newRoot.getAbsolutePath()));
            Utils.deleteAll(newRoot);
            return out[1].isEmpty();
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public JobState getJobState(String name) {
        try {
            String out[] = exec("$kbctl logs $job --namespace owasp-zap", Map.of(
                    "kbctl", getKubectl(),//KUBECTL_FILE,
                    "helm", getHelm(),//HELM_FILE,
                    "job", "jobs/" + name + "-kube-owasp-zap"
            ));
            if(out[1].contains("NotFound")){
                return null;
            }
            return new JobState(name, out[0] + "\n" + out[1], out[0].contains("Shutting down ZAP daemo") ? JobState.State.COMPLETED : JobState.State.RUNNING);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private String getHelm(){
     String os = System.getProperty("os.name");
     if(os.startsWith("Windows")) {
     return "helm.exe";
     }
     return null;
     }
    
     private String getKubectl(){
     String os = System.getProperty("os.name");
     if(os.startsWith("Windows")) {
     return "kubectl.exe";
     }
     return null;
     }

}
