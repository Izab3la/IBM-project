/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap;

import autozap.database.AbstractDatabase;
import autozap.database.SQLiteDatabase;
import autozap.kubectlinterface.AbstractInterface;
import autozap.kubectlinterface.KubectlInterface;
import java.util.Arrays;
import org.autumn.Logger;
import org.autumn.Server;

/**
 *
 * @author piotr
 */
public class AutoZap {

    /*******************************************************************************
     *                                                                             *
     * This class is only a demo of the entire project, and must be treated as such*
     *                          Please keep that in mind!                          *
     *                                                                             *
     *******************************************************************************/

    
    public static void main(String[] args) throws Exception {
        AbstractDatabase database = new SQLiteDatabase("database.db");
        AbstractInterface kubeInterface = new KubectlInterface();
        
        Server server = new Server("autozap.pages", new Logger() {
            @Override
            public void logException(int severity, String tag, Exception ex, String... info) {
                System.out.println("TAG: " + tag);
                ex.printStackTrace();
                Arrays.asList(info).forEach(System.out::println);
            }

            @Override
            public void log(int severity, String tag, String... data) {
                Arrays.asList(data).forEach(n -> System.out.println(String.format("[%d] - [%s]: %s", severity, tag, n)));
            }
        }, new Class<?>[]{
            AbstractDatabase.class,
            AbstractInterface.class,
        }, new Object[]{
            database,
            kubeInterface,
        });
        
        server.start(9999);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop()));
    }
    
}
