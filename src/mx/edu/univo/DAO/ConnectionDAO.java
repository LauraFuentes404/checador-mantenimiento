/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.edu.univo.DAO;

/**
 *
 * @author Laura
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.swing.JOptionPane;

public class ConnectionDAO {

    public String user;
    public String pass;
    public String bdname;
    public String hostname;
    public String port;

    Connection conn = null;

    public void archivo() {
        try {

            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            user = props.getProperty("user");
            pass = props.getProperty("pass");
            bdname = props.getProperty("bdname");
            port = props.getProperty("port");
            hostname = props.getProperty("hostname");
        } catch (IOException ex) {
            System.out.println("Error: " + ex);
        }
    }

    public Connection conectar() {
        archivo();
        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "No se encontro el PostgreSQL JDBC ");
            e.printStackTrace();

        }

        try {
            String url= "jdbc:postgresql://" + hostname + ":" + port + "/" + bdname;
            System.out.println(url);
            conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Si se conecto");
        } catch (SQLException e) {
            StringBuilder sb = new StringBuilder(e.toString());
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append("\n\tat ");
                sb.append(ste);
            }
            String trace = sb.toString();
            JOptionPane.showMessageDialog(null, trace,
                    "[ERROR]", JOptionPane.ERROR_MESSAGE);
            System.out.println("Error de Conexión");
            e.printStackTrace();

        }

        if (conn != null) {
        } else {
            System.out.println("¡Fallo al hacer la conexión!");
        }
        return conn;
    }

    public void desconectar() {
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println("¡Fallo al hacer en desconexión!");
        }
    }

}
