/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.edu.univo.Jframes;

import mx.edu.univo.DAO.ConnectionDAO;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.GregorianCalendar;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import mx.edu.univo.POJOs.RegistroPOJO;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 *
 * @author Laura
 */
public class LectorHuellaJframe extends javax.swing.JFrame {

    /**
     * Creates new form LectorHuellaJframe
     */
    private Timer timer;
    private DPFPCapture lector = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
    private DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";
    public String claveEmpleadoActual = "";

    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm:ss.SSS");
    SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat formatter4 = new SimpleDateFormat("dd/MMM/YYYY");
    SimpleDateFormat formatter5 = new SimpleDateFormat("HH:mm:ss");
    public DPFPFeatureSet featuresInscripcion;
    public DPFPFeatureSet featuresVerificacion;

    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }

    public Image crearImagenHuella(DPFPSample sample) {
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }

    public void dibujarHuella(Image image) {
        lblImagen.setIcon(new ImageIcon(image.getScaledInstance(lblImagen.getWidth(), lblImagen.getHeight(), image.SCALE_DEFAULT)));
        repaint();
    }

    public void enviarTexto(String string) {
        estadosHuella.setText(estadosHuella.getText() + "\n" + string);
    }

    public void start() {
        lector.startCapture();
        enviarTexto("Utilizando lector de huella");
    }

    public void stop() {
        lector.stopCapture();
        enviarTexto("No se esta usando el lector");
    }

    public DPFPTemplate getTemplate() {
        return template;
    }

    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }

    public void estadoHuellas() {
        enviarTexto("Muestra de huellas necesarias para guaradar template "
                + reclutador.getFeaturesNeeded());
    }

    public void procesarCaptura(DPFPSample sample) {
        featuresInscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresVerificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if (featuresInscripcion != null) {
            try {
                reclutador.addFeatures(featuresInscripcion);
                Image image = crearImagenHuella(sample);
                dibujarHuella(image);
                switch (reclutador.getTemplateStatus()) {
                    case TEMPLATE_STATUS_INSUFFICIENT:
                        stop();
                        setTemplate(reclutador.getTemplate());
                        enviarTexto("La plantilla de la huella ha sido creada, se empieza a verificar...");
                        verificarHuella();

                        break;
                    case TEMPLATE_STATUS_READY:
                        stop();
                        setTemplate(reclutador.getTemplate());
                        enviarTexto("La plantilla de la huella ha sido creada, se empieza a verificar...");
                        verificarHuella();

                        break;
                    case TEMPLATE_STATUS_FAILED:
                        reclutador.clear();
                        stop();
                        estadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(null, "Fallo captura, coloque de nuevo la huella",
                                "[ERROR]", JOptionPane.ERROR_MESSAGE);
                        start();
                        break;
                }

            } catch (DPFPImageQualityException e) {
                enviarTexto("Error: " + e.getMessage());
            }
        }
    }

    public void verificarHuella() {
        try {
            Connection c = con.conectar();
            PreparedStatement identificarStmt = c.prepareStatement("select * from tswhuella_administrativo");
            ResultSet rs = identificarStmt.executeQuery();
            while (rs.next()) {
                byte templateBuffer[] = rs.getBytes("chuella");
                String claveDocente = rs.getString("cclaveadministrativo");
                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);

                setTemplate(referenceTemplate);
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    claveEmpleadoActual = claveDocente;
                    panelHuella.setBackground(Color.decode("#1c6c1c"));
                    return;
                }
            }
            JOptionPane.showMessageDialog(null, "No hay registro que coincida con la huella digital", "Error", JOptionPane.ERROR_MESSAGE);
            setTemplate(null);
        } catch (SQLException e) {
            enviarTexto("Error al verificar huella dactilar. " + e.getMessage());
        }
        reiniciarLectura();
    }

    protected void iniciar() {
        lector.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(final DPFPDataEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("La huella ha sido capturada ");
                        procesarCaptura(e.getSample());
                    }
                });
            }
        });
        lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("El sensor esta activado o conectado");
                    }
                });
            }

            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("El sensor esta desactivado o no conectado");
                    }
                });
            }
        });
        lector.addSensorListener(new DPFPSensorAdapter() {
            @Override
            public void fingerTouched(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("El dedo ha sido colocado sobre el lector de huella");
                    }
                });
            }

            @Override
            public void fingerGone(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("El dedo ha sido quitado del lector de huella");
                    }
                });
            }
        });
        lector.addErrorListener(new DPFPErrorAdapter() {
            //@Override
            public void errorReader(final DPFPErrorAdapter e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enviarTexto("Error: " + e.toString());
                    }
                });
            }
        });
    }

    ConnectionDAO con = new ConnectionDAO();

    public RegistroPOJO extraerHorario(String claveEmpleado, int diaActual, String hora) {
        Connection c = con.conectar();
        RegistroPOJO obj = null;

        try {
            PreparedStatement horarioStmt = c.prepareStatement("SELECT(to_timestamp(CURRENT_DATE || ' ' || '" + hora + "', 'YYYY-MM-DD HH24:mi:ss') -to_timestamp(CURRENT_DATE || ' ' || tswhorarios_administrativo.chora_entrada, 'YYYY-MM-DD HH24:mi:ss')) AS tiempo_restante, "
                    + " tswhorarios_administrativo.chora_entrada,  tswhorarios_administrativo.chora_salida, tswhorarios_administrativo.cdia, "
                    + " tswhuella_administrativo.cnombrecompleto, tswhuella_administrativo.cdepartamento "
                    + " FROM tswhorarios_administrativo "
                    + " INNER JOIN tswhuella_administrativo  ON tswhuella_administrativo.cclaveadministrativo= tswhorarios_administrativo.cclaveadministrativo "
                    + " WHERE  tswhorarios_administrativo.cdia= ? AND tswhorarios_administrativo.cclaveadministrativo= ? ");

            horarioStmt.setInt(1, diaActual);
            horarioStmt.setString(2, claveEmpleado);
            ResultSet rs = horarioStmt.executeQuery();

            while (rs.next()) {
                obj = new RegistroPOJO();
                obj.setClaveEmpleado(claveEmpleado);
                obj.setNombreEmpleado(rs.getString("cnombrecompleto"));
                obj.setDia(rs.getInt("cdia"));
                obj.setDepartamento(rs.getString("cdepartamento"));
                obj.setHoraEntrada(rs.getString("chora_entrada"));
                obj.setHoraSalida(rs.getString("chora_salida"));
                obj.setRetardo(rs.getString("tiempo_restante"));
            }
            if (obj == null) {
                JOptionPane.showMessageDialog(null, "Hoy no trabaja el empleado " + claveEmpleado, "Error", JOptionPane.ERROR_MESSAGE);
                clear();
                return null;
            }
        } catch (SQLException e) {
            enviarTexto("[Error] " + e.getMessage());
        }
        return obj;
    }

    public void registrarEntrada(int minutosActuales, RegistroPOJO obj) throws ParseException {
        Calendar c = Calendar.getInstance();
        int numeroDeLaSemana = c.get(Calendar.WEEK_OF_YEAR);
        int numDia = c.get(Calendar.DAY_OF_WEEK);
        int diaActualFormato = cambiarFormatoDia(numDia);
        String estado = "";
        int tipo = 0;
        int hora = convertirHora(obj.getRetardo());
        int minutos = convertirMinutos(obj.getRetardo());
        int entrada = buscarRegistroDeAsistencia(obj);
        System.out.println(obj.getDepartamento());
        if (!"VELADOR".equals(obj.getDepartamento())) {
            if (entrada == 0) {
                if (obj.getRetardo().length() == 9 || obj.getRetardo().length() == 10 || obj.getRetardo().length() == 11) {
                    if (hora == 0 && minutos <= 30) { //asistencia
                        estado = "A";
                        jpHoraRegistrada.setBackground(Color.GREEN);
                        obj.setRetardo("00:00:00");
                        insertAsistencia(obj, estado, tipo, diaActualFormato, numeroDeLaSemana);
                        JOptionPane.showMessageDialog(null, "Se registro entrada " + obj.getClaveEmpleado());
                        clear();
                        return;
                    } else {
                        JOptionPane.showMessageDialog(null, "Aun no puede registrar  el empleado  " + obj.getClaveEmpleado());
                        clear();
                        return;
                    }
                } else {
                    if (hora == 0 && minutos < 6) {
                        jpHoraRegistrada.setBackground(Color.GREEN);
                        estado = "A"; //asistencia
                        insertAsistencia(obj, estado, tipo, diaActualFormato, numeroDeLaSemana);
                        JOptionPane.showMessageDialog(null, "Se registro entrada del empleado " + obj.getClaveEmpleado());
                        clear();
                        return;

                    } else if (hora == 0 && minutos >= 6 && hora == 0 && minutos <= 10) {
                        jpHoraRegistrada.setBackground(Color.YELLOW);
                        estado = "B"; //retardo
                        insertAsistencia(obj, estado, tipo, diaActualFormato, numeroDeLaSemana);
                        JOptionPane.showMessageDialog(null, "Se registro entrada " + obj.getClaveEmpleado());
                        clear();
                        return;
                    } else if (hora == 0 && minutos >= 11) {
                        jpHoraRegistrada.setBackground(Color.LIGHT_GRAY);
                        estado = "C"; //falta 
                        insertAsistencia(obj, estado, tipo, diaActualFormato, numeroDeLaSemana);
                        JOptionPane.showMessageDialog(null, "Se registro entrada " + obj.getClaveEmpleado());
                        clear();
                        return;
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Ya registro entrada el empleado " + obj.getClaveEmpleado());
                clear();
                return;
            }
        } else {
            /* este else es para que el velador registre cada hora su entrada, ya que es una forma de verificar que esten despiertos en la noche*/
            estado = "A";
            tipo = 1;
            jpHoraRegistrada.setBackground(Color.GREEN);
            obj.setRetardo("00:00:00");
            insertAsistencia(obj, estado, tipo, diaActualFormato, numeroDeLaSemana);
            JOptionPane.showMessageDialog(null, "Se registro entrada " + obj.getClaveEmpleado());
            clear();
            return;
        }

        JOptionPane.showMessageDialog(null, "Hoy no trabaja el empleado  " + obj.getClaveEmpleado());
        clear();
    }
    public void registrarSalida(String claveEmpleado) {
        RegistroPOJO obj = datosEmpleado(claveEmpleado);
        String fecha = buscarUltimaEntrada(claveEmpleado);
        updateAsistencia(obj, fecha);
    }

    public String buscarUltimaEntrada(String claveEmpleado) {

        Date today = (Date) Calendar.getInstance().getTime();
        String fechaActual = formatter1.format(today);
        Connection c = con.conectar();
        String horaDeEntrada = "prueba";
        try {
            PreparedStatement buscarEntradaStmt = c.prepareStatement("SELECT cfecha FROM tswasistencia_administrativo WHERE "
                    + " cclaveadministrativo = ? AND  chorasalida IS NULL ORDER BY cfecha DESC LIMIT 1");
            buscarEntradaStmt.setString(1, claveEmpleado);
            ResultSet rs = buscarEntradaStmt.executeQuery();
            while (rs.next()) {
                horaDeEntrada = rs.getString("cfecha");
            }

        } catch (SQLException e) {
            enviarTexto("[Error] " + e.getMessage());
        }
        return horaDeEntrada;
    }

    public int buscarRegistroDeAsistencia(RegistroPOJO obj) {
        Date today = (Date) Calendar.getInstance().getTime();
        String fechaActual = formatter1.format(today);
        Connection c = con.conectar();
        int entrada = 0;
        String horaDeEntrada = "";
        try {
            PreparedStatement buscarEntradaStmt = c.prepareStatement("SELECT cestado "
                    + "	FROM tswasistencia_administrativo WHERE cfecha= '" + fechaActual + "' AND cclaveadministrativo = ?");
            buscarEntradaStmt.setString(1, obj.getClaveEmpleado());
            ResultSet rs = buscarEntradaStmt.executeQuery();
            while (rs.next()) {
                horaDeEntrada = rs.getString("cestado");
            }

        } catch (SQLException e) {
            enviarTexto("[Error] " + e.getMessage());
        }
        if (horaDeEntrada != "") {
            entrada = 1;
        } else {
            entrada = 0;
        }
        return entrada;
    }

    public RegistroPOJO datosEmpleado(String claveEmpleado) {
        Date today = (Date) Calendar.getInstance().getTime();
        Connection c = con.conectar();
        RegistroPOJO obj = null;
        try {
            PreparedStatement buscarEmpleadoStmt = c.prepareStatement("SELECT cdepartamento, cnombrecompleto FROM tswhuella_administrativo WHERE  "
                    + "cclaveadministrativo= ?");
            buscarEmpleadoStmt.setString(1, claveEmpleado);
            ResultSet rs = buscarEmpleadoStmt.executeQuery();
            while (rs.next()) {
                obj = new RegistroPOJO();
                obj.setDepartamento(rs.getString("cdepartamento"));
                obj.setNombreEmpleado(rs.getString("cnombrecompleto"));
                obj.setClaveEmpleado(claveEmpleado);
            }

        } catch (SQLException e) {
            enviarTexto("[Error] " + e.getMessage());
        }

        return obj;
    }

    public void updateAsistencia(RegistroPOJO obj, String fecha) {
        Calendar calendar = new GregorianCalendar();
        Date today = (Date) Calendar.getInstance().getTime();
        int horaActual = calendar.get(Calendar.HOUR_OF_DAY);
        String horaSalida = formatter2.format(today);
        int estado = 0;
        Connection c = con.conectar();
        try {
            PreparedStatement updateAsistenciaStmt = c.prepareStatement("UPDATE tswasistencia_administrativo "
                    + "	SET   chorasalida= ? "
                    + "	WHERE   cclaveadministrativo= ?  AND  cfecha = '" + fecha + "' ;");

            updateAsistenciaStmt.setString(1, horaSalida);
            updateAsistenciaStmt.setString(2, obj.getClaveEmpleado());
            estado = updateAsistenciaStmt.executeUpdate();
            if (estado != 0) {
                jlnombreadministrativo.setText(obj.getNombreEmpleado());
                jlclaveadministrativo.setText(obj.getClaveEmpleado());
                jlhoraRegistrada.setText(horaSalida);
                jldepartamento.setText(obj.getDepartamento());

                JOptionPane.showMessageDialog(null, "Se registro Salida");
                clear();
            } else {
                JOptionPane.showMessageDialog(null, "Error en parametros para registrar salida");
                clear();
            }

        } catch (SQLException ex) {
            enviarTexto(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage());
            estado = 0;
        }
    }

    public void insertAsistencia(RegistroPOJO obj, String estado, int tipo, int dia, int semana) {
        Date today = (Date) Calendar.getInstance().getTime();
        String fechaActual = formatter1.format(today);
        String horaActual = formatter2.format(today);
        Connection c = con.conectar();
        try {
            PreparedStatement insertASistenciaStmt = c.prepareStatement(
                    "INSERT INTO tswasistencia_administrativo( "
                    + "	cfecha,cclaveadministrativo, choraentrada,cminutos,cestado,ctipo,cdia, csemana) "
                    + "	VALUES ('" + fechaActual + "',?,?, ?, ?,?,?,?)");
            insertASistenciaStmt.setString(1, obj.getClaveEmpleado());
            insertASistenciaStmt.setString(2, horaActual);
            insertASistenciaStmt.setString(3, obj.getRetardo());
            insertASistenciaStmt.setString(4, estado);
            insertASistenciaStmt.setInt(5, tipo);
            insertASistenciaStmt.setInt(6, dia);
            insertASistenciaStmt.setInt(7, semana);
            insertASistenciaStmt.executeUpdate();
            jlnombreadministrativo.setText(obj.getNombreEmpleado());
            jlclaveadministrativo.setText(obj.getClaveEmpleado());
            jlhoraRegistrada.setText(horaActual);
            jldepartamento.setText(obj.getDepartamento());
            jlEstado.setText("(" + estadoRegistrado(estado) + ")");

        } catch (SQLException ex) {
            Logger.getLogger(LectorHuellaJframe.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clear() {
        jpHoraRegistrada.setBackground(Color.decode("#dedede"));
        jlEstado.setText("");
        jlnombreadministrativo.setText("");
        jlclaveadministrativo.setText("");
        jlhoraRegistrada.setText("");
        jldepartamento.setText("");
        panelHuella.setBackground(Color.darkGray);
    }

    public void reiniciarLectura() {
        stop();
        setTemplate(null);
        reclutador.clear();
        lblImagen.setIcon(null);
        claveEmpleadoActual = "";
        start();
    }

    public void cerrarSesion() {
        con.desconectar();
    }

    public int convertirMinutos(String horario) {
        Date date = null;
        try {
            date = formatter5.parse(horario);
        } catch (ParseException ex) {
            Logger.getLogger(LectorHuellaJframe.class.getName()).log(Level.SEVERE, null, ex);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int minutosActuales = cal.get(Calendar.MINUTE);
        return minutosActuales;
    }

    public int convertirHora(String horario) {
        if (horario.length() == 9) {
            horario = horario.substring(1, 9);
        }
        Date date = null;
        try {
            date = formatter5.parse(horario);
        } catch (ParseException ex) {
            Logger.getLogger(LectorHuellaJframe.class.getName()).log(Level.SEVERE, null, ex);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int horaActuales = cal.get(Calendar.HOUR_OF_DAY);

        return horaActuales;
    }

    public int cambiarFormatoDia(int dia) {
        int diaCambio = 6;
        switch (dia) {
            case 1:
                diaCambio = 6;
                break;
            case 2:
                diaCambio = 0;
                break;
            case 3:
                diaCambio = 1;
                break;
            case 4:
                diaCambio = 2;
                break;
            case 5:
                diaCambio = 3;
                break;
            case 6:
                diaCambio = 4;
                break;
            case 7:
                diaCambio = 5;
                break;
        }
        return diaCambio;

    }

    public String estadoRegistrado(String estado) {
        String textoEstado = "";
        if (estado.equals("A")) {
            textoEstado = "Asistencia";
        } else if (estado.equals("B")) {
            textoEstado = "Retardo";
        } else if (estado.equals("C")) {
            textoEstado = "Falta";
        }
        return textoEstado;
    }

    public class horaSistema implements ActionListener {

        public void actionPerformed(ActionEvent evt) {
            int hora, minutos, segundos;
            Date today = (Date) Calendar.getInstance().getTime();
            String fechaActual = formatter4.format(today);
            GregorianCalendar tiempo = new GregorianCalendar();
            hora = tiempo.get(Calendar.HOUR_OF_DAY);
            minutos = tiempo.get(Calendar.MINUTE);
            segundos = tiempo.get(Calendar.SECOND);

            jlHora.setText(String.valueOf(hora));
            jlMinutos.setText(String.valueOf(minutos));
            jlSegundos.setText(String.valueOf(segundos));
            jlFechaSistema.setText(fechaActual);

        }
    }

    @Override
    public Image getIconImage() {
        Image retValue = Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("mx/edu/univo/resources/iconreloj.png"));

        return retValue;
    }

    public LectorHuellaJframe() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Imposible cambiar el tema" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jldepartamento = new javax.swing.JLabel();
        label3 = new java.awt.Label();
        jLabel1 = new javax.swing.JLabel();
        jlclaveadministrativo = new javax.swing.JLabel();
        estadosHuella = new java.awt.TextArea();
        jLabel13 = new javax.swing.JLabel();
        jlnombreadministrativo = new javax.swing.JLabel();
        btnSalida = new java.awt.Button();
        btnEntrada = new java.awt.Button();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jlSegundos = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jlMinutos = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jlHora = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jlFechaSistema = new javax.swing.JLabel();
        panelHuella = new java.awt.Panel();
        lblImagen = new javax.swing.JLabel();
        label2 = new java.awt.Label();
        jLabel4 = new javax.swing.JLabel();
        jpHoraRegistrada = new javax.swing.JPanel();
        jlEstado = new javax.swing.JLabel();
        jlhoraRegistrada = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMOpciones = new javax.swing.JMenu();
        jMIRegistrarUsuario = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("REGISTRAR ASISTENCIA");
        setIconImage(getIconImage());
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jldepartamento.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        label3.setAlignment(java.awt.Label.CENTER);
        label3.setBackground(new java.awt.Color(12, 72, 106));
        label3.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        label3.setForeground(new java.awt.Color(255, 255, 255));
        label3.setText("Información");

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel1.setText("Clave:");

        jlclaveadministrativo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        estadosHuella.setEditable(false);
        estadosHuella.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        estadosHuella.setForeground(new java.awt.Color(51, 51, 51));
        estadosHuella.setName(""); // NOI18N

        jLabel13.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel13.setText("Empleado:");

        jlnombreadministrativo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        btnSalida.setBackground(new java.awt.Color(102, 102, 102));
        btnSalida.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        btnSalida.setForeground(new java.awt.Color(255, 255, 255));
        btnSalida.setLabel("Salida");
        btnSalida.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalidaActionPerformed(evt);
            }
        });

        btnEntrada.setBackground(new java.awt.Color(204, 6, 103));
        btnEntrada.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnEntrada.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        btnEntrada.setForeground(new java.awt.Color(255, 255, 255));
        btnEntrada.setLabel("Entrada");
        btnEntrada.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEntradaActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel6.setBackground(new java.awt.Color(0, 153, 153));
        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(12, 72, 106));
        jLabel6.setText("Hora del Sistema:");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(12, 72, 106));
        jLabel8.setText("Hrs");

        jlSegundos.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jlSegundos.setForeground(new java.awt.Color(12, 72, 106));
        jlSegundos.setText("00");

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(12, 72, 106));
        jLabel12.setText(":");

        jlMinutos.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jlMinutos.setForeground(new java.awt.Color(12, 72, 106));
        jlMinutos.setText("00");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(12, 72, 106));
        jLabel9.setText(":");

        jlHora.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jlHora.setForeground(new java.awt.Color(12, 72, 106));
        jlHora.setText("00");

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mx/edu/univo/resources/logunivmin.jpg"))); // NOI18N

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(12, 72, 106));
        jLabel7.setText("Fecha:");

        jlFechaSistema.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jlFechaSistema.setForeground(new java.awt.Color(12, 72, 106));
        jlFechaSistema.setText("00-00-00");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(101, 101, 101)
                        .addComponent(jLabel7))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(jlFechaSistema)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jlHora)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlMinutos)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlSegundos)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8)))
                .addGap(45, 45, 45))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jlFechaSistema))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jlHora)
                            .addComponent(jLabel9)
                            .addComponent(jlMinutos)
                            .addComponent(jLabel12)
                            .addComponent(jlSegundos)
                            .addComponent(jLabel8))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelHuella.setBackground(new java.awt.Color(153, 153, 153));

        lblImagen.setBackground(new java.awt.Color(255, 255, 255));

        label2.setAlignment(java.awt.Label.CENTER);
        label2.setBackground(new java.awt.Color(12, 72, 106));
        label2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        label2.setForeground(new java.awt.Color(255, 255, 255));
        label2.setText("Huella");

        javax.swing.GroupLayout panelHuellaLayout = new javax.swing.GroupLayout(panelHuella);
        panelHuella.setLayout(panelHuellaLayout);
        panelHuellaLayout.setHorizontalGroup(
            panelHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelHuellaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelHuellaLayout.setVerticalGroup(
            panelHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelHuellaLayout.createSequentialGroup()
                .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel4.setText("Departamento:");

        jlEstado.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jlhoraRegistrada.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel3.setText("Hora registrada:");

        javax.swing.GroupLayout jpHoraRegistradaLayout = new javax.swing.GroupLayout(jpHoraRegistrada);
        jpHoraRegistrada.setLayout(jpHoraRegistradaLayout);
        jpHoraRegistradaLayout.setHorizontalGroup(
            jpHoraRegistradaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHoraRegistradaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jlhoraRegistrada)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jlEstado)
                .addGap(102, 102, 102))
        );
        jpHoraRegistradaLayout.setVerticalGroup(
            jpHoraRegistradaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpHoraRegistradaLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jpHoraRegistradaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jlEstado)
                    .addComponent(jlhoraRegistrada)
                    .addComponent(jLabel3)))
        );

        jMOpciones.setText("Opciones");

        jMIRegistrarUsuario.setText("Registrar Usuario");
        jMIRegistrarUsuario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRegistrarUsuarioActionPerformed(evt);
            }
        });
        jMOpciones.add(jMIRegistrarUsuario);

        jMenuBar1.add(jMOpciones);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(panelHuella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEntrada, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(btnSalida, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(22, 22, 22))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel13)
                            .addComponent(jLabel4))
                        .addGap(38, 38, 38)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jlnombreadministrativo)
                            .addComponent(jlclaveadministrativo)
                            .addComponent(jldepartamento))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGap(38, 38, 38)
                        .addComponent(jpHoraRegistrada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addComponent(estadosHuella, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(label3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jpHoraRegistrada, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jlnombreadministrativo)
                                .addGap(25, 25, 25)
                                .addComponent(jlclaveadministrativo)
                                .addGap(78, 78, 78))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel13)
                                        .addGap(24, 24, 24)
                                        .addComponent(jLabel1)
                                        .addGap(46, 46, 46))
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(jldepartamento)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnSalida, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnEntrada, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(panelHuella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(estadosHuella, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        Calendar calendar = new GregorianCalendar();
        timer = new Timer(1000, new horaSistema());
        timer.start();
        iniciar();
        start();
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void jMIRegistrarUsuarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRegistrarUsuarioActionPerformed
        reclutador.clear();
        stop();
        LoginJframe login = new LoginJframe();
        this.setVisible(false);
        login.setVisible(true);
    }//GEN-LAST:event_jMIRegistrarUsuarioActionPerformed

    private void btnSalidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalidaActionPerformed
        Calendar calendar = new GregorianCalendar();
        int diaActual = calendar.get(Calendar.DAY_OF_WEEK);
        int horaActual = calendar.get(Calendar.HOUR_OF_DAY);
        if ("".equals(claveEmpleadoActual)) {
            JOptionPane.showMessageDialog(null, "Ingrese su huella",
                    "Mensaje", JOptionPane.INFORMATION_MESSAGE);
            reiniciarLectura();
        } else {
            registrarSalida(claveEmpleadoActual);
            reiniciarLectura();
            cerrarSesion();
        }


    }//GEN-LAST:event_btnSalidaActionPerformed

    private void btnEntradaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEntradaActionPerformed
        try {
            Calendar calendar = new GregorianCalendar();
            Date today = (Date) Calendar.getInstance().getTime();
            int diaActual = calendar.get(Calendar.DAY_OF_WEEK);
            String horaMinutos = formatter5.format(today);
            int diaActualFormato = cambiarFormatoDia(diaActual);
            if ("".equals(claveEmpleadoActual)) {
                JOptionPane.showMessageDialog(null, "Ingrese su huella",
                        "Mensaje", JOptionPane.INFORMATION_MESSAGE);
                reiniciarLectura();
            } else {
                RegistroPOJO horario = extraerHorario(claveEmpleadoActual, diaActualFormato, horaMinutos);
                if (horario != null) {
                    registrarEntrada(diaActualFormato, horario);
                    reiniciarLectura();
                    cerrarSesion();
                }
                reiniciarLectura();
                cerrarSesion();
            }
        } catch (ParseException ex) {
            Logger.getLogger(LectorHuellaJframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnEntradaActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LectorHuellaJframe.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LectorHuellaJframe.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LectorHuellaJframe.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LectorHuellaJframe.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LectorHuellaJframe().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Button btnEntrada;
    private java.awt.Button btnSalida;
    private java.awt.TextArea estadosHuella;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMIRegistrarUsuario;
    private javax.swing.JMenu jMOpciones;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel jlEstado;
    private javax.swing.JLabel jlFechaSistema;
    private javax.swing.JLabel jlHora;
    private javax.swing.JLabel jlMinutos;
    private javax.swing.JLabel jlSegundos;
    private javax.swing.JLabel jlclaveadministrativo;
    private javax.swing.JLabel jldepartamento;
    private javax.swing.JLabel jlhoraRegistrada;
    private javax.swing.JLabel jlnombreadministrativo;
    private javax.swing.JPanel jpHoraRegistrada;
    private java.awt.Label label2;
    private java.awt.Label label3;
    private javax.swing.JLabel lblImagen;
    private java.awt.Panel panelHuella;
    // End of variables declaration//GEN-END:variables
}
