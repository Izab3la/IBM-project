/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.database;

import autozap.ConfigParameter;
import autozap.dataobjects.Job;
import autozap.dataobjects.Template;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author piotr
 */
public class SQLiteDatabase implements AbstractDatabase {

    private Connection connection;
    private final Object MUTEX = new Object();

    public SQLiteDatabase(String fileName) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
        createSchema();
    }

    private void execute(String sql) throws SQLException {
        synchronized(MUTEX){
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
        }
    }

    private ResultSet query(String sql, Object... parameters) throws SQLException {
        synchronized(MUTEX){
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            ResultSet rs =  statement.execute() ? statement.getResultSet() : null;
            return rs;
        }
    }

    private void createSchema() throws SQLException {
        try {
            execute("select 'a' from templates");
        } catch (SQLException ex) {
            execute("create table parameters (id integer primary key autoincrement, ownerId integer, type integer, name text, value text)");
            /*
            Types:
            0 - template
            1 - job
             */

            execute("create table templates (id integer primary key autoincrement, friendlyName string unique)");
            execute("create table jobs (id integer primary key autoincrement, friendlyName string, internalName string unique, timeCreated integer)");

            execute("create trigger parameters_cleanup_adter_template_delete after delete on templates for each row begin delete from parameters where ownerID = old.id and type = 0; end;");
            execute("create trigger parameters_cleanup_adter_job_delete after delete on jobs for each row begin delete from parameters where ownerID = old.id and type = 1; end;");
        }
    }

    private void createParameter(ConfigParameter parameter, Object value, int type, int ownerID) throws SQLException {
        query("insert into parameters (ownerID, type, name, value) values (?, ?, ?, ?)", ownerID, type, parameter.name(), value.toString());
    }

    private Map<ConfigParameter, Object> getParametersFor(int id, int type) throws SQLException {
        ResultSet rs = query("select name, value from parameters where ownerid = ? and type = ?", id, type);
        Map<ConfigParameter, Object> objects = new HashMap<>();
        while (rs.next()) {
            String name = rs.getString("name");
            String serializedObject = rs.getString("value");

            ConfigParameter cfgParam = ConfigParameter.valueOf(name);
            Object deserializedObject = null;
            switch (cfgParam.getType()) {
                case INTEGER:
                    deserializedObject = Integer.valueOf(serializedObject);
                    break;
                case BOOLEAN:
                    deserializedObject = "true".equals(serializedObject.toLowerCase());
                    break;
                case STRING:
                    deserializedObject = serializedObject;
                    break;
                default:
                    System.err.println("Unsupported type: " + cfgParam.getType().name());
                    break;
            }
            objects.put(cfgParam, deserializedObject);
        }
        rs.close();
        return objects;
    }

    @Override
    public boolean createTemplate(Template template) {
        try {
            ResultSet rs = query("insert into templates (friendlyName, friendlyUsername, password) values (?) returning id", template.getFriendlyName(), template.getFriendlyUsername(), template.getPassword());
            rs.next();
            int newId = rs.getInt("id");
            rs.close();
            for (ConfigParameter param : template.getParameters().keySet()) {
                createParameter(param, template.getParameters().get(param), 0, newId);
            }
            template.setId(newId);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public Template getTemplateByID(int id) {
        try {
            ResultSet rs = query("select friendlyName, friendlyUsername, password from templates where id = ?", id);
            if (!rs.next()) {
                return null;
            }
            String friendlyName = rs.getString("friendlyName"),
                   friendlyUsername = rs.getString("friendlyUsername"),
                   password = rs.getString("password");
            rs.close();
            Template template = new Template(friendlyName, friendlyUsername, password, getParametersFor(id, 0));
            template.setId(id);
            return template;
        } catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public List<Template> getAllTemplates() {
        List<Template> templates = new LinkedList<>();
        try {
            ResultSet rs = query("select id, friendlyName, friendlyUsername, password from templates");
            while (rs.next()) {
                int id = rs.getInt("id");
                Template templ = new Template(rs.getString("friendlyName"),rs.getString("friendlyUsername"),rs.getString("password"), getParametersFor(id, 0));
                templ.setId(id);
                templates.add(templ);
            }
            rs.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return templates;
    }

    @Override
    public boolean createJob(Job job) {
        try {
            ResultSet rs = query("insert into jobs (friendlyName, friendlyUsername, password, internalName, timeCreated) values (?, ?, ?) returning id", job.getFriendlyName(), job.getFriendlyUsername(), job.getPassword(), job.getInternalName(), job.getTimeCreated());
            rs.next();
            int newId = rs.getInt("id");
            rs.close();
            for (ConfigParameter param : job.getParameters().keySet()) {
                createParameter(param, job.getParameters().get(param), 1, newId);
            }
            job.setId(newId);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public Job getJobByID(int id) {
        try {
            ResultSet rs = query("select friendlyName, friendlyUsername, password, internalName, timeCreated from jobs where id = ?", id);
            if (!rs.next()) {
                rs.close();
                return null;
            }
            String friendlyName = rs.getString("friendlyName"),
                   friendlyUsername = rs.getString("friendlyUsername"),
                   password = rs.getString("password"),
                   internalName = rs.getString("internalName");
            long timeCreated = rs.getLong("timeCreated");
            rs.close();
            Job job = new Job(friendlyName, friendlyUsername, password, internalName);
            job.setTimeCreated(timeCreated);
            job.setParameters(getParametersFor(id, 1));
            job.setId(id);
            return job;
        } catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public List<Job> getAllJobs() {
        List<Job> jobs = new LinkedList<>();
        try {
            ResultSet rs = query("select id, friendlyName, friendlyUsername, password, internalName, timeCreated from jobs");
            while(rs.next()){
                int id = rs.getInt("id");
                String friendlyName = rs.getString("friendlyName"),
                       friendlyUsername = rs.getString("friendlyUsername"),
                       password = rs.getString("password"),
                       internalName = rs.getString("internalName");
                long timeCreated = rs.getLong("timeCreated");
                Job job = new Job(friendlyName, friendlyUsername, password, internalName);
                job.setTimeCreated(timeCreated);
                job.setParameters(getParametersFor(id, 1));
                job.setId(id);
                jobs.add(job);
            }
            rs.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return jobs;
    }

    @Override
    public boolean renameTemplate(int id, String newName) {
        try {
            query("update templates set friendlyName = ? where id = ?", newName, id);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean setTemplateParameter(int id, ConfigParameter parameter, Object newValue) {
        try {
            ResultSet tested = query("select friendlyName from templates where id = ?", id);
            if(!tested.next()){
                tested.close();
                return false;
            }
            tested.close();
            ResultSet rs = query("select value from parameters where name = ? and ownerid = ? and type = 0", parameter.name(), id);
            if(rs.next()){
                // has it already defined
                query("update parameters set value = ? where name = ? and ownerid = ? and type = 0", newValue.toString(), parameter.name(), id);
            }else{
                query("insert into parameters (name, ownerid, type, value) values (?, ?, 0, ?)", parameter.name(), id, newValue.toString());
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean deleteTemplate(int id){
        try{
            query("delete from templates where id = ?", id);
            return true;
        }catch(SQLException ex){
            return false;
        }
    }
    
    @Override
    public boolean deleteJob(int id){
        try{
            query("delete from jobs where id = ?", id);
            return true;
        }catch(SQLException ex){
            return false;
        }
    }

}
