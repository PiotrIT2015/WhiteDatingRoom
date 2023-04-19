import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class Client implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    @Override
    public void run(){
        try{
            Socket client=new Socket("127.0.0.1", 9999);
            out=new PrintWriter(client.getOutputStream(),true);
            in=new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler=new InputHandler();
            Thread t=new Thread(inHandler);
            t.start();

            String inMessage;
            while((inMessage=in.readLine()) != null){
                System.out.println(inMessage);
            }
        }catch(IOException ex){
            shutdown();
        }
    }

    public void shutdown(){
        done=true;
        try{
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        }catch(IOException ex){

        }
    }

    class InputHandler implements Runnable{

        @Override
        public void run() {
            try{
                BufferedReader inReader=new BufferedReader(new InputStreamReader(System.in));
                while(!done){
                    String message=inReader.readLine();
                    if(message.equals("/quit")){
                        inReader.close();
                        shutdown();
                    }else{
                        out.println(message);
                    }
                }
            }catch(IOException ex){
                shutdown();
            }
        }
    }

    public static void main(String[] args) {

        //Client client=new Client();
        //client.run();                 //unblock these two lines to enter chat

        final File[] fileToSend=new File[1];

        JFrame jFrame=new JFrame("Relaxation tool");
        jFrame.setSize(450,450);
        jFrame.setLayout(new BoxLayout(jFrame.getContentPane(), BoxLayout.Y_AXIS));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel jlTitle = new JLabel("PP's file sender");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jlTitle.setBorder(new EmptyBorder(20,0,10,0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jlFileName=new JLabel("Chose a file to send.");
        jlFileName.setFont(new Font("Arial",Font.BOLD, 20));
        jlFileName.setBorder(new EmptyBorder(50,0,0,0));
        jlFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jpButton=new JPanel();
        jpButton.setBorder(new EmptyBorder(75,0,10,0));

        JButton jbSendFile=new JButton("Send file");
        jbSendFile.setPreferredSize(new Dimension(150,75));
        jbSendFile.setFont((new Font("Arial",Font.BOLD,20)));

        JButton jbChooseFile=new JButton("Choose file");
        jbChooseFile.setPreferredSize(new Dimension(150,75));
        jbChooseFile.setFont((new Font("Arial",Font.BOLD,20)));

        jpButton.add(jbSendFile);
        jpButton.add(jbChooseFile);

        jbChooseFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser=new JFileChooser();
                jFileChooser.setDialogTitle("Choose a file to send");

                if(jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                    fileToSend[0]=jFileChooser.getSelectedFile();
                    jlFileName.setText("The file you want to sent is:"+fileToSend[0].getName());
                }
            }
        });

        jbSendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(fileToSend[0]==null){
                    jlFileName.setText("Please choose a file first.");
                }else{
                    try {
                        FileInputStream fileInputStream = new FileInputStream(fileToSend[0].getAbsolutePath());
                        Socket socket = new Socket("localhost", 1234);
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        String fileName = fileToSend[0].getName();
                        byte[] fileNameBytes = fileName.getBytes();
                        byte[] fileContentBytes = new byte[(int) fileToSend[0].length()];
                        fileInputStream.read(fileContentBytes);

                        dataOutputStream.writeInt(fileNameBytes.length);
                        dataOutputStream.write(fileNameBytes);

                        dataOutputStream.writeInt(fileContentBytes.length);
                        dataOutputStream.write(fileContentBytes);
                    }catch(IOException error){
                        error.printStackTrace();
                    }
                }
            }
        });

        jFrame.add(jlTitle);
        jFrame.add(jlFileName);
        jFrame.add(jpButton);
        jFrame.setVisible(true);

    }
}