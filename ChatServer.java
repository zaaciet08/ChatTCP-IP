package ChatTCP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class ChatServer extends JFrame implements Runnable {
    private static final int PORT = 12345;
    private Map<Socket, String> clientSockets = new ConcurrentHashMap<>(); // Lưu socket và username
    private JTextArea textArea;

    public ChatServer() {
        // Thiết lập giao diện Swing
        setTitle("Chat Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea));

        // Đặt cửa sổ ở giữa màn hình
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        new Thread(server).start();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logMessage("Server đã khởi động với port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start(); // Bắt đầu xử lý client mới
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                username = in.readLine(); // Nhận username từ client
                String newusername = username + ",";
                clientSockets.put(socket, newusername); // Lưu socket và username vào map

                logMessage(username + " đã đăng nhập vào Server Chat.");
                sendClientListToAll(); // Cập nhật danh sách client sau khi thêm người mới

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/w ")) { // Nhắn tin riêng
                        String[] messageParts = message.split(" ", 3);
                        if (messageParts.length == 3) {
                            String targetUser = messageParts[1];
                            String privateMessage = messageParts[2];
                            sendPrivateMessage(targetUser, username + privateMessage);
                        }
                    } else {
                        logMessage(username + ": " + message);
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientSockets.remove(socket); // Xóa client khỏi danh sách khi ngắt kết nối
                logMessage(username + " đã thoát khỏi Server.");
                sendClientListToAll(); // Cập nhật danh sách client sau khi một client rời đi
            }
        }

        // Nhắn tin riêng tới một người dùng cụ thể
        private void sendPrivateMessage(String targetUser, String message) {
            boolean userFound = false;
            for (Map.Entry<Socket, String> entry : clientSockets.entrySet()) {
                if (entry.getValue().trim().equals(targetUser + ",")) {
                    try {
                        PrintWriter writer = new PrintWriter(entry.getKey().getOutputStream(), true);
                        writer.println(message);
                        userFound = true;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!userFound) {
                // Có thể gửi thông báo lỗi cho người gửi nếu người nhận không tìm thấy
                sendPrivateMessage(username, "Error: User " + targetUser + " not found.");
            }
        }

        // Gửi tin nhắn đến tất cả các client
        private void broadcastMessage(String message) {
            for (Socket clientSocket : clientSockets.keySet()) {
                try {
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    writer.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    clientSockets.remove(clientSocket); // Xóa client bị lỗi
                }
            }
        }
    }

    // Gửi danh sách username cho tất cả client
    private void sendClientListToAll() {
        StringBuilder clientList = new StringBuilder("CLIENT_LIST:");
        for (String username : clientSockets.values()) {
            clientList.append(" ").append(username);
        }
        String clientListMessage = clientList.toString();

        // Gửi danh sách client cho tất cả các client
        for (Socket clientSocket : clientSockets.keySet()) {
            try {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println(clientListMessage);
            } catch (IOException e) {
                e.printStackTrace();
                clientSockets.remove(clientSocket); // Xóa client bị lỗi
            }
        }
    }

    // Phương thức hiển thị tin nhắn trên giao diện Swing
    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
        });
    }
}
