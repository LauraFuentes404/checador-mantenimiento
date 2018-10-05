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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author Laura
 */
public class AdministradorJframe extends javax.swing.JFrame {

    /**
     * Creates new form CapturarHuella
     */
    private DPFPCapture lector = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
    private DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";

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
        lblImagen2.setIcon(new ImageIcon(image.getScaledInstance(lblImagen2.getWidth(), lblImagen2.getHeight(), image.SCALE_DEFAULT)));
        repaint();
    }

    public void estadoHuellas() {
        enviarTexto("Muestras de huellas necesarias para guardar template "
                + reclutador.getFeaturesNeeded());
    }

    public void enviarTexto(String string) {
        textArea1.setText(textArea1.getText() + "\n" + string);
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

    public void procesarCaptura(DPFPSample sample) {
        featuresInscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresVerificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if (featuresInscripcion != null) {
            try {
                System.out.println("Las caracteristicas de la huella han sido creadas");
                reclutador.addFeatures(featuresInscripcion);
                Image image = crearImagenHuella(sample);
                dibujarHuella(image);

                estadoHuellas();
                System.out.println(reclutador.getTemplateStatus().toString());
                switch (reclutador.getTemplateStatus()) {
                    case TEMPLATE_STATUS_INSUFFICIENT:
                        stop();
                        estadoHuellas();
                        setTemplate(null);
                        start();
                        JOptionPane.showMessageDialog(null, "Se requiere otra muestra",
                                "Mensaje", JOptionPane.INFORMATION_MESSAGE);
                        enviarTexto("muestra insuficiente");
                        break;
                    case TEMPLATE_STATUS_READY:
                        panel1.setBackground(Color.decode("#1c6c1c"));
                        stop();
                        setTemplate(reclutador.getTemplate());
                        JOptionPane.showMessageDialog(null, "Se capturo huella");
                        enviarTexto("La plantilla de la huella ha sido creada, ya puede verificarla");
                        btnActualizar.setEnabled(true);
                        btnGuardar.setEnabled(true);
                        break;
                    case TEMPLATE_STATUS_FAILED:
                        reclutador.clear();
                        stop();
                        estadoHuellas();
                        setTemplate(null);
                        start();
                        JOptionPane.showMessageDialog(null, "Fallo captura, coloque de nuevo la huella",
                                "[ERROR]", JOptionPane.ERROR_MESSAGE);

                        break;
                }

            } catch (DPFPImageQualityException e) {
                enviarTexto("Error: " + e.getMessage());
            }

        }
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

    public void guardarHuella() throws IOException {
        ByteArrayInputStream datosHuella = new ByteArrayInputStream(template.serialize());
        Integer tamanoHuella = template.serialize().length;
        try {
            Connection c = con.conectar();
            String cclaveEmpleado = (txtClaveEmpleado.getText());
            String cnombre = (txtNombreEmpleado.getText());
            String cdepartamento =(txtDepartamentoEmpleado.getText());
            if (!verificarClaveEmpleado(cclaveEmpleado).equals(cclaveEmpleado)) {
                PreparedStatement guardarStmt = c.prepareStatement("insert into tswhuella_administrativo (cclaveadministrativo,chuella,cnombrecompleto,cdepartamento) values (?,?,?,?)");
                guardarStmt.setString(1, cclaveEmpleado);
                guardarStmt.setBinaryStream(2, datosHuella);
                guardarStmt.setString(3, cnombre);
                guardarStmt.setString(4, cdepartamento);
                guardarStmt.execute();
                guardarStmt.close();
                JOptionPane.showMessageDialog(null, "huella guardada correctamente");

                reiniciarLectura();
                btnActualizar.setEnabled(false);
                btnGuardar.setEnabled(false);

                btnVerificar.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(null, "La clave del empleado " + cclaveEmpleado + " ya existe");
            }

        } catch (SQLException e) {
            enviarTexto("Error al guardar los datos de la huella");
            enviarTexto(e.getMessage());
        }
    }

    public void actualizarHuella() throws IOException {
        ByteArrayInputStream datosHuella = new ByteArrayInputStream(template.serialize());
        Integer tamanoHuella = template.serialize().length;
        String claveEmpleado = (txtClaveEmpleado.getText());
        try {
            Connection c = con.conectar();
            if (verificarClaveEmpleado(claveEmpleado).equals(claveEmpleado)) {
                PreparedStatement actualizarStmt = c.prepareStatement("UPDATE  tswhuella_administrativo  SET chuella=? WHERE cclaveadministrativo=?");
                actualizarStmt.setBinaryStream(1, datosHuella);
                actualizarStmt.setString(2, claveEmpleado);
                actualizarStmt.execute();
                actualizarStmt.close();
                JOptionPane.showMessageDialog(null, "huella guardada correctamente");
                reiniciarLectura();
                btnActualizar.setEnabled(false);
                btnGuardar.setEnabled(false);
                btnVerificar.setEnabled(true);
            } else {

                JOptionPane.showMessageDialog(null, "La clave docente " + claveEmpleado + " no existe");
            }

        } catch (SQLException e) {
            enviarTexto("Error al guardar los datos de la huella");
            enviarTexto(e.getMessage());
        }
    }

    public String verificarClaveEmpleado(String cclaveEmpleado) {
        String claveEmpleadoTempo = "";
        Connection c = con.conectar();
        try {
            PreparedStatement buscarEmpleadoStmt = c.prepareStatement("SELECT cclaveadministrativo "
                    + "	FROM public.tswhuella_administrativo where cclaveadministrativo=?");
            buscarEmpleadoStmt.setString(1, cclaveEmpleado);
            ResultSet rs = buscarEmpleadoStmt.executeQuery();
            while (rs.next()) {
                claveEmpleadoTempo = rs.getString("cclaveadministrativo");
            }
        } catch (SQLException e) {
            enviarTexto("[Error] " + e.getMessage());
        }
        return claveEmpleadoTempo;
    }

    public void verificarHuella() {
        try {
            String message = "";
            Connection c = con.conectar();
            PreparedStatement identificarStmt = c.prepareStatement("select * from tswhuella_administrativo");
            ResultSet rs = identificarStmt.executeQuery();
            while (rs.next()) {
                byte templateBuffer[] = rs.getBytes("chuella");
                String claveEmpleado = rs.getString("cclaveadministrativo");
                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);

                setTemplate(referenceTemplate);
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    message = "La huella capturada es del " + claveEmpleado;
                    
                }
            }
            JOptionPane.showMessageDialog(null, message);

        } catch (SQLException e) {
            enviarTexto("Error al identificar huella dactilar. " + e.getMessage());
        } finally {
            con.desconectar();
        }
        reiniciarLectura();
    }

    public void reiniciarLectura() {
        stop();
        setTemplate(null);
        panel1.setBackground(Color.darkGray);
        reclutador.clear();
        lblImagen2.setIcon(null);
        con.desconectar();
        start();
    }

    public AdministradorJframe() {
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

        lblImagen = new javax.swing.JLabel();
        panel1 = new java.awt.Panel();
        lblImagen2 = new javax.swing.JLabel();
        label2 = new java.awt.Label();
        jLabel1 = new javax.swing.JLabel();
        btnSalir = new java.awt.Button();
        btnActualizar = new java.awt.Button();
        btnGuardar = new java.awt.Button();
        btnVerificar = new java.awt.Button();
        textArea1 = new java.awt.TextArea();
        txtClaveEmpleado = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtNombreEmpleado = new javax.swing.JTextField();
        label3 = new java.awt.Label();
        jLabel3 = new javax.swing.JLabel();
        txtDepartamentoEmpleado = new javax.swing.JTextField();

        lblImagen.setBackground(new java.awt.Color(255, 255, 255));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ADMINISTRAR DATOS DE HUELLA");
        setBackground(new java.awt.Color(204, 204, 255));
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

        panel1.setBackground(java.awt.Color.gray);

        lblImagen2.setBackground(new java.awt.Color(255, 255, 255));

        label2.setAlignment(java.awt.Label.CENTER);
        label2.setBackground(new java.awt.Color(12, 72, 106));
        label2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        label2.setForeground(new java.awt.Color(255, 255, 255));
        label2.setText("Huella");

        javax.swing.GroupLayout panel1Layout = new javax.swing.GroupLayout(panel1);
        panel1.setLayout(panel1Layout);
        panel1Layout.setHorizontalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagen2, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panel1Layout.setVerticalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblImagen2, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel1.setText("Clave empleado:");

        btnSalir.setActionCommand("btnLogin");
        btnSalir.setBackground(new java.awt.Color(153, 0, 0));
        btnSalir.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnSalir.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        btnSalir.setForeground(new java.awt.Color(255, 255, 255));
        btnSalir.setLabel("Regresar");
        btnSalir.setName("btnSalir"); // NOI18N
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        btnActualizar.setBackground(new java.awt.Color(204, 102, 0));
        btnActualizar.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        btnActualizar.setForeground(new java.awt.Color(255, 255, 255));
        btnActualizar.setLabel("Actualizar");
        btnActualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarActionPerformed(evt);
            }
        });

        btnGuardar.setBackground(new java.awt.Color(204, 6, 103));
        btnGuardar.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        btnGuardar.setForeground(new java.awt.Color(255, 255, 255));
        btnGuardar.setLabel("Crear");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnVerificar.setBackground(new java.awt.Color(12, 72, 106));
        btnVerificar.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        btnVerificar.setForeground(new java.awt.Color(255, 255, 255));
        btnVerificar.setLabel("Verificar");
        btnVerificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerificarActionPerformed(evt);
            }
        });

        textArea1.setBackground(new java.awt.Color(204, 204, 204));
        textArea1.setFont(new java.awt.Font("Dialog", 0, 8)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabel2.setText("Nombre Completo:");

        label3.setAlignment(java.awt.Label.CENTER);
        label3.setBackground(new java.awt.Color(12, 72, 106));
        label3.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        label3.setForeground(new java.awt.Color(255, 255, 255));
        label3.setText("Información del empleado");

        jLabel3.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabel3.setText("Departamento:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(textArea1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnSalir, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(label3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(jLabel2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(jLabel3)
                                    .addGap(33, 33, 33)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtClaveEmpleado)
                            .addComponent(txtNombreEmpleado)
                            .addComponent(txtDepartamentoEmpleado)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 195, Short.MAX_VALUE)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtClaveEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtNombreEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtDepartamentoEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(44, 44, 44)
                        .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textArea1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        iniciar();
        start();
        estadoHuellas();

        btnActualizar.setEnabled(false);
        btnGuardar.setEnabled(false);
        btnVerificar.setEnabled(false);
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        reclutador.clear();
        stop();
        LectorHuellaJframe view = new LectorHuellaJframe();
        this.setVisible(false);
        view.setVisible(true);

    }//GEN-LAST:event_btnSalirActionPerformed

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarActionPerformed
        try {
            actualizarHuella();
        } catch (Exception e) {
            Logger.getLogger(AdministradorJframe.class.getName()).log(Level.SEVERE, null, e);
        }
    }//GEN-LAST:event_btnActualizarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        try {

            guardarHuella();
        } catch (Exception e) {
            Logger.getLogger(AdministradorJframe.class.getName()).log(Level.SEVERE, null, e);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnVerificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerificarActionPerformed
        verificarHuella();
    }//GEN-LAST:event_btnVerificarActionPerformed

    @Override
    public Image getIconImage() {
        Image retValue = Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("mx/edu/univo/resources/iconreloj.png"));

        return retValue;
    }

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
            java.util.logging.Logger.getLogger(AdministradorJframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AdministradorJframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AdministradorJframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AdministradorJframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
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
                new AdministradorJframe().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Button btnActualizar;
    private java.awt.Button btnGuardar;
    private java.awt.Button btnSalir;
    private java.awt.Button btnVerificar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private java.awt.Label label2;
    private java.awt.Label label3;
    private javax.swing.JLabel lblImagen;
    private javax.swing.JLabel lblImagen2;
    private java.awt.Panel panel1;
    private java.awt.TextArea textArea1;
    private javax.swing.JTextField txtClaveEmpleado;
    private javax.swing.JTextField txtDepartamentoEmpleado;
    private javax.swing.JTextField txtNombreEmpleado;
    // End of variables declaration//GEN-END:variables
}
