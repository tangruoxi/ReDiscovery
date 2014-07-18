package com.strider.dataanonymizer;

import com.strider.dataanonymizer.functions.Functions;
import com.strider.dataanonymizer.requirement.Column;
import com.strider.dataanonymizer.requirement.Parameter;
import com.strider.dataanonymizer.requirement.Requirement;
import com.strider.dataanonymizer.requirement.Table;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.collections.IteratorUtils;

import org.apache.commons.configuration.*;
import org.apache.log4j.Logger;


/**
 *
 * @author strider
 */
public class DatabaseAnonymizer implements IAnonymizer { 
    
    static Logger log = Logger.getLogger(DatabaseAnonymizer.class);

    @Override
    public void anonymize(String propertyFile) {

        // Reading configuration file
        Configuration configuration = null;
        try {
            configuration = new PropertiesConfiguration(propertyFile);
        } catch (ConfigurationException ex) {
            log.error(ColumnDiscoverer.class);
        }
        
        String driver = configuration.getString("driver");
        String database = configuration.getString("database");
        String url = configuration.getString("url");
        String userName = configuration.getString("username");
        String password = configuration.getString("password");
        log.debug("Using driver " + driver);
        log.debug("Database type: " + database);
        log.debug("Database URL: " + url);
        log.debug("Logging in using username " + userName);

        log.info("Connecting to database");
        Connection connection = null;
        try {
            Class.forName(driver).newInstance();
            connection = DriverManager.getConnection(url,userName,password);
            connection.setAutoCommit(false);
        } catch (Exception e) {
            log.error("Problem connecting to database.\n" + e.toString(), e);
        }        
                
        // Now we collect data from the requirement
        Requirement requirement = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(Requirement.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            requirement = (Requirement) unmarshaller.unmarshal(new File("src/main/resources/Requirement.xml"));        
        } catch (JAXBException je) {
            log.error(je.toString());
        }

        // Initializing static data in Functions
        Functions.init();
        
        // Iterate over the requirement
        log.info("Anonymizing data for client " + requirement.getClient() + " Version " + requirement.getVersion());
        
        for(Table table : requirement.getTables()) {
            log.info("Table [" + table.getName() + "]. Start ...");
            
            // Here we start building SQL query
            
            PreparedStatement pstmt = null;
            Statement stmt = null;
            ResultSet rs = null;
            StringBuilder sql = new StringBuilder("UPDATE " + table.getName() + " SET ");
            int batchCounter = 0;            
            
            // First iteration over columns to build the UPDATE statement
            for(Column column : table.getColumns()) {
                sql.append(column.getName() + " = ?,");
            }
            // remove training ","
            if (sql.length() > 0) {
                sql.setLength(sql.length() - 1);
            }
            
            sql.append(" WHERE ").append(table.getPKey()).append(" = ?");
            String updateString = sql.toString();
            
            try {
                stmt = connection.createStatement();
                rs = stmt.executeQuery("SELECT id FROM " + table.getName());
                pstmt = connection.prepareStatement(updateString);
                while (rs.next()) {
                    int id = rs.getInt("id");
                    // Second iteration over columns
                    int index = 0;

                    for(Column column : table.getColumns()) {
                        log.info("    Taking care of column [" + column.getName() + "]");
                        String function = column.getFunction();
                        if (function == null || function.equals("")) {
                            log.warn("    Function is not defined for column [" + column + "]. Moving to the next column.");
                        } else {
                            try {
                                if (column.getParameters().isEmpty()) {
                                    log.info("    Function [" + function + "] has no defined parameters.");
                                    pstmt.setString(++index, Functions.class.getMethod(function, String.class).invoke(null).toString());
                                } else {
                                    log.info("    Function [" + function + "] accepts following parameter(s):");
                                    for(Parameter parameter : column.getParameters()) {
                                        log.info("        " + parameter.getName());
                                        log.info("        " + parameter.getValue());
                                        Class clazz = Functions.class;
                                        Method method = clazz.getMethod(function, String.class);
                                        String result = method.invoke(null, parameter.getValue()).toString();
                                        log.info(result);
                                        pstmt.setString(++index, result);
                                    }
                                }
                                //Method method = Functions.class.getMethod(function, String.class);
                                //Object o = method.invoke(null, "whatever");
                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                java.util.logging.Logger.getLogger(DatabaseAnonymizer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    pstmt.setInt(++index, id);
                    pstmt.addBatch();
                    batchCounter++;
                    // @todo Get rid of this hardcoding
                    if (batchCounter == 1000) {
                        pstmt.executeBatch();
                        connection.commit();
                        batchCounter = 0;
                    }
                }
                pstmt.executeBatch();
                connection.commit();
            } catch (SQLException sqle) {
                log.error(sqle.toString());
                try {
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (pstmt != null) {
                        pstmt.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException sqlex) {
                    log.error(sqlex.toString());
                }                
            }
            log.info("Table " + table.getName() + ". End ...");
            log.info("");
        }                
        
        // log.info(map.toString());
        
    }
    
    private class Pair {
        private String tableName;
        private String columnName;

        Pair(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }
        
        public String getTableName() {
            return this.tableName;
        }
        
        public String getColumnName() {
            return this.columnName;
        }
        
        public String toString() {
            return this.tableName + "->" + this.columnName;
        }
    }
}