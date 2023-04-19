import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server(){
        connections=new ArrayList<>();
        done=false;
    }

    @Override
    public void run(){
        try {
            server=new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done){
                Socket client=server.accept();
                ConnectionHandler handler=new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcast(String message){
        for(ConnectionHandler ch: connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            done=true;
            if(!server.isClosed()){
                server.close();
            }
            for(ConnectionHandler ch: connections){
                ch.shutdown();
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }

    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client){
            this.client=client;
        }

        @Override
        public void run(){
            try{
                out=new PrintWriter(client.getOutputStream(),true);
                in=new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Wprowadź nick: ");
                nickname=in.readLine();
                System.out.println(nickname+"połączono");
                broadcast(nickname+" dołączył do chat");
                String message;
                while((message=in.readLine()) != null){
                    if(message.startsWith("/nick")){
                        String[] messageSplit = message.split(" ",2);
                        if(messageSplit.length==2){
                            broadcast(nickname+"zmienił nazwę na"+messageSplit[1]);
                            System.out.println(nickname+"zmienił nazwę na"+messageSplit[1]);
                            nickname=messageSplit[1];
                            out.println("Zmiana nick zakończona sukcesem. "+nickname);
                        }else{
                            out.println("Nie wprowadzono żadnego nick.");
                        }
                    }else if(message.startsWith("/quit")){
                        broadcast(nickname+" opuścił chat");
                        shutdown();
                    }else{
                        broadcast(nickname + ": " + message);
                    }
                }
            }catch(IOException ex){
                shutdown();
            }

        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try{
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }

        }

    }

    static ArrayList<MyFile> myFiles=new ArrayList<>();

    public static void main(String[] args) throws IOException {

        //Server server=new Server();
        //server.run();                 //unblock these two lines to enter chat

        int fileId=0;
        JFrame jFrame=new JFrame("PP's Server");
        jFrame.setSize(400,400);
        jFrame.setLayout(new BoxLayout(jFrame.getContentPane(), BoxLayout.Y_AXIS));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel jPanel=new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        JScrollPane jScrollPane=new JScrollPane(jPanel);
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JLabel jlTitle=new JLabel("PP's files receiver");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jlTitle.setBorder(new EmptyBorder(20,0,10,0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        jFrame.add(jlTitle);
        jFrame.add(jScrollPane);
        jFrame.setVisible(true);

        ServerSocket serverSocket=new ServerSocket(1234);

        while(true){

            try{
                Socket socket=serverSocket.accept();
                DataInputStream dataInputStream=new DataInputStream(socket.getInputStream());
                int fileNameLength=dataInputStream.readInt();

                if(fileNameLength > 0){
                    byte[] fileNameBytes=new byte[fileNameLength];
                    dataInputStream.readFully(fileNameBytes,0,fileNameBytes.length);
                    String fileName=new String(fileNameBytes);

                    int fileContentLength=dataInputStream.readInt();

                    if(fileContentLength>0){
                        byte[] fileContentBytes=new byte[fileContentLength];
                        dataInputStream.readFully(fileContentBytes,0,fileContentLength);

                        JPanel jpFileRow=new JPanel();
                        jpFileRow.setLayout(new BoxLayout(jpFileRow,BoxLayout.Y_AXIS));

                        JLabel jlFileName=new JLabel(fileName);
                        jlFileName.setFont(new Font("Arial",Font.BOLD,20));
                        jlFileName.setBorder(new EmptyBorder(10,0,10,0));

                        if(getFileExtension(fileName).equalsIgnoreCase("txt")){
                            jpFileRow.setName(String.valueOf(fileId));
                            jpFileRow.addMouseListener(getMyMouseListener());

                            jpFileRow.add(jlFileName);
                            jPanel.add(jpFileRow);
                            jFrame.validate();
                        }else{
                            jpFileRow.setName(String.valueOf(fileId));
                            jpFileRow.addMouseListener(getMyMouseListener());
                            jpFileRow.add(jlFileName);
                            jPanel.add(jpFileRow);

                            jFrame.validate();
                        }
                        myFiles.add(new MyFile(fileId, fileName, fileContentBytes, getFileExtension(fileName)));

                        fileId++;
                    }
                }

            }catch(IOException ex){
                ex.printStackTrace();
            }

        }

    }

    public static MouseListener getMyMouseListener(){
        return new MouseListener(){

            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel jpanel=(JPanel)e.getSource();
                int fileId=Integer.parseInt(jpanel.getName());

                for(MyFile myFile: myFiles){
                    if(myFile.getId()==fileId){
                        JFrame jfPreview=createFrame(myFile.getName(), myFile.getData(), myFile.getFileExtension());
                        jfPreview.setVisible(true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
    }

    public static JFrame createFrame(String fileName, byte[] fileData, String fileExtension){
        JFrame jframe=new JFrame("PP's files downloader.");
        jframe.setSize(400,400);

        JPanel jPanel=new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel,BoxLayout.Y_AXIS));

        JLabel jlTitle=new JLabel("PP's file downloader.");
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        jlTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jlTitle.setBorder(new EmptyBorder(20,0,10,0));

        JLabel jlPrompt=new JLabel("Are you sure you want to download?"+fileName);
        jlPrompt.setFont(new Font("Arial", Font.BOLD, 20));
        jlPrompt.setBorder(new EmptyBorder(20,0,10,0));
        jlPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbYes=new JButton("Yes");
        jbYes.setPreferredSize(new Dimension(150,75));
        jbYes.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jbNo=new JButton("No");
        jbNo.setPreferredSize(new Dimension(150,75));
        jbNo.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel jlFileContent=new JLabel();
        jlFileContent.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jpButtons=new JPanel();

        jpButtons.setLayout(new BoxLayout(jpButtons,BoxLayout.X_AXIS));

        jpButtons.setBorder(new EmptyBorder(20,0,10,0));
        jpButtons.add(jbYes);
        jpButtons.add(jbNo);

        if(fileExtension.equalsIgnoreCase("txt")){
            jlFileContent.setText("<html>"+new String(fileData)+"</html>");
        }else{
            jlFileContent.setIcon(new ImageIcon(fileData));
        }

        jbYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File fileToDownload=new File(fileName);

                try{
                    FileOutputStream fileOutputStream=new FileOutputStream(fileToDownload);

                    fileOutputStream.write(fileData);
                    fileOutputStream.close();
                    jframe.dispose();
                }catch(IOException ex){
                    ex.printStackTrace();
                }
            }
        });

        jbNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jframe.dispose();
            }
        });

        jPanel.add(jlTitle);
        jPanel.add(jlPrompt);
        jPanel.add(jlFileContent);
        jPanel.add(jpButtons);

        jframe.add(jPanel);

        return jframe;
    }

    public static String getFileExtension(String fileName){
        int i=fileName.lastIndexOf('.');
        if(i>0){
            return fileName.substring(i+1);
        }else{
            return "No extension found.";
        }
    }
}