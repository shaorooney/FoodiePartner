
package com.cantor.drop.aggregator.model;

import javax.jms.*;
import javax.naming.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;


public class tibjmsDurableSubscriber
{
    String      serverUrl       = "tcp://usitdjms-zp1:7222,tcp://usitdjms-zp2:7222";
    String      userName        = "admin";
    String      password        = null;

    String      topicName       = "drop_agg_risk";
    String      clientID        = "0320";
    String      durableName     = "handsome";

    boolean     unsubscribe     = false;

    public tibjmsDurableSubscriber(String[] args) {

        parseArgs(args);

        try {
            tibjmsUtilities.initSSLParams(serverUrl,args);
        }
        catch (JMSSecurityException e)
        {
            System.err.println("JMSSecurityException: "+e.getMessage()+", provider="+e.getErrorCode());
            e.printStackTrace();
            System.exit(0);
        }

        if (!unsubscribe && (topicName == null)) {
            System.err.println("Error: must specify topic name");
            usage();
        }

        if (durableName == null) {
            System.err.println("Error: must specify durable subscriber name");
            usage();
        }

        System.err.println("DurableSubscriber sample.");
        System.err.println("Using clientID:       "+clientID);
        System.err.println("Using Durable Name:   "+durableName);

        try
        {
            TopicConnectionFactory factory = new com.tibco.tibjms.TibjmsTopicConnectionFactory(serverUrl);

            TopicConnection connection = factory.createTopicConnection(userName,password);

            /* if clientID is specified we must set it right here */
            if (clientID != null)
                connection.setClientID(clientID);

            TopicSession session = connection.createTopicSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);

            if (unsubscribe) {
                System.err.println("Unsubscribing durable subscriber "+durableName);
                session.unsubscribe(durableName);
                System.err.println("Successfully unsubscribed "+durableName);
                connection.close();
                return;
            }

            System.err.println("Subscribing to topic: "+topicName);

            /*
             * Use createTopic() to enable subscriptions to dynamic topics.
             */
            javax.jms.Topic topic = session.createTopic(topicName);

            TopicSubscriber subscriber = session.createDurableSubscriber(topic,durableName);

            connection.start();

            /* read topic messages */
            while(true)
            {
                javax.jms.Message message = subscriber.receive();
                if (message == null)
                    break;

                System.err.println("\nReceived message: "+message);

                /*
                Implement Method to Read Trade Object from JMS
                 */
                if (message instanceof BytesMessage) {
                    final BytesMessage bytesMessage = (BytesMessage) message;
                    // init a DataStructure to capture each trade object.
                    try {
                        //inner logical part is obtain trade object.
                        CFTrade.Trade trade = CFTrade.Trade.parseFrom(new InputStream() {
                            @Override
                            public int read() throws IOException {
                                try {
                                    //System.out.println("bytesMessage.readByte(): " + bytesMessage.readByte());
                                    return bytesMessage.readByte();
                                } catch (JMSException e) {
                                    return -1;
                                }
                            }
                        });
                        System.out.println("trade: " + trade.toString());

                        /*
                        Insert the trade object into MarketRisk tt_transaction table
                         */









                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //update into database

                } else
                    System.out.println("Got" + message.toString());


            }

            connection.close();
        }
        catch(JMSException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void main(String args[])
    {
        /*
        MarketRisk_UAT connection
         */

        String connectionUrl = "jdbc:sqlserver://RPDDCMG0502:1433;databaseName=marketrisk;" +
                "user=marketrisk;password=E551A82D72";

        // Declare the JDBC objects.
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            con = DriverManager.getConnection(connectionUrl);
            System.out.println("successfully connect to MarketRisk database");
            // Create and execute an SQL statement that returns some data.
//            String SQL = "insert into tt_transaction(execId) values('ss') ";
//            stmt = con.createStatement();
//            rs = stmt.executeQuery(SQL);
//
//            // Iterate through the data in the result set and display it.
//            while (rs.next()) {
//                System.out.println(rs.toString());
//            }
        }
        // Handle any errors that may have occurred.
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (rs != null) try { rs.close(); } catch(Exception e) {}
            if (stmt != null) try { stmt.close(); } catch(Exception e) {}
            if (con != null) try { con.close(); } catch(Exception e) {}
        }

        tibjmsDurableSubscriber t = new tibjmsDurableSubscriber(args); //create subscription.

    }



    void usage()
    {
        System.err.println("\nUsage: java tibjmsDurableSubscriber [options]");
        System.err.println("");
        System.err.println("   where options are:");
        System.err.println("");
        System.err.println(" -server   <server URL> - EMS server URL, default is local server");
        System.err.println(" -user     <user name>  - user name, default is null");
        System.err.println(" -password <password>   - password, default is null");
        System.err.println(" -topic    <topic-name> - topic name, default is \"topic.sample\"");
        System.err.println(" -clientID <client-id>  - clientID, default is null");
        System.err.println(" -durable  <name>       - durable subscriber name,");
        System.err.println("                          default is \"subscriber\"");
        System.err.println(" -unsubscribe           - unsubscribe and quit");
        System.err.println(" -help-ssl              - help on ssl parameters\n");
        System.exit(0);
    }

    void parseArgs(String[] args)
    {
        int i=0;

        while(i < args.length)
        {
            if (args[i].compareTo("-server")==0)
            {
                if ((i+1) >= args.length) usage();
                serverUrl = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-topic")==0)
            {
                if ((i+1) >= args.length) usage();
                topicName = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-user")==0)
            {
                if ((i+1) >= args.length) usage();
                userName = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-password")==0)
            {
                if ((i+1) >= args.length) usage();
                password = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-durable")==0)
            {
                if ((i+1) >= args.length) usage();
                durableName = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-clientID")==0)
            {
                if ((i+1) >= args.length) usage();
                clientID = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-unsubscribe")==0)
            {
                unsubscribe = true;
                i += 1;
            }
            else
            if (args[i].compareTo("-help")==0)
            {
                usage();
            }
            else
            if (args[i].compareTo("-help-ssl")==0)
            {
                tibjmsUtilities.sslUsage();
            }
            else
            if(args[i].startsWith("-ssl"))
            {
                i += 2;
            }
            else
            {
                System.err.println("Unrecognized parameter: "+args[i]);
                usage();
            }
        }
    }

}