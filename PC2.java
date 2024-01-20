import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class PC2 extends JFrame {
    private static final int receiverPort = 12345;


    private final JTextArea messagesArea;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton sendFileButton;
    private final DatagramSocket socket;
    private DatagramPacket lastReceivePacket;
    private int receivedMessagesCount = 0;
    private int sentMessagesCount = 0;
    private final JLabel receivedMessagesLabel;
    private final JLabel sentMessagesLabel;
    private final JProgressBar progressBar;  // Add a progress bar


    private class ImagePanel extends JPanel {
        private BufferedImage background;


        public ImagePanel(String imagePath) {
            try {
                background = ImageIO.read(new File(imagePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (background != null) {
                g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }


    public PC2() throws Exception {
        socket = new DatagramSocket(receiverPort);


        messagesArea = new JTextArea(10, 30);
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Century Gothic", Font.BOLD, 20));
        JScrollPane scrollPane = new JScrollPane(messagesArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


        messageField = new JTextField(20);


        ImageIcon sendIcon = new ImageIcon("C:/Users/KIIT/Downloads/send_10322482.png");
        Image scaledSendIcon = sendIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        sendIcon = new ImageIcon(scaledSendIcon);
        sendButton = new JButton(sendIcon);


        sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(e -> sendFile());


        receivedMessagesLabel = new JLabel("Received Messages: 0");
        sentMessagesLabel = new JLabel("Sent Messages: 0");


        sendButton.addActionListener(e -> sendResponse());


        ImagePanel topPanel = new ImagePanel("C:/Users/KIIT/Downloads/22489089_15199.jpg");
        topPanel.setLayout(new BorderLayout());


        ImageIcon logoIcon = new ImageIcon("C:/Users/KIIT/Downloads/drdo.png");
        Image scaledLogo = logoIcon.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
        logoIcon = new ImageIcon(scaledLogo);
        JLabel logoLabel = new JLabel(logoIcon);
        topPanel.add(logoLabel, BorderLayout.WEST);


        JLabel titleLabel = new JLabel("  SecureCommand Network : PC - 2");
        titleLabel.setFont(new Font("Bookman Old Style", Font.BOLD, 50));
        topPanel.add(titleLabel, BorderLayout.CENTER);


        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        counterPanel.add(receivedMessagesLabel);
        counterPanel.add(Box.createRigidArea(new Dimension(20, 0))); // Add some spacing
        counterPanel.add(sentMessagesLabel);


        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(counterPanel, BorderLayout.NORTH);


        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendFileButton, BorderLayout.WEST);


        progressBar = new JProgressBar();
        progressBar.setStringPainted(true); // Display progress as a percentage
        inputPanel.add(progressBar, BorderLayout.SOUTH); // Add progress bar to the input panel


        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.NORTH);


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);


        setContentPane(mainPanel);


        setTitle("UDP Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);


        receiveMessages();
    }


    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);


        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();


            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        if (lastReceivePacket != null) {
                            byte[] fileData = readFile(selectedFile);


                            String fileContent = new String(fileData);
                            String[] lines = fileContent.split("\\r?\\n");  // Split file content into lines


                            final int totalLines = lines.length;
                            final int[] sentLines = {0};  // Use an array to make it effectively final


                            for (int i = 0; i < lines.length; i++) {
                                // Send one line at a time
                                String line = lines[i];
                                byte[] sendData = line.getBytes();


                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                        lastReceivePacket.getAddress(), lastReceivePacket.getPort());
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


                            SwingUtilities.invokeLater(() -> {
                                messagesArea.append("File sent to " + lastReceivePacket.getAddress() +
                                        ":" + lastReceivePacket.getPort() + ": " + selectedFile.getName() + "\n");
                                messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
                            });


                            sentMessagesCount++;
                            sentMessagesLabel.setText("Sent Messages: " + sentMessagesCount);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };


            worker.execute();
        }
    }


    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {


            byte[] buffer = new byte[1024];
            int bytesRead;


            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }


            return bos.toByteArray();
        }
    }


    private void sendResponse() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (lastReceivePacket != null) {
                        String message = messageField.getText();
                        byte[] sendData = message.getBytes();


                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                lastReceivePacket.getAddress(), lastReceivePacket.getPort());
                        socket.send(sendPacket);


                        SwingUtilities.invokeLater(() -> {
                            messagesArea.append("Message sent to " + lastReceivePacket.getAddress() +
                                    ":" + lastReceivePacket.getPort() + ": " + message + "\n");
                            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
                        });


                        messageField.setText("");


                        sentMessagesCount++;
                        sentMessagesLabel.setText("Sent Messages: " + sentMessagesCount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };


        worker.execute();
    }


    private void receiveMessages() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    while (true) {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);
                        lastReceivePacket = receivePacket;
                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());


                        publish(receivedMessage);


                        if (receivedMessage.equalsIgnoreCase("exit")) {
                            publish("Communication Terminated\n");
                            socket.close();
                            System.exit(0);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }


            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    messagesArea.append("Received Message: " + message + "\n");
                    messagesArea.setCaretPosition(messagesArea.getDocument().getLength());


                    receivedMessagesCount++;
                    receivedMessagesLabel.setText("Received Messages: " + receivedMessagesCount);
                }
            }
        };


        worker.execute();
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new PC2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

