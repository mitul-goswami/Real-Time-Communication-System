import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;


class BackgroundPanel extends JPanel {
    private ImageIcon backgroundImage;


    public BackgroundPanel(String imagePath) {
        setBackgroundImage(imagePath);
    }


    public void setBackgroundImage(String imagePath) {
        this.backgroundImage = new ImageIcon(imagePath);
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage.getImage(), 0, 0, getWidth(), getHeight(), this);
        }
    }
}


public class PC1 extends JFrame {
    private static final String receiverIP = "10.2.101.234";
    private static final int receiverPort = 12345;


    private final JTextArea messagesArea;
    private final JTextField messageField;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JButton sendFileButton;
    private final DatagramSocket socket;
    private int sentMessagesCount = 0;
    private int receivedMessagesCount = 0;
    private final JLabel sentMessagesLabel;
    private final JLabel receivedMessagesLabel;
    private JFileChooser fileChooser;
    private JPanel inputPanel;
    private final JProgressBar progressBar;  // Add a progress bar


    public PC1() throws Exception {
        socket = new DatagramSocket();
        fileChooser = new JFileChooser();


        messagesArea = new JTextArea(20, 20);
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Century Gothic", Font.BOLD, 20));
        JScrollPane scrollPane = new JScrollPane(messagesArea);


        messageField = new JTextField(40);
        inputField = new JTextField(50);


        ImageIcon sendIcon = new ImageIcon("C:/Users/KIIT/Downloads/send_10322482.png");
        Image scaledSendIcon = sendIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        sendIcon = new ImageIcon(scaledSendIcon);
        sendButton = new JButton(sendIcon);


        sendFileButton = new JButton("Send File");


        sendButton.addActionListener(e -> sendMessage());
        sendFileButton.addActionListener(e -> sendFile());


        ImageIcon mainLogoIcon = new ImageIcon("C:/Users/KIIT/Downloads/drdo.png");
        Image scaledMainLogo = mainLogoIcon.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
        mainLogoIcon = new ImageIcon(scaledMainLogo);
        JLabel mainLogoLabel = new JLabel(mainLogoIcon);


        BackgroundPanel topPanel = new BackgroundPanel("C:/Users/KIIT/Downloads/22489089_15199.jpg");
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER));


        Font titleFont = new Font("Bookman Old Style", Font.BOLD, 50);
        JTextField titleLabel = new JTextField("  SecureCommand Network : PC - 1");
        titleLabel.setFont(titleFont);
        titleLabel.setEditable(false);
        titleLabel.setBorder(BorderFactory.createEmptyBorder());
        titleLabel.setOpaque(false);


        JButton changeBackgroundButton = new JButton("Change Background");
        changeBackgroundButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(PC1.this);


            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String imagePath = selectedFile.getAbsolutePath();
                topPanel.setBackgroundImage(imagePath);
            }
        });


        topPanel.add(mainLogoLabel);
        topPanel.add(titleLabel);
        topPanel.add(changeBackgroundButton);


        BackgroundPanel terminalPanel = new BackgroundPanel("C:/Users/KIIT/Downloads/terminal_background.jpg");
        terminalPanel.setLayout(new BorderLayout());
        terminalPanel.add(scrollPane, BorderLayout.CENTER);


        inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendFileButton, BorderLayout.WEST);


        progressBar = new JProgressBar();
        progressBar.setStringPainted(true); // Display progress as a percentage
        inputPanel.add(progressBar, BorderLayout.SOUTH); // Add progress bar to the input panel


        sentMessagesLabel = new JLabel("Sent Messages: 0");
        receivedMessagesLabel = new JLabel("Received Messages: 0");


        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        counterPanel.add(sentMessagesLabel);
        counterPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        counterPanel.add(receivedMessagesLabel);


        inputPanel.add(counterPanel, BorderLayout.NORTH);


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(terminalPanel, BorderLayout.CENTER);
        mainPanel.add(messageField, BorderLayout.SOUTH);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);


        setContentPane(mainPanel);


        setTitle("UDP Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);


        receiveMessages();
    }


    private void sendMessage() {
        try {
            String message = inputField.getText();
            byte[] sendData = message.getBytes();


            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    InetAddress.getByName(receiverIP), receiverPort);


            socket.send(sendPacket);


            if (message.equalsIgnoreCase("exit")) {
                messagesArea.append("Communication Terminated\n");
                socket.close();
                System.exit(0);
            }


            messagesArea.append("Message sent to " + receiverIP + ":" + receiverPort + ": " + message + "\n");
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());


            inputField.setText("");


            sentMessagesCount++;
            sentMessagesLabel.setText("Sent Messages: " + sentMessagesCount);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendFile() {
        int result = fileChooser.showOpenDialog(PC1.this);


        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();


            try {
                // Read file content
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());


                // Send file content in a separate thread
                new Thread(() -> sendFileInBackground(fileData, selectedFile.getName())).start();


            } catch (IOException e) {
                e.printStackTrace();
                messagesArea.append("Error reading the file.\n");
            }
        }
    }


    private void sendFileInBackground(byte[] fileData, String fileName) {
        try {
            String fileContent = new String(fileData);
            String[] lines = fileContent.split("\\r?\\n");  // Split file content into lines


            final int totalLines = lines.length;
            final int[] sentLines = {0};  // Use an array to make it effectively final


            for (int i = 0; i < lines.length; i++) {
                // Send one line at a time
                String line = lines[i];
                byte[] sendData = line.getBytes();


                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        InetAddress.getByName(receiverIP), receiverPort);


                socket.send(sendPacket);
                sentLines[0]++;


                // Update progress bar on the event dispatch thread
                SwingUtilities.invokeLater(() -> {
                    int progress = (int) ((double) sentLines[0] / totalLines * 100);
                    progressBar.setValue(progress);
                    progressBar.setString("Sending: " + progress + "%");


                    // If all lines are sent, set the progress bar to full
                    if (sentLines[0] == totalLines) {
                        progressBar.setValue(100);
                        progressBar.setString("Sending: 100%");
                    }
                });


                Thread.sleep(1000);  // Sleep for 1 second before sending the next line
            }


            messagesArea.append("File sent to " + receiverIP + ":" + receiverPort + ": " + fileName + "\n");
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());


            sentMessagesCount++;
            sentMessagesLabel.setText("Sent Messages: " + sentMessagesCount);


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            messagesArea.append("Error sending the file.\n");
        }
    }


    private void receiveMessages() {
        new Thread(() -> {
            try {
                while (true) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());


                    SwingUtilities.invokeLater(() -> {
                        messagesArea.append("Received Response: " + receivedMessage + "\n");
                        messagesArea.setCaretPosition(messagesArea.getDocument().getLength());


                        if (receivedMessage.equalsIgnoreCase("exit")) {
                            messagesArea.append("Communication Terminated\n");
                            socket.close();
                            System.exit(0);
                        }


                        receivedMessagesCount++;
                        receivedMessagesLabel.setText("Received Messages: " + receivedMessagesCount);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new PC1();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
