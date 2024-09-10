package ChatTCP;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatClient extends JFrame implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JTextField messageField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 40);
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel chatPanel = new JPanel(new BorderLayout());
    private JPanel clientListPanel = new JPanel(new BorderLayout());
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private String userName;
    private String serverAddress;
    private Map<String, JPanel> privateChatTabs = new HashMap<>();

    public ChatClient(String serverAddress, String userName) throws IOException {
        this.serverAddress = serverAddress;
        this.userName = userName;

        // Kết nối tới server
        try {
            socket = new Socket(serverAddress, 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Gửi tên người dùng tới server sau khi kết nối
            out.println(userName);
            
        } catch (IOException e) {
            showError("Unable to connect to the server.");
            throw e;
        }

        // Thiết lập giao diện Swing
        setTitle("Server Chat");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Thiết lập chat panel
        messageArea.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        JPanel sendPanel = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Gửi");
        sendPanel.add(messageField, BorderLayout.CENTER);
        sendPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(messageScrollPane, BorderLayout.CENTER);
        chatPanel.add(sendPanel, BorderLayout.SOUTH);

        // Thiết lập client list panel
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane clientListScrollPane = new JScrollPane(clientList);
        clientListPanel.add(clientListScrollPane);

        // Khi nhấp đúp vào danh sách người dùng, thêm một tab mới thay vì tạo cửa sổ
        clientList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String targetUser = clientList.getSelectedValue();
                    if (targetUser != null && !targetUser.equals(userName)) {
                        openPrivateChatTab(targetUser);
                    }
                }
            }
        });

        // Thêm các panel vào tabbedPane
        tabbedPane.addTab("Danh sách online", clientListPanel);
        tabbedPane.addTab("Chat tổng", chatPanel);
        getContentPane().add(tabbedPane);

        // Đặt cửa sổ ở giữa màn hình
        setLocationRelativeTo(null);

        // Xử lý sự kiện gửi tin nhắn
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });

        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });
    }

    // Phương thức mở một tab chat riêng
    private void openPrivateChatTab(String targetUser) {
        if (privateChatTabs.containsKey(targetUser)) {
            tabbedPane.setSelectedComponent(privateChatTabs.get(targetUser)); // Chuyển sang tab đã tồn tại
            return;
        }

        // Tạo panel chat mới
        JPanel privateChatPanel = new JPanel(new BorderLayout());
        JTextArea privateChatArea = new JTextArea();
        privateChatArea.setEditable(false);
        JScrollPane privateScrollPane = new JScrollPane(privateChatArea);
        JTextField privateMessageField = new JTextField();
        JButton sendPrivateButton = new JButton("Gửi");

        JPanel privateInputPanel = new JPanel(new BorderLayout());
        privateInputPanel.add(privateMessageField, BorderLayout.CENTER);
        privateInputPanel.add(sendPrivateButton, BorderLayout.EAST);

        privateChatPanel.add(privateScrollPane, BorderLayout.CENTER);
        privateChatPanel.add(privateInputPanel, BorderLayout.SOUTH);

        // Thêm tab vào JTabbedPane
        tabbedPane.addTab("Chat với " + targetUser, privateChatPanel);
        privateChatTabs.put(targetUser, privateChatPanel);
        tabbedPane.setSelectedComponent(privateChatPanel); // Chuyển sang tab mới

        // Gửi tin nhắn riêng khi nhấn nút "Gửi"
        sendPrivateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendPrivateMessage(targetUser, privateMessageField.getText(), privateChatArea);
            }
        });

        privateMessageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendPrivateMessage(targetUser, privateMessageField.getText(), privateChatArea);
            }
        });
    }

    // Phương thức gửi tin nhắn riêng
    private void sendPrivateMessage(String targetUser, String message, JTextArea chatArea) {
        if (!message.trim().isEmpty()) {
            out.println("/w " + targetUser + " " + message); // Gửi tin nhắn riêng đến server
            chatArea.append("Bạn: " + message + "\n"); // Hiển thị tin nhắn trong tab
            messageField.setText("");
        }
    }

    // Nhận và hiển thị tin nhắn riêng trong tab tương ứng
    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.contains("(Private):")) {
                int index = message.indexOf(":");
                if (index == -1) return; // Kiểm tra tính hợp lệ của chỉ số phân tách

                String sender = message.substring(0, index).trim();
                String privateMessage = message.substring(index + 1).trim();

                if (!privateChatTabs.containsKey(sender)) {
                    openPrivateChatTab(sender); // Mở tab mới nếu chưa có
                } else {
                    System.out.println("Tab đã tồn tại cho: " + sender);
                }

                // Kiểm tra lại để đảm bảo không gặp lỗi khi thêm tin nhắn
                if (privateChatTabs.containsKey(sender)) {
                    JTextArea chatArea = (JTextArea) ((JScrollPane) privateChatTabs.get(sender).getComponent(0)).getViewport().getView();
                    chatArea.append(sender + ": " + privateMessage + "\n");
                } else {
                    System.err.println("Error: User " + sender + " not found.");
                }
            } else {
                messageArea.append(message + "\n");
            }
        });
    }


    // Phương thức gửi tin nhắn với hỗ trợ nhắn tin riêng
    private void sendMessage(String message) {
        if (message.trim().isEmpty()) return;

        out.println(message); // Gửi tin nhắn tới server
        messageField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void start() throws IOException {
        // Hiển thị cửa sổ chat và kích hoạt nhập liệu
        setVisible(true);

        // Nhận tin nhắn từ server và hiển thị trong messageArea hoặc clientListArea
        String message;
        try {
            while ((message = in.readLine()) != null) {
                if (message.startsWith("CLIENT_LIST:")) {
                    updateClientList(message);
                } else {
                    appendMessage(message);
                }
            }
        } catch (IOException e) {
            showError("Connection to server lost.");
        } finally {
            closeResources();
        }
    }

    private void updateClientList(String message) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            String[] users = message.replace("CLIENT_LIST:", "").trim().split(",");
            for (String user : users) {
                user = user.trim();
                if (!user.isEmpty()) {
                    user = user.replace(",", "");
                    clientListModel.addElement(user);
                }
            }
        });
    }

    private void closeResources() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lớp cho cửa sổ chat riêng tư
    private class PrivateChatWindow extends JFrame {
        private String targetUser;
        private JTextArea chatArea;
        private JTextField inputField;

        public PrivateChatWindow(String targetUser) {
            this.targetUser = targetUser;
            setTitle("Chat với " + targetUser);
            setSize(400, 300);
            setLayout(new BorderLayout());

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);

            inputField = new JTextField();
            add(inputField, BorderLayout.SOUTH);

            inputField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sendPrivateMessage();
                }
            });

            // Đặt cửa sổ ở giữa màn hình
            setLocationRelativeTo(null);
        }

        private void sendPrivateMessage() {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                // Gửi tin nhắn riêng tư với cú pháp đặc biệt
                out.println("/w " + targetUser + " " + message);
                chatArea.append("Bạn: " + message + "\n");
                inputField.setText("");
            }
        }

        // Phương thức để nhận và hiển thị tin nhắn
        public void receiveMessage(String message) {
            chatArea.append(targetUser + ": " + message + "\n");
        }
    }

    public static void main(String[] args) {
        // Lấy địa chỉ IP của máy hiện tại
        String localIP = getLocalIP();
        
        // Yêu cầu người dùng nhập tên và hiển thị địa chỉ IP
        JPanel inputPanel = new JPanel();
        JTextField nameField = new JTextField(20);
        JTextField ipField = new JTextField(20);

        // Hiển thị địa chỉ IP của máy hiện tại trong trường ipField
        ipField.setText(localIP);
        ipField.setEditable(false); // Không cho phép người dùng chỉnh sửa địa chỉ IP

        inputPanel.add(new JLabel("Nhập tên hiển thị:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Địa chỉ IP máy:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Nhập địa chỉ kết nối Server:"));
        JTextField serverIPField = new JTextField(20);

        inputPanel.add(serverIPField);

        int option = JOptionPane.showConfirmDialog(null, inputPanel, "Enter Your Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String userName = nameField.getText().trim();
            String serverAddress = serverIPField.getText().trim();

            if (userName.isEmpty() || serverAddress.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập tên hiển thị và địa chỉ Server.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                ChatClient client = new ChatClient(serverAddress, userName);
                new Thread(client).start(); // Khởi chạy luồng nhận tin nhắn
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getLocalIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Unknown IP";
        }
    }

    @Override
    public void run() {
        // Nhận và xử lý tin nhắn từ server
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
